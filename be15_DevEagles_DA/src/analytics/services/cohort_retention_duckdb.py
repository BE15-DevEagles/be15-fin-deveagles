"""
코호트 리텐션 분석 서비스 - DuckDB 버전

DuckDB에서 데이터를 조회하여 코호트 리텐션 분석을 수행합니다.
"""

import pandas as pd
import numpy as np

try:
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    import seaborn as sns
    import matplotlib.font_manager as fm
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

from datetime import datetime, timedelta
import warnings
import os

from analytics.core.logging import get_logger
from analytics.core.database import get_analytics_db, get_crm_db

# 한글 폰트 설정
font_candidates = [
    (r"C:/Windows/Fonts/malgun.ttf", "Malgun Gothic"),
    ("/usr/share/fonts/truetype/nanum/NanumGothic.ttf", "NanumGothic")
]

for fp, family in font_candidates:
    if os.path.exists(fp):
        fm.fontManager.addfont(fp)
        plt.rc("font", family=family)
        break
else:
    plt.rcParams['font.family'] = 'DejaVu Sans'

plt.rcParams['axes.unicode_minus'] = False


class CohortRetentionAnalyzerDuckDB:
    """코호트 리텐션 분석기 - DuckDB 버전"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.analytics_db = get_analytics_db()
        self.crm_db = get_crm_db()
        self.customer_data = None
        self.reservation_data = None
        self.cohort_data = None
        self.sales_data = None
        
    def load_data(self):
        """DuckDB에서 데이터 로딩"""
        self.logger.info("📊 DuckDB에서 코호트 분석 데이터 로딩 중...")
        
        try:
            # 고객 데이터 로드 (단수형 테이블명)
            self.logger.info("고객 데이터 로딩...")
            customer_query = "SELECT * FROM customer"
            self.customer_data = self.analytics_db.execute(customer_query).fetchdf()
            
            # 예약 데이터 로드 (최근 1년, 단수형 테이블명)
            self.logger.info("예약 데이터 로딩...")
            reservation_query = """
            SELECT * FROM reservation 
            WHERE reservation_start_at >= NOW() - INTERVAL 365 DAY
            """
            self.reservation_data = self.analytics_db.execute(reservation_query).fetchdf()
            
            # 매출 데이터 로드 (최근 1년)
            self.logger.info("매출 데이터 로딩...")
            sales_query = """
            SELECT * FROM sales 
            WHERE sales_date >= NOW() - INTERVAL 365 DAY
            """
            self.sales_data = self.analytics_db.execute(sales_query).fetchdf()
            
            self.logger.info(f"✅ 로드 완료 - 고객: {len(self.customer_data):,}명, "
                           f"예약: {len(self.reservation_data):,}건, "
                           f"매출: {len(self.sales_data):,}건")
            
            # 날짜 컬럼 변환
            if 'recent_visit_date' in self.customer_data.columns:
                self.customer_data['recent_visit_date'] = pd.to_datetime(self.customer_data['recent_visit_date'])
                
        except Exception as e:
            self.logger.error(f"DuckDB 데이터 로딩 실패: {e}")
            raise
        
    def prepare_cohort_data(self):
        """코호트 분석을 위한 데이터 전처리"""
        self.logger.info("🔄 코호트 데이터 준비 중...")
        
        # 날짜 컬럼 변환
        self.customer_data['created_at'] = pd.to_datetime(self.customer_data['created_at'])
        self.customer_data['birthdate'] = pd.to_datetime(self.customer_data['birthdate'])
        self.reservation_data['reservation_start_at'] = pd.to_datetime(self.reservation_data['reservation_start_at'])
        
        if self.sales_data is not None and len(self.sales_data) > 0:
            self.sales_data['sales_date'] = pd.to_datetime(self.sales_data['sales_date'])
            self.sales_data['birthdate'] = pd.to_datetime(self.sales_data['birthdate'])
        
        # 고객별 첫 예약월 계산 (코호트 기준)
        customer_first_reservations = self.reservation_data.groupby('customer_id')['reservation_start_at'].min().reset_index()
        customer_first_reservations.columns = ['customer_id', 'first_reservation_date']
        customer_first_reservations['cohort_month'] = customer_first_reservations['first_reservation_date'].dt.to_period('M')
        
        # 고객 데이터와 첫 예약 정보 병합
        self.customer_data = self.customer_data.merge(
            customer_first_reservations[['customer_id', 'cohort_month', 'first_reservation_date']], 
            on='customer_id', 
            how='inner'
        )
        
        # 예약 데이터에 고객 정보 조인
        merged_data = self.reservation_data.merge(
            self.customer_data[['customer_id', 'cohort_month', 'first_reservation_date', 'gender', 'birthdate']], 
            on='customer_id'
        )
        
        # 예약월 계산
        merged_data['reservation_month'] = merged_data['reservation_start_at'].dt.to_period('M')
        
        # 첫 예약 후 경과 개월 수 계산
        merged_data['months_since_first_reservation'] = (
            merged_data['reservation_month'] - merged_data['cohort_month']
        ).apply(lambda x: x.n)
        
        # 음수 값 제거
        merged_data = merged_data[merged_data['months_since_first_reservation'] >= 0]
        
        # 연령대 계산
        current_date = pd.Timestamp.now()
        merged_data['age'] = (current_date - merged_data['birthdate']).dt.days / 365.25
        merged_data['age_group'] = pd.cut(
            merged_data['age'], 
            bins=[0, 30, 40, 50, 60, 100], 
            labels=['20-29', '30-39', '40-49', '50-59', '60+'], 
            right=False
        )
        
        self.cohort_data = merged_data
        self.logger.info(f"✅ 코호트 데이터 준비 완료: {len(self.cohort_data):,}건")
        
    def create_cohort_table(self, data=None):
        """코호트 테이블 생성"""
        if data is None:
            data = self.cohort_data
            
        # 현재 날짜에서 불완전한 코호트 제외 (최근 2개월)
        current_date = datetime.now()
        cutoff_date = current_date - timedelta(days=60)
        cutoff_cohort = pd.Period(cutoff_date, freq='M')
        
        # 불완전한 코호트 제외
        complete_data = data[data['cohort_month'] <= cutoff_cohort]
        
        if len(complete_data) == 0:
            self.logger.warning("⚠️  2개월 기준으로는 완전한 코호트가 없어 전체 데이터를 사용합니다.")
            complete_data = data.copy()
        
        # 코호트별 고객 수 계산
        cohort_sizes = complete_data.groupby('cohort_month')['customer_id'].nunique().reset_index()
        cohort_sizes.columns = ['cohort_month', 'total_customers']
        
        # 각 코호트의 월별 활성 고객 수 계산
        cohort_table = complete_data.groupby(['cohort_month', 'months_since_first_reservation'])['customer_id'].nunique().reset_index()
        cohort_table.columns = ['cohort_month', 'period_number', 'customers']
        
        # 피벗 테이블 생성
        cohort_table = cohort_table.pivot(index='cohort_month', 
                                         columns='period_number', 
                                         values='customers')
        
        # 코호트 크기와 병합
        cohort_sizes.set_index('cohort_month', inplace=True)
        
        # 리텐션율 계산
        cohort_table = cohort_table.divide(cohort_sizes['total_customers'], axis=0)
        
        return cohort_table, cohort_sizes
    
    def analyze_shop_cohorts(self):
        """매장별 코호트 분석"""
        self.logger.info("\n🏪 매장별 코호트 분석 시작...")
        
        shop_cohort_summary = []
        
        # 매장 목록 가져오기
        shop_list = self.customer_data[['shop_id', 'shop_name']].drop_duplicates()
        
        for _, shop_info in shop_list.iterrows():
            shop_id = shop_info['shop_id']
            shop_name = shop_info['shop_name']
            
            # 매장별 데이터 필터링
            shop_cohort_data = self.cohort_data[self.cohort_data['shop_id'] == shop_id]
            
            if len(shop_cohort_data) == 0:
                continue
            
            # 매장별 코호트 테이블 생성
            shop_cohort_table, shop_cohort_sizes = self.create_cohort_table(shop_cohort_data)
            
            if shop_cohort_table is None or shop_cohort_table.empty:
                continue
            
            # 매장별 리텐션 지표 계산
            total_customers = shop_cohort_sizes['total_customers'].sum()
            month1_retention = shop_cohort_table[1].mean() if 1 in shop_cohort_table.columns else 0
            month3_retention = shop_cohort_table[3].mean() if 3 in shop_cohort_table.columns else 0
            month6_retention = shop_cohort_table[6].mean() if 6 in shop_cohort_table.columns else 0
            
            # 매장별 평균 객단가 계산
            avg_price = 0
            if self.sales_data is not None and len(self.sales_data) > 0:
                shop_sales = self.sales_data[self.sales_data['shop_id'] == shop_id]
                if len(shop_sales) > 0:
                    avg_price = shop_sales['total_amount'].mean()
            
            shop_cohort_summary.append({
                'shop_id': shop_id,
                'shop_name': shop_name,
                'total_customers': total_customers,
                'total_reservations': len(shop_cohort_data),
                'month1_retention': month1_retention,
                'month3_retention': month3_retention,
                'month6_retention': month6_retention,
                'avg_price': avg_price,
                'cohort_table': shop_cohort_table,
                'cohort_sizes': shop_cohort_sizes
            })
        
        # 결과 출력
        shop_df = pd.DataFrame(shop_cohort_summary)
        if not shop_df.empty:
            shop_df = shop_df.sort_values('month1_retention', ascending=False)
            
            self.logger.info(f"\n📊 매장별 리텐션 분석 결과:")
            self.logger.info("=" * 100)
            self.logger.info(f"{'순위':<4} {'매장명':<20} {'고객수':<8} {'1개월':<8} {'3개월':<8} {'6개월':<8} {'평균금액':<12}")
            self.logger.info("-" * 100)
            
            for i, row in enumerate(shop_df.iterrows(), 1):
                data = row[1]
                self.logger.info(f"{i:<4} {data['shop_name']:<20} {data['total_customers']:<8,.0f} "
                              f"{data['month1_retention']:<8.1%} {data['month3_retention']:<8.1%} "
                              f"{data['month6_retention']:<8.1%} {data['avg_price']:<12,.0f}")
        
        return shop_df
    
    def create_overall_cohort_heatmap(self):
        """전체 코호트 히트맵 생성"""
        self.logger.info("\n🎨 전체 코호트 히트맵 생성 중...")
        
        cohort_table, cohort_sizes = self.create_cohort_table()
        
        if cohort_table is None:
            self.logger.warning("⚠️  코호트 테이블 생성 실패")
            return
        
        plt.figure(figsize=(16, 10))
        
        # 통계 계산
        total_customers = cohort_sizes['total_customers'].sum()
        total_reservations = len(self.cohort_data)
        cohort_count = len(cohort_table)
        analysis_period = f"{cohort_table.index.min()} ~ {cohort_table.index.max()}"
        
        # 평균 리텐션율 계산
        month1_retention = cohort_table[1].mean() if 1 in cohort_table.columns else 0
        month3_retention = cohort_table[3].mean() if 3 in cohort_table.columns else 0
        month6_retention = cohort_table[6].mean() if 6 in cohort_table.columns else 0
        
        # 히트맵 생성
        sns.heatmap(cohort_table, 
                   annot=True, 
                   fmt='.1%',
                   cmap='YlOrRd',
                   linewidths=0.5,
                   cbar_kws={'label': 'Retention Rate'})
        
        # 제목 설정
        plt.title('Overall Cohort Retention Analysis (DuckDB)', fontsize=18, pad=30, fontweight='bold')
        
        # 통계 정보 텍스트
        stats_text = f"""Period: {analysis_period}  |  Total Customers: {total_customers:,}  |  Total Reservations: {total_reservations:,}  |  Cohorts: {cohort_count}
Average Retention - 1M: {month1_retention:.1%}  |  3M: {month3_retention:.1%}  |  6M: {month6_retention:.1%}"""
        
        plt.figtext(0.5, 0.95, stats_text, ha='center', va='top', fontsize=11, 
                   bbox=dict(boxstyle="round,pad=0.5", facecolor="lightgray", alpha=0.8))
        
        plt.xlabel('Months Since First Visit', fontsize=12)
        plt.ylabel('Cohort Month', fontsize=12)
        plt.xticks(rotation=0)
        plt.yticks(rotation=0)
        
        plt.tight_layout()
        plt.subplots_adjust(top=0.85)
        
        save_path = 'overall_cohort_heatmap_duckdb.png'
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        self.logger.info(f"✅ 전체 코호트 히트맵 저장: {save_path}")
        
        plt.close()
    
    def run_full_analysis(self):
        """전체 코호트 분석 실행"""
        try:
            self.logger.info("🚀 DevEagles 코호트 리텐션 분석 시작 (DuckDB)")
            self.logger.info("="*60)
            
            # 1. 데이터 로드 및 전처리
            self.load_data()
            self.prepare_cohort_data()
            
            # 2. 전체 코호트 분석
            self.create_overall_cohort_heatmap()
            
            # 3. 매장별 분석
            shop_df = self.analyze_shop_cohorts()
            
            # 4. 종합 결과 반환
            cohort_table, cohort_sizes = self.create_cohort_table()
            
            # 전체 리텐션 지표
            overall_1m = cohort_table[1].mean() if 1 in cohort_table.columns else 0
            overall_3m = cohort_table[3].mean() if 3 in cohort_table.columns else 0
            overall_6m = cohort_table[6].mean() if 6 in cohort_table.columns else 0
            
            # 평균 객단가 계산
            aov_results = None
            gender_aov_df = None
            age_aov_df = None
            
            if self.sales_data is not None and len(self.sales_data) > 0:
                total_sales = self.sales_data['total_amount'].sum()
                total_orders = len(self.sales_data)
                avg_order_value = total_sales / total_orders if total_orders else 0
                
                aov_results = {
                    'total_sales': total_sales,
                    'total_orders': total_orders,
                    'avg_order_value': avg_order_value
                }
                
                # 성별 AOV
                gender_aov_df = self.sales_data.groupby('gender')['total_amount'].mean().reset_index()
                
                # 연령대별 AOV
                current_date = pd.Timestamp.now()
                tmp = self.sales_data.copy()
                tmp['age'] = (current_date - pd.to_datetime(tmp['birthdate'])).dt.days / 365.25
                tmp['age_group'] = pd.cut(tmp['age'], bins=[0,30,40,50,60,100], labels=['20대','30대','40대','50대','60대+'], right=False)
                age_aov_df = tmp.groupby('age_group')['total_amount'].mean().reset_index()
            
            self.logger.info(f"\n✅ DuckDB 기반 코호트 리텐션 분석 완료!")
            
            return {
                'shop_analysis': shop_df,
                'overall_cohort_table': cohort_table,
                'total_customers': len(self.customer_data),
                'total_reservations': len(self.cohort_data),
                'overall_retention': {
                    '1_month': overall_1m,
                    '3_month': overall_3m,
                    '6_month': overall_6m
                },
                'aov_results': aov_results,
                'gender_aov': gender_aov_df,
                'age_aov': age_aov_df,
                'data_source': 'DuckDB'
            }
            
        except Exception as e:
            self.logger.error(f"❌ DuckDB 기반 분석 중 오류 발생: {e}")
            import traceback
            traceback.print_exc()
            return None