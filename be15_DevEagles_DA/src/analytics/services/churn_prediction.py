from __future__ import annotations

"""고객 이탈(Churn) 예측 서비스.

CRM DB에서 고객·예약·매출 데이터를 조회해 파생 피처를 생성하고
scikit-learn 모델로 이탈 확률을 예측한다.
"""

from datetime import datetime
from typing import Dict, List, Tuple

try:
    import matplotlib
    matplotlib.use("Agg")  # GUI 없는 환경에서도 이미지 저장 가능
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

from analytics.core.logging import get_logger

# --- 상수 정의 ---
_FEATURE_COLUMNS: List[str] = [
    "visit_count",
    "total_revenue",
    "days_since_last_visit",
    "customer_lifetime_days",
    "age",
    "noshow_count",
    "avg_days_between_visits",
    "visit_frequency",
    "avg_order_value",
    "total_sales_count",
    "noshow_rate",
    "cancellation_rate",
    "completion_rate",
    "marketing_consent",
    "gender",
    "channel_id",
    "customer_segment",
]


class ChurnPredictionService:  # pylint: disable=too-many-instance-attributes
    """고객 이탈 예측 파이프라인"""

    def __init__(self, crm_engine=None):
        self.logger = get_logger(__name__)
        if crm_engine is None:
            from analytics.core.database import get_crm_db
            self.crm_engine = get_crm_db()
        else:
            self.crm_engine = crm_engine

        self._scaler = StandardScaler()
        self._label_encoders: Dict[str, LabelEncoder] = {}
        self._model: RandomForestClassifier | LogisticRegression | None = None

    # ------------------------------------------------------------------
    # 데이터 로드
    # ------------------------------------------------------------------
    def _load_dataframe(self) -> pd.DataFrame:
        """CRM DB에서 필요한 컬럼을 읽어온다."""
        query = (
            """
            SELECT 
                c.customer_id,
                c.customer_name,
                c.phone_number,
                c.visit_count,
                c.total_revenue,
                c.recent_visit_date,
                c.birthdate,
                c.noshow_count,
                c.gender,
                c.marketing_consent,
                c.channel_id,
                c.created_at,
                s.shop_id,
                s.shop_name,
                COUNT(r.reservation_id)                                        AS total_reservations,
                COUNT(CASE WHEN r.reservation_status_name = 'PAID' THEN 1 END) AS paid_reservations,
                COUNT(CASE WHEN r.reservation_status_name = 'NO_SHOW' THEN 1 END) AS noshow_reservations,
                COUNT(CASE WHEN r.reservation_status_name IN ('CBC', 'CBS') THEN 1 END) AS cancelled_reservations,
                AVG(sl.total_amount)                                            AS avg_order_value,
                SUM(sl.total_amount)                                            AS total_sales_amount,
                COUNT(sl.sales_id)                                              AS total_sales_count
            FROM customer c
            LEFT JOIN shop s ON c.shop_id = s.shop_id
            LEFT JOIN reservation r ON c.customer_id = r.customer_id
            LEFT JOIN sales sl ON c.customer_id = sl.customer_id
            WHERE c.deleted_at IS NULL
            GROUP BY c.customer_id
            """
        )

        raw_conn = self.crm_engine.raw_connection()
        try:
            df = pd.read_sql_query(query, raw_conn)
        finally:
            raw_conn.close()

        self.logger.info("Loaded %s customers", f"{len(df):,}")
        return df

    # ------------------------------------------------------------------
    # Feature Engineering
    # ------------------------------------------------------------------
    def _create_features(self, df: pd.DataFrame) -> pd.DataFrame:
        current_date = datetime.now().date()

        # 날짜형 변환
        df["recent_visit_date"] = pd.to_datetime(df["recent_visit_date"], errors="coerce")
        df["created_at"] = pd.to_datetime(df["created_at"], errors="coerce")
        df["birthdate"] = pd.to_datetime(df["birthdate"], errors="coerce")

        df["days_since_last_visit"] = (
            pd.Timestamp(current_date) - df["recent_visit_date"]
        ).dt.days
        df["customer_lifetime_days"] = (
            pd.Timestamp(current_date) - df["created_at"]
        ).dt.days
        df["age"] = (pd.Timestamp(current_date) - df["birthdate"]).dt.days / 365.25

        df["avg_days_between_visits"] = df["customer_lifetime_days"] / (df["visit_count"] + 1)
        df["visit_frequency"] = df["visit_count"] / (
            (df["customer_lifetime_days"] / 365) + 0.1
        )

        # 결측값 및 이상치 보정
        for col in [
            "days_since_last_visit",
            "customer_lifetime_days",
            "avg_days_between_visits",
            "visit_frequency",
        ]:
            df[col] = df[col].replace([np.inf, -np.inf], np.nan)
        df.fillna({
            "days_since_last_visit": 9999,
            "customer_lifetime_days": 0,
            "avg_days_between_visits": 9999,
            "visit_frequency": 0,
            "avg_order_value": 0,
            "total_sales_amount": 0,
            "total_sales_count": 0,
            "total_reservations": 0,
            "paid_reservations": 0,
        }, inplace=True)

        # 예약 비율
        df["noshow_rate"] = df["noshow_reservations"] / (df["total_reservations"] + 1)
        df["cancellation_rate"] = df["cancelled_reservations"] / (
            df["total_reservations"] + 1
        )
        df["completion_rate"] = df["paid_reservations"] / (df["total_reservations"] + 1)

        # 고객 세그먼트
        df["customer_segment"] = "Regular"
        df.loc[df["visit_count"] >= 20, "customer_segment"] = "VIP"
        df.loc[df["visit_count"] <= 2, "customer_segment"] = "New"
        df.loc[(df["visit_count"] > 2) & (df["visit_count"] < 10), "customer_segment"] = "Growing"

        # 이탈 라벨 생성
        df["expected_visit_interval"] = df["avg_days_between_visits"].clip(upper=120)
        churn_threshold = (df["expected_visit_interval"] * 2.5).clip(lower=60, upper=180)
        churn_threshold.loc[df["customer_segment"] == "VIP"] *= 1.5
        churn_threshold.loc[df["customer_segment"] == "New"] *= 0.7

        df["is_churned"] = (
            (df["days_since_last_visit"] > churn_threshold)
            & (df["visit_count"] >= 2)
            & (df["customer_lifetime_days"] > 60)
        ).astype(int)

        self.logger.info(
            "Churn ratio: %.2f%%", df["is_churned"].mean() * 100
        )
        return df

    # ------------------------------------------------------------------
    # 모델 입력 준비
    # ------------------------------------------------------------------
    def _prepare_Xy(self, df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
        data = df[_FEATURE_COLUMNS + ["is_churned"]].copy()

        for col in ["gender", "customer_segment"]:
            le = LabelEncoder()
            data[col] = le.fit_transform(data[col].astype(str))
            self._label_encoders[col] = le

        # 결측값 보정
        for col in data.columns:
            if data[col].dtype.kind in {"f", "i"}:
                data[col] = data[col].fillna(data[col].median())
            else:
                mode = data[col].mode()
                fill_value = mode[0] if len(mode) else 0
                data[col] = data[col].fillna(fill_value)

        X = data[_FEATURE_COLUMNS]
        y = data["is_churned"]
        return X, y

    # ------------------------------------------------------------------
    # 모델 학습
    # ------------------------------------------------------------------
    def _train(self, X: pd.DataFrame, y: pd.Series) -> Tuple[Dict, str]:
        X_tr, X_te, y_tr, y_te = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )
        X_tr_scaled = self._scaler.fit_transform(X_tr)
        X_te_scaled = self._scaler.transform(X_te)

        models = {
            "RandomForest": RandomForestClassifier(
                n_estimators=50,
                max_depth=8,
                min_samples_split=20,
                min_samples_leaf=10,
                class_weight="balanced",
                random_state=42,
            ),
            "LogReg": LogisticRegression(
                max_iter=1000,
                C=0.1,
                class_weight="balanced",
                random_state=42,
            ),
        }
        results: Dict[str, Dict] = {}
        for name, mdl in models.items():
            if name == "LogReg":
                mdl.fit(X_tr_scaled, y_tr)
                preds = mdl.predict_proba(X_te_scaled)[:, 1]
                cv = cross_val_score(mdl, X_tr_scaled, y_tr, cv=5, scoring="roc_auc")
            else:
                mdl.fit(X_tr, y_tr)
                preds = mdl.predict_proba(X_te)[:, 1]
                cv = cross_val_score(mdl, X_tr, y_tr, cv=5, scoring="roc_auc")
            auc = roc_auc_score(y_te, preds)
            results[name] = {"model": mdl, "auc": auc, "cv": cv.mean()}
            self.logger.info("%s AUC=%.3f CV=%.3f", name, auc, cv.mean())

        best = max(results, key=lambda n: results[n]["auc"])
        self._model = results[best]["model"]
        return results, best

    # ------------------------------------------------------------------
    # 이탈 위험 태그 부여
    # ------------------------------------------------------------------
    def _assign_churn_risk_tags(self, df: pd.DataFrame) -> pd.DataFrame:
        """고객별 이탈 위험 태그 및 위험 수준 부여"""
        df = df.copy()
        
        # 기본값 설정
        df['churn_risk_tag'] = 'NORMAL'
        df['risk_level'] = 'low'
        
        # 1. 신규 고객 관련 태그
        new_customers = df['customer_segment'] == 'New'
        
        # NEW_FOLLOWUP: 신규 고객 팔로업 필요 (가입 후 7-20일)
        new_followup_condition = (
            new_customers & 
            (df['customer_lifetime_days'] >= 7) & 
            (df['customer_lifetime_days'] <= 20) &
            (df['visit_count'] <= 2)
        )
        df.loc[new_followup_condition, 'churn_risk_tag'] = 'NEW_FOLLOWUP'
        df.loc[new_followup_condition, 'risk_level'] = 'medium'
        
        # NEW_AT_RISK: 신규 고객 이탈 위험 (30일 이상 미방문)
        new_at_risk_condition = (
            new_customers &
            (df['days_since_last_visit'] >= 30) &
            (df['customer_lifetime_days'] >= 30)
        )
        df.loc[new_at_risk_condition, 'churn_risk_tag'] = 'NEW_AT_RISK'
        df.loc[new_at_risk_condition, 'risk_level'] = 'high'
        
        # 2. 기존 고객 관련 태그
        # REACTIVATION_NEEDED: 재활성화 필요 (60일 이상 미방문)
        reactivation_condition = (
            (df['customer_segment'].isin(['Growing', 'Regular', 'VIP'])) &
            (df['days_since_last_visit'] >= 60) &
            (df['visit_count'] >= 3)
        )
        df.loc[reactivation_condition, 'churn_risk_tag'] = 'REACTIVATION_NEEDED'
        df.loc[reactivation_condition, 'risk_level'] = 'high'
        
        # GROWING_DELAYED: 성장 고객 방문 지연
        growing_delayed_condition = (
            (df['customer_segment'] == 'Growing') &
            (df['days_since_last_visit'] >= 45) &
            (df['days_since_last_visit'] < 60)
        )
        df.loc[growing_delayed_condition, 'churn_risk_tag'] = 'GROWING_DELAYED'
        df.loc[growing_delayed_condition, 'risk_level'] = 'medium'
        
        # VIP_ATTENTION: VIP 고객 패턴 변화/이상
        vip_attention_condition = (
            (df['customer_segment'] == 'VIP') &
            (
                (df['days_since_last_visit'] >= 30) |  # VIP가 30일 이상 미방문
                (df['churn_probability'] >= 0.3)  # VIP인데 이탈 확률이 높음
            )
        )
        df.loc[vip_attention_condition, 'churn_risk_tag'] = 'VIP_ATTENTION'
        df.loc[vip_attention_condition, 'risk_level'] = 'high'
        
        # 3. 모델 기반 이탈 위험 태그
        # CHURN_RISK_HIGH: 모델이 예측한 이탈 위험 고객
        high_churn_prob_condition = (
            (df['churn_probability'] >= 0.7) &
            (df['churn_risk_tag'] == 'NORMAL')  # 다른 태그가 없는 경우만
        )
        df.loc[high_churn_prob_condition, 'churn_risk_tag'] = 'CHURN_RISK_HIGH'
        df.loc[high_churn_prob_condition, 'risk_level'] = 'high'
        
        # 4. 위험 수준 재조정 (이탈 확률 기반)
        # 높은 이탈 확률을 가진 고객들의 위험 수준 상향 조정
        high_prob_mask = df['churn_probability'] >= 0.6
        df.loc[high_prob_mask & (df['risk_level'] == 'low'), 'risk_level'] = 'medium'
        df.loc[high_prob_mask & (df['risk_level'] == 'medium'), 'risk_level'] = 'high'
        
        # 매우 낮은 이탈 확률의 경우 위험 수준 하향 조정
        very_low_prob_mask = df['churn_probability'] <= 0.1
        df.loc[very_low_prob_mask & (df['risk_level'] == 'high'), 'risk_level'] = 'medium'
        df.loc[very_low_prob_mask & (df['risk_level'] == 'medium'), 'risk_level'] = 'low'
        
        self.logger.info("Churn risk tags assigned:")
        tag_counts = df['churn_risk_tag'].value_counts()
        for tag, count in tag_counts.items():
            self.logger.info(f"  {tag}: {count} customers")
        
        risk_counts = df['risk_level'].value_counts()
        for level, count in risk_counts.items():
            self.logger.info(f"Risk level {level}: {count} customers")
        
        return df

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def run_full_analysis(self) -> Dict:
        df = self._load_dataframe()
        df = self._create_features(df)
        X, y = self._prepare_Xy(df)
        results, best = self._train(X, y)

        # 전체 데이터에 대해 이탈 확률 계산
        if isinstance(self._model, LogisticRegression):
            probs = self._model.predict_proba(self._scaler.transform(X))[:, 1]
        else:
            probs = self._model.predict_proba(X)[:, 1]

        df["churn_probability"] = probs

        # 세그먼트별 이탈률 집계
        segment_stats = (
            df.groupby("customer_segment")[["is_churned", "churn_probability"]]
            .agg(total_customers=("is_churned", "size"),
                 churned=("is_churned", "sum"),
                 avg_prob=("churn_probability", "mean"))
            .reset_index()
        )

        # 고위험 고객 상위 30명 추출
        high_risk_df = df.nlargest(30, "churn_probability")[
            [
                "customer_id",
                "customer_name",
                "phone_number",
                "shop_name",
                "visit_count",
                "days_since_last_visit",
                "total_revenue",
                "churn_probability",
            ]
        ].copy()

        # 이탈 위험 태그 부여
        df = self._assign_churn_risk_tags(df)
        
        # 위험 태그별 통계
        risk_tag_stats = df['churn_risk_tag'].value_counts().to_dict()
        
        return {
            "customers": len(df),
            "churn_rate": float(df["is_churned"].mean()),
            "best_model": best,
            "results": results,
            "segment_stats": segment_stats.to_dict(orient="records"),
            "high_risk_customers": high_risk_df.to_dict(orient="records"),
            "risk_tag_stats": risk_tag_stats,
            "predictions": df[['customer_id', 'customer_name', 'churn_probability', 'churn_risk_tag', 'risk_level']].to_dict(orient="records")
        }

    def predict(self, customers: pd.DataFrame) -> pd.Series:
        if self._model is None:
            raise RuntimeError("Model not trained yet")
        df = customers.copy()
        for col, enc in self._label_encoders.items():
            if col in df.columns:
                df[col] = enc.transform(df[col].astype(str))
        if isinstance(self._model, LogisticRegression):
            X_scaled = self._scaler.transform(df[_FEATURE_COLUMNS])
            return pd.Series(self._model.predict_proba(X_scaled)[:, 1], index=df.index)
        return pd.Series(self._model.predict_proba(df[_FEATURE_COLUMNS])[:, 1], index=df.index)

    def predict_customer_churn(self) -> Dict:
        """고객 이탈 예측 실행 (기존 인터페이스 호환용)"""
        return self.run_full_analysis() 