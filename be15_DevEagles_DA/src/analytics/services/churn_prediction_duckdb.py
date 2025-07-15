"""
고객 이탈(Churn) 예측 서비스 - DuckDB 버전

DuckDB에서 데이터를 조회해 파생 피처를 생성하고
scikit-learn 모델로 이탈 확률을 예측합니다.
"""

from datetime import datetime
from typing import Dict, List, Tuple

import matplotlib
matplotlib.use("Agg")

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

from analytics.core.logging import get_logger
from analytics.core.database import get_analytics_db, get_crm_db

# 상수 정의
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


class ChurnPredictionServiceDuckDB:
    """고객 이탈 예측 파이프라인 - DuckDB 버전"""

    def __init__(self):
        self.logger = get_logger(__name__)
        self.analytics_db = get_analytics_db()
        self.crm_db = get_crm_db()

        self._scaler = StandardScaler()
        self._label_encoders: Dict[str, LabelEncoder] = {}
        self._model: RandomForestClassifier | LogisticRegression | None = None

    def _load_dataframe(self) -> pd.DataFrame:
        """DuckDB에서 필요한 데이터를 조회합니다."""
        try:
            self.logger.info("DuckDB에서 이탈 분석 데이터 로딩 시작")
            
            # DuckDB에서 직접 이탈 분석용 데이터 조회
            query = """
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
                c.shop_id,
                c.shop_name,
                COUNT(r.reservation_id) as total_reservations,
                COUNT(CASE WHEN r.reservation_status_name = 'PAID' THEN 1 END) as paid_reservations,
                COUNT(CASE WHEN r.reservation_status_name = 'NO_SHOW' THEN 1 END) as noshow_reservations,
                COUNT(CASE WHEN r.reservation_status_name IN ('CBC', 'CBS') THEN 1 END) as cancelled_reservations,
                AVG(s.total_amount) as avg_order_value,
                SUM(s.total_amount) as total_sales_amount,
                COUNT(s.sales_id) as total_sales_count
            FROM customer c
            LEFT JOIN reservation r ON c.customer_id = r.customer_id
            LEFT JOIN sales s ON c.customer_id = s.customer_id
            GROUP BY c.customer_id, c.customer_name, c.phone_number, c.visit_count, c.total_revenue, c.recent_visit_date, c.birthdate, c.noshow_count, c.gender, c.marketing_consent, c.channel_id, c.created_at, c.shop_id, c.shop_name
            """
            
            df = self.analytics_db.execute(query).fetchdf()
            
            self.logger.info("Loaded %s customers", f"{len(df):,}")
            return df
            
        except Exception as e:
            self.logger.error(f"DuckDB 데이터 로딩 실패: {e}")
            raise

    def _create_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """피처 엔지니어링"""
        current_date = datetime.now().date()

        # 날짜형 변환
        df["recent_visit_date"] = pd.to_datetime(df["recent_visit_date"], errors="coerce")
        df["created_at"] = pd.to_datetime(df["created_at"], errors="coerce")
        df["birthdate"] = pd.to_datetime(df["birthdate"], errors="coerce")

        # 파생 피처 생성
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

        # 예약 비율 계산
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

    def _prepare_Xy(self, df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
        """모델 입력 데이터 준비"""
        data = df[_FEATURE_COLUMNS + ["is_churned"]].copy()

        # 범주형 변수 인코딩
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

    def _train(self, X: pd.DataFrame, y: pd.Series) -> Tuple[Dict, str]:
        """모델 학습"""
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

    def run_full_analysis(self) -> Dict:
        """전체 이탈 분석 실행"""
        try:
            self.logger.info("DuckDB 기반 이탈 분석 시작")
            
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

            self.logger.info("DuckDB 기반 이탈 분석 완료")
            
            return {
                "customers": len(df),
                "churn_rate": float(df["is_churned"].mean()),
                "best_model": best,
                "results": results,
                "segment_stats": segment_stats.to_dict(orient="records"),
                "high_risk_customers": high_risk_df.to_dict(orient="records"),
                "data_source": "DuckDB"
            }
            
        except Exception as e:
            self.logger.error(f"DuckDB 기반 이탈 분석 실패: {e}")
            raise

    def predict(self, customers: pd.DataFrame) -> pd.Series:
        """개별 고객 이탈 확률 예측"""
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