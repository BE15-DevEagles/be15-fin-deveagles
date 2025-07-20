"""
코호트 리텐션 분석 서비스

매장별 코호트 리텐션 분석과 대시보드 기능을 제공합니다.
"""

import pandas as pd
import numpy as np

try:
    import matplotlib
    matplotlib.use('Agg')  # GUI 없이 이미지만 생성
    import matplotlib.pyplot as plt
    import seaborn as sns
    import matplotlib.font_manager as fm
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
from datetime import datetime, timedelta
import warnings
import os

# --- 한글 폰트 설정 (Windows / Linux 모두 대응) ---
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

# 마이너스 기호 깨짐 방지
plt.rcParams['axes.unicode_minus'] = False

class CohortRetentionAnalyzer:
    """코호트 리텐션 분석기"""
    
    def __init__(self, crm_engine, analytics_engine):
        """
        코호트 리텐션 분석기 초기화
        
        Args:
            crm_engine: CRM 데이터베이스 연결 객체
            analytics_engine: Analytics 데이터베이스 연결 객체
        """
        self.crm_engine = crm_engine
        self.analytics_engine = analytics_engine
        self.customer_data = None
        self.reservation_data = None
        self.cohort_data = None
        self.shop_data = None
        self.sales_data = None  # 매출 데이터 (객단가 분석용)
        
    def load_data(self):
        """데이터베이스에서 고객 및 예약 데이터 로드"""
        print("📊 코호트 분석 데이터 로딩 중...")
        
        # 고객 데이터 로드 (plain string)
        customer_query = """
        SELECT 
            c.customer_id,
            c.customer_name,
            c.created_at as customer_created_at,
            c.shop_id,
            s.shop_name,
            c.deleted_at,
            c.gender,
            c.birthdate,
            c.total_revenue,
            c.visit_count,
            c.recent_visit_date
        FROM customer c
        JOIN shop s ON c.shop_id = s.shop_id
        WHERE c.deleted_at IS NULL
        ORDER BY c.created_at
        """
        
        # 예약 데이터 로드 (plain string)
        reservation_query = """
        SELECT 
            r.reservation_id,
            r.customer_id,
            r.reservation_start_at,
            r.reservation_status_name,
            r.shop_id,
            s.shop_name,
            r.deleted_at,
            r.staff_id
        FROM reservation r
        JOIN shop s ON r.shop_id = s.shop_id
        WHERE r.deleted_at IS NULL 
        AND r.customer_id IS NOT NULL
        AND r.reservation_status_name IN ('CONFIRMED', 'PAID')
        ORDER BY r.reservation_start_at
        """
        
        # 매장 데이터 로드 (plain string)
        shop_query = """
        SELECT 
            s.shop_id,
            s.shop_name,
            s.industry_id,
            s.address,
            s.created_at
        FROM shop s
        ORDER BY s.shop_name
        """
        
        # 매출 데이터 로드 (plain string)
        sales_query = """
        SELECT 
            sl.customer_id,
            sl.shop_id,
            sl.total_amount,
            sl.sales_date,
            c.gender,
            c.birthdate
        FROM sales sl
        JOIN customer c ON sl.customer_id = c.customer_id
        WHERE sl.deleted_at IS NULL
        AND c.deleted_at IS NULL
        ORDER BY sl.sales_date
        """
        
        print("고객 데이터 로딩...")
        raw_conn = self.crm_engine.raw_connection()
        try:
            self.customer_data = pd.read_sql_query(customer_query, raw_conn)
        finally:
            raw_conn.close()

        print("예약 데이터 로딩...")
        raw_conn = self.crm_engine.raw_connection()
        try:
            self.reservation_data = pd.read_sql_query(reservation_query, raw_conn)
        finally:
            raw_conn.close()

        print("매장 데이터 로딩...")
        raw_conn = self.crm_engine.raw_connection()
        try:
            self.shop_data = pd.read_sql_query(shop_query, raw_conn)
        finally:
            raw_conn.close()
        
        # 매출 데이터 로드
        print("매출 데이터 로딩...")
        raw_conn = self.crm_engine.raw_connection()
        try:
            self.sales_data = pd.read_sql_query(sales_query, raw_conn)
        finally:
            raw_conn.close()
        
        print(f"✅ 로드 완료 - 고객: {len(self.customer_data):,}명, 예약: {len(self.reservation_data):,}건, 매장: {len(self.shop_data):,}개")
        
        if 'recent_visit_date' in self.customer_data.columns:
            self.customer_data['recent_visit_date'] = pd.to_datetime(self.customer_data['recent_visit_date'])
        
    def prepare_cohort_data(self):
        """코호트 분석을 위한 데이터 전처리"""
        print("🔄 코호트 데이터 준비 중...")
        
        # 날짜 컬럼 변환
        self.customer_data['customer_created_at'] = pd.to_datetime(self.customer_data['customer_created_at'])
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
            how='inner'  # 예약이 있는 고객만 포함
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
        print(f"✅ 코호트 데이터 준비 완료: {len(self.cohort_data):,}건")
        
    def create_cohort_table(self, data=None):
        """코호트 테이블 생성"""
        if data is None:
            data = self.cohort_data
            
        # 현재 날짜에서 불완전한 코호트 제외 (최근 2개월)
        current_date = datetime.now()
        cutoff_date = current_date - timedelta(days=60)  # 2개월 전
        cutoff_cohort = pd.Period(cutoff_date, freq='M')
        
        # 불완전한 코호트 제외
        complete_data = data[data['cohort_month'] <= cutoff_cohort]
        
        if len(complete_data) == 0:
            print("⚠️  2개월 기준으로는 완전한 코호트가 없어 전체 데이터를 사용합니다.")
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
        print("\n🏪 매장별 코호트 분석 시작...")
        
        shop_cohort_summary = []
        
        for shop_id in self.shop_data['shop_id'].unique():
            shop_info = self.shop_data[self.shop_data['shop_id'] == shop_id].iloc[0]
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
            
            # 매장별 평균 객단가 계산 (sales_data 활용)
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
            
            print(f"\n📊 매장별 리텐션 분석 결과:")
            print("=" * 100)
            print(f"{'순위':<4} {'매장명':<20} {'고객수':<8} {'1개월':<8} {'3개월':<8} {'6개월':<8} {'평균금액':<12}")
            print("-" * 100)
            
            for i, row in enumerate(shop_df.iterrows(), 1):
                data = row[1]
                print(f"{i:<4} {data['shop_name']:<20} {data['total_customers']:<8,.0f} "
                      f"{data['month1_retention']:<8.1%} {data['month3_retention']:<8.1%} "
                      f"{data['month6_retention']:<8.1%} {data['avg_price']:<12,.0f}")
        
        return shop_df
        
    def create_shop_cohort_heatmaps(self, shop_df):
        """매장별 코호트 히트맵 생성"""
        print("\n🎨 매장별 코호트 히트맵 생성 중...")
        
        # 상위 6개 매장만 시각화
        top_shops = shop_df.head(6)
        
        if len(top_shops) == 0:
            print("⚠️  시각화할 매장 데이터가 없습니다.")
            return
        
        # 2x3 서브플롯 생성
        fig, axes = plt.subplots(2, 3, figsize=(20, 12))
        axes = axes.flatten()
        
        for idx, (_, shop_data) in enumerate(top_shops.iterrows()):
            if idx >= 6:
                break
                
            ax = axes[idx]
            cohort_table = shop_data['cohort_table']
            shop_name = shop_data['shop_name']
            
            # 최근 12개월 데이터만 표시
            recent_cohorts = cohort_table.tail(12)
            
            if len(recent_cohorts) == 0:
                ax.text(0.5, 0.5, f'{shop_name}\n데이터 없음', 
                       ha='center', va='center', transform=ax.transAxes)
                continue
            
            # 히트맵 생성
            sns.heatmap(recent_cohorts, 
                       annot=True, 
                       fmt='.1%', 
                       cmap='YlOrRd',
                       center=0.3,
                       vmin=0, vmax=0.8,
                       ax=ax,
                       cbar_kws={'label': 'Retention Rate'})
            
            # 제목 설정
            ax.set_title(f'{shop_name}\n고객 {shop_data["total_customers"]:,.0f}명', 
                        fontsize=12, fontweight='bold')
            ax.set_xlabel('Months Since First Visit', fontsize=10)
            ax.set_ylabel('Cohort Month', fontsize=10)
            
            # 폰트 크기 조정
            ax.tick_params(axis='both', which='major', labelsize=8)
        
        # 사용하지 않는 서브플롯 제거
        for idx in range(len(top_shops), 6):
            axes[idx].remove()
        
        plt.suptitle('Top 6 Shops - Cohort Retention Heatmaps', fontsize=16, fontweight='bold')
        plt.tight_layout()
        
        save_path = 'shop_cohort_heatmaps.png'
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        print(f"✅ 매장별 히트맵 저장: {save_path}")
        
        plt.close()
        
    def create_overall_cohort_heatmap(self):
        """전체 코호트 히트맵 생성"""
        print("\n🎨 전체 코호트 히트맵 생성 중...")
        
        cohort_table, cohort_sizes = self.create_cohort_table()
        
        if cohort_table is None:
            print("⚠️  코호트 테이블 생성 실패")
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
        plt.title('Overall Cohort Retention Analysis', fontsize=18, pad=30, fontweight='bold')
        
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
        
        save_path = 'overall_cohort_heatmap.png'
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        print(f"✅ 전체 코호트 히트맵 저장: {save_path}")
        
        plt.close()
        
    def create_retention_curves(self):
        """리텐션 커브 생성"""
        print("\n📈 리텐션 커브 생성 중...")
        
        cohort_table, cohort_sizes = self.create_cohort_table()
        
        if cohort_table is None:
            print("⚠️  코호트 테이블 생성 실패")
            return
        
        plt.figure(figsize=(14, 10))
        
        # 최근 6개 코호트만 표시
        recent_cohorts = cohort_table.tail(6)
        
        for cohort in recent_cohorts.index:
            cohort_data = recent_cohorts.loc[cohort].dropna()
            plt.plot(cohort_data.index, cohort_data.values, 
                    marker='o', label=f'{cohort}', linewidth=2.5, markersize=6)
        
        plt.title('Cohort Retention Curves', fontsize=18, pad=30, fontweight='bold')
        plt.xlabel('Months Since First Visit', fontsize=12)
        plt.ylabel('Retention Rate', fontsize=12)
        plt.legend(title='First Visit Cohort', bbox_to_anchor=(1.05, 1), loc='upper left')
        plt.grid(True, alpha=0.3)
        plt.gca().yaxis.set_major_formatter(plt.FuncFormatter(lambda y, _: '{:.0%}'.format(y)))
        
        plt.tight_layout()
        
        save_path = 'retention_curves.png'
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        print(f"✅ 리텐션 커브 저장: {save_path}")
        
        plt.close()
        
    def analyze_gender_cohorts(self):
        """성별 코호트 분석"""
        print("\n👥 성별 코호트 분석 시작...")
        
        gender_results = {}
        
        for gender in ['M', 'F']:
            gender_name = 'Male' if gender == 'M' else 'Female'
            
            # 성별 데이터 필터링
            gender_cohort_data = self.cohort_data[self.cohort_data['gender'] == gender]
            
            if len(gender_cohort_data) == 0:
                print(f"⚠️  {gender_name} 고객 데이터가 없습니다.")
                continue
            
            # 성별 코호트 테이블 생성
            gender_cohort_table, gender_cohort_sizes = self.create_cohort_table(gender_cohort_data)
            
            if gender_cohort_table is None:
                continue
            
            # 성별 리텐션 지표 계산
            total_customers = gender_cohort_sizes['total_customers'].sum()
            month1_retention = gender_cohort_table[1].mean() if 1 in gender_cohort_table.columns else 0
            month3_retention = gender_cohort_table[3].mean() if 3 in gender_cohort_table.columns else 0
            month6_retention = gender_cohort_table[6].mean() if 6 in gender_cohort_table.columns else 0
            
            gender_results[gender] = {
                'cohort_table': gender_cohort_table,
                'cohort_sizes': gender_cohort_sizes,
                'total_customers': total_customers,
                'month1_retention': month1_retention,
                'month3_retention': month3_retention,
                'month6_retention': month6_retention
            }
            
            print(f"{gender_name}: {total_customers:,} customers, "
                  f"1M: {month1_retention:.1%}, 3M: {month3_retention:.1%}, 6M: {month6_retention:.1%}")
        
        return gender_results
        
    def analyze_age_cohorts(self):
        """연령대별 코호트 분석"""
        print("\n🎂 연령대별 코호트 분석 시작...")
        
        age_results = {}
        
        for age_group in ['20-29', '30-39', '40-49', '50-59', '60+']:
            # 연령대별 데이터 필터링
            age_cohort_data = self.cohort_data[self.cohort_data['age_group'] == age_group]
            
            if len(age_cohort_data) == 0:
                print(f"⚠️  {age_group} 고객 데이터가 없습니다.")
                continue
            
            # 연령대별 코호트 테이블 생성
            age_cohort_table, age_cohort_sizes = self.create_cohort_table(age_cohort_data)
            
            if age_cohort_table is None:
                continue
            
            # 연령대별 리텐션 지표 계산
            total_customers = age_cohort_sizes['total_customers'].sum()
            month1_retention = age_cohort_table[1].mean() if 1 in age_cohort_table.columns else 0
            month3_retention = age_cohort_table[3].mean() if 3 in age_cohort_table.columns else 0
            month6_retention = age_cohort_table[6].mean() if 6 in age_cohort_table.columns else 0
            
            age_results[age_group] = {
                'cohort_table': age_cohort_table,
                'cohort_sizes': age_cohort_sizes,
                'total_customers': total_customers,
                'month1_retention': month1_retention,
                'month3_retention': month3_retention,
                'month6_retention': month6_retention
            }
            
            print(f"{age_group}: {total_customers:,} customers, "
                  f"1M: {month1_retention:.1%}, 3M: {month3_retention:.1%}, 6M: {month6_retention:.1%}")
        
        return age_results
        
    def generate_summary_report(self, shop_df, gender_results, age_results):
        """종합 분석 리포트 생성"""
        print("\n📋 종합 분석 리포트 생성 중...")
        
        # 전체 코호트 분석
        cohort_table, cohort_sizes = self.create_cohort_table()
        
        if cohort_table is None:
            print("⚠️  전체 코호트 분석 실패")
            return
        
        # 기본 통계
        total_customers = cohort_sizes['total_customers'].sum()
        total_reservations = len(self.cohort_data)
        total_shops = len(self.shop_data)
        
        # 전체 리텐션 지표
        overall_1m = cohort_table[1].mean() if 1 in cohort_table.columns else 0
        overall_3m = cohort_table[3].mean() if 3 in cohort_table.columns else 0
        overall_6m = cohort_table[6].mean() if 6 in cohort_table.columns else 0
        
        print("\n" + "="*80)
        print("📊 COHORT RETENTION ANALYSIS SUMMARY REPORT")
        print("="*80)
        
        print(f"\n🔍 OVERVIEW")
        print(f"   Total Customers: {total_customers:,}")
        print(f"   Total Reservations: {total_reservations:,}")
        print(f"   Total Shops: {total_shops:,}")
        print(f"   Analysis Period: {cohort_table.index.min()} ~ {cohort_table.index.max()}")
        
        print(f"\n📈 OVERALL RETENTION RATES")
        print(f"   1 Month: {overall_1m:.1%}")
        print(f"   3 Months: {overall_3m:.1%}")
        print(f"   6 Months: {overall_6m:.1%}")
        
        # 매장별 Top 5
        if not shop_df.empty:
            print(f"\n🏆 TOP 5 SHOPS BY 1-MONTH RETENTION")
            for i, (_, shop) in enumerate(shop_df.head(5).iterrows(), 1):
                print(f"   {i}. {shop['shop_name']}: {shop['month1_retention']:.1%} "
                      f"({shop['total_customers']:,} customers)")
        
        # 성별 분석
        if gender_results:
            print(f"\n👥 GENDER ANALYSIS")
            for gender, data in gender_results.items():
                gender_name = 'Male' if gender == 'M' else 'Female'
                print(f"   {gender_name}: {data['total_customers']:,} customers, "
                      f"1M: {data['month1_retention']:.1%}, 3M: {data['month3_retention']:.1%}")
        
        # 연령대별 분석
        if age_results:
            print(f"\n🎂 AGE GROUP ANALYSIS")
            for age_group, data in age_results.items():
                print(f"   {age_group}: {data['total_customers']:,} customers, "
                      f"1M: {data['month1_retention']:.1%}, 3M: {data['month3_retention']:.1%}")
        
        print("\n" + "="*80)
        print("📁 Generated Files:")
        print("   - overall_cohort_heatmap.png")
        print("   - shop_cohort_heatmaps.png")
        print("   - retention_curves.png")
        print("="*80)
        
    def analyze_average_order_value(self):
        """평균 객단가(AOV) 계산 및 간단 리포트 반환"""
        if self.sales_data is None or len(self.sales_data) == 0:
            return None

        total_sales = self.sales_data['total_amount'].sum()
        total_orders = len(self.sales_data)
        avg_order_value = total_sales / total_orders if total_orders else 0

        return {
            'total_sales': total_sales,
            'total_orders': total_orders,
            'avg_order_value': avg_order_value
        }
        
    def run_full_analysis(self):
        """전체 코호트 분석 실행"""
        try:
            print("🚀 DevEagles 코호트 리텐션 분석 시작")
            print("="*60)
            
            # 1. 데이터 로드 및 전처리
            self.load_data()
            self.prepare_cohort_data()
            
            # 2. 전체 코호트 분석
            self.create_overall_cohort_heatmap()
            self.create_retention_curves()
            
            # 3. 매장별 분석
            shop_df = self.analyze_shop_cohorts()
            if not shop_df.empty:
                self.create_shop_cohort_heatmaps(shop_df)
            
            # 4. 성별 분석
            gender_results = self.analyze_gender_cohorts()
            
            # 5. 연령대별 분석
            age_results = self.analyze_age_cohorts()
            
            # 6. 종합 리포트 생성
            self.generate_summary_report(shop_df, gender_results, age_results)
            
            # 7. 평균 객단가 계산
            aov_results = self.analyze_average_order_value()

            # 간단 gender/age AOV 집계
            gender_aov_df = None
            age_aov_df = None
            if self.sales_data is not None and len(self.sales_data) > 0:
                gender_aov_df = self.sales_data.groupby('gender')['total_amount'].mean().reset_index()
                current_date = pd.Timestamp.now()
                tmp = self.sales_data.copy()
                tmp['age'] = (current_date - pd.to_datetime(tmp['birthdate'])).dt.days / 365.25
                tmp['age_group'] = pd.cut(tmp['age'], bins=[0,30,40,50,60,100], labels=['20대','30대','40대','50대','60대+'], right=False)
                age_aov_df = tmp.groupby('age_group')['total_amount'].mean().reset_index()
            
            print(f"\n✅ 코호트 리텐션 분석 완료!")
            
            return {
                'shop_analysis': shop_df,
                'gender_analysis': gender_results,
                'age_analysis': age_results,
                'overall_cohort_table': self.create_cohort_table()[0] if hasattr(self, 'cohort_data') else None,
                'total_customers': len(self.customer_data),
                'total_reservations': len(self.cohort_data),
                'aov_results': aov_results,
                'gender_aov': gender_aov_df,
                'age_aov': age_aov_df
            }
            
        except Exception as e:
            print(f"❌ 분석 중 오류 발생: {e}")
            import traceback
            traceback.print_exc()
            return None 