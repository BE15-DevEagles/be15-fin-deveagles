"""Customer Segmentation Service."""

import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
from enum import Enum

import pandas as pd
from analytics.core.database import get_analytics_db, get_crm_db
from analytics.core.config import get_settings
from analytics.core.logging import get_logger


class CustomerSegment(str, Enum):
    """고객 세그먼트 정의."""
    NEW = "new"              # 신규 고객 (첫 방문 후 30일 이내, 방문 3회 미만)
    GROWING = "growing"      # 성장 고객 (방문 3-10회, 꾸준한 방문)
    LOYAL = "loyal"          # 충성 고객 (방문 10회 이상, 정기적 방문)
    VIP = "vip"              # VIP 고객 (고액 결제, 프리미엄 서비스)
    DORMANT = "dormant"      # 휴면 고객 (6개월 이상 미방문)


class CustomerSegmentationService:
    """고객 세그멘테이션 서비스."""
    
    def __init__(self):
        self.settings = get_settings()
        self.logger = get_logger("segmentation")
        self.analytics_db = get_analytics_db()
        
        # 세그멘테이션 기준값들
        self.new_customer_days = 30
        self.new_customer_visits = 3
        self.growing_customer_visits = 10
        self.vip_amount_threshold = 500000  # 50만원
        self.dormant_days = 180
        
    def segment_all_customers(self) -> Dict[str, int]:
        """모든 고객을 세그멘테이션하고 결과를 반환."""
        self.logger.info("Starting customer segmentation for all customers")
        
        try:
            # 고객 분석 데이터 조회
            customers_df = self._get_customer_analytics_data()
            
            if customers_df.empty:
                self.logger.warning("No customer data found for segmentation")
                return {}
            
            # 세그멘테이션 수행
            segmented_customers = self._perform_segmentation(customers_df)
            
            # 결과를 데이터베이스에 업데이트
            updated_counts = self._update_customer_segments(segmented_customers)
            
            self.logger.info(f"Segmentation completed. Updated {sum(updated_counts.values())} customers")
            return updated_counts
            
        except Exception as e:
            self.logger.error(f"Customer segmentation failed: {e}")
            raise
    
    def segment_customer(self, customer_id: int) -> str:
        """특정 고객의 세그먼트를 계산하고 반환."""
        try:
            customer_data = self._get_customer_data(customer_id)
            
            if customer_data is None:
                raise ValueError(f"Customer {customer_id} not found")
            
            segment = self._calculate_segment(customer_data)
            
            # 데이터베이스 업데이트
            self._update_customer_segment(customer_id, segment)
            
            return segment
            
        except Exception as e:
            self.logger.error(f"Failed to segment customer {customer_id}: {e}")
            raise
    
    def get_segment_distribution(self) -> Dict[str, Dict[str, int]]:
        """세그먼트별 고객 분포 통계."""
        try:
            query = """
            SELECT 
                segment,
                COUNT(*) as customer_count,
                AVG(total_visits) as avg_visits,
                AVG(total_amount) as avg_amount,
                AVG(days_since_last_visit) as avg_days_since_visit
            FROM customer_analytics 
            GROUP BY segment
            ORDER BY customer_count DESC
            """
            
            result = self.analytics_db.execute(query).fetchall()
            
            distribution = {}
            for row in result:
                distribution[row[0]] = {
                    'count': row[1],
                    'avg_visits': round(row[2], 1) if row[2] else 0,
                    'avg_amount': round(row[3], 2) if row[3] else 0,
                    'avg_days_since_visit': round(row[4], 1) if row[4] else 0
                }
            
            return distribution
            
        except Exception as e:
            self.logger.error(f"Failed to get segment distribution: {e}")
            raise
    
    def _get_customer_analytics_data(self) -> pd.DataFrame:
        """고객 분석 데이터 조회."""
        query = """
        SELECT 
            customer_id,
            name,
            total_visits,
            total_amount,
            first_visit_date,
            last_visit_date,
            days_since_last_visit,
            lifecycle_days,
            visits_3m,
            amount_3m,
            segment as current_segment
        FROM customer_analytics
        """
        
        return pd.read_sql(query, self.analytics_db)
    
    def _get_customer_data(self, customer_id: int) -> Optional[Dict]:
        """특정 고객의 데이터 조회."""
        query = """
        SELECT 
            customer_id,
            name,
            total_visits,
            total_amount,
            first_visit_date,
            last_visit_date,
            days_since_last_visit,
            lifecycle_days,
            visits_3m,
            amount_3m
        FROM customer_analytics
        WHERE customer_id = ?
        """
        
        result = self.analytics_db.execute(query, [customer_id]).fetchone()
        
        if result:
            return {
                'customer_id': result[0],
                'name': result[1],
                'total_visits': result[2],
                'total_amount': result[3],
                'first_visit_date': result[4],
                'last_visit_date': result[5],
                'days_since_last_visit': result[6],
                'lifecycle_days': result[7],
                'visits_3m': result[8],
                'amount_3m': result[9]
            }
        
        return None
    
    def _perform_segmentation(self, customers_df: pd.DataFrame) -> pd.DataFrame:
        """데이터프레임의 모든 고객에 대해 세그멘테이션 수행."""
        customers_df['new_segment'] = customers_df.apply(
            lambda row: self._calculate_segment(row.to_dict()), axis=1
        )
        
        return customers_df
    
    def _calculate_segment(self, customer_data: Dict) -> str:
        """개별 고객의 세그먼트 계산."""
        total_visits = customer_data.get('total_visits', 0)
        total_amount = customer_data.get('total_amount', 0)
        days_since_last_visit = customer_data.get('days_since_last_visit', 0)
        lifecycle_days = customer_data.get('lifecycle_days', 0)
        visits_3m = customer_data.get('visits_3m', 0)
        amount_3m = customer_data.get('amount_3m', 0)
        
        # 1. 휴면 고객 (6개월 이상 미방문)
        if days_since_last_visit >= self.dormant_days:
            return CustomerSegment.DORMANT
        
        # 3. VIP 고객 (고액 결제 + 충성도)
        if (total_amount >= self.vip_amount_threshold and 
            total_visits >= self.growing_customer_visits and
            days_since_last_visit <= 30):
            return CustomerSegment.VIP
        
        # 4. 충성 고객 (방문 10회 이상, 정기적 방문)
        if (total_visits >= self.growing_customer_visits and 
            days_since_last_visit <= 45):
            return CustomerSegment.LOYAL
        
        # 5. 성장 고객 (방문 3-9회, 꾸준한 활동)
        if (total_visits >= self.new_customer_visits and 
            total_visits < self.growing_customer_visits and
            visits_3m > 0):
            return CustomerSegment.GROWING
        
        # 6. 신규 고객 (첫 방문 후 30일 이내 또는 방문 3회 미만)
        if (lifecycle_days <= self.new_customer_days or 
            total_visits < self.new_customer_visits):
            return CustomerSegment.NEW
        
        # 기본값: 성장 고객
        return CustomerSegment.GROWING
    
    def _update_customer_segments(self, customers_df: pd.DataFrame) -> Dict[str, int]:
        """고객 세그먼트를 데이터베이스에 업데이트."""
        segment_counts = {}
        updated_count = 0
        
        for _, row in customers_df.iterrows():
            customer_id = row['customer_id']
            new_segment = row['new_segment']
            current_segment = row.get('current_segment')
            
            # 세그먼트가 변경된 경우만 업데이트
            if current_segment != new_segment:
                self._update_customer_segment(customer_id, new_segment)
                updated_count += 1
            
            # 세그먼트별 카운트
            segment_counts[new_segment] = segment_counts.get(new_segment, 0) + 1
        
        self.logger.info(f"Updated {updated_count} customers with new segments")
        return segment_counts
    
    def _update_customer_segment(self, customer_id: int, segment: str):
        """개별 고객의 세그먼트 업데이트."""
        query = """
        UPDATE customer_analytics 
        SET segment = ?, segment_updated_at = ?
        WHERE customer_id = ?
        """
        
        self.analytics_db.execute(query, [segment, datetime.now(), customer_id])
    
    def get_segment_insights(self, segment: str) -> Dict:
        """특정 세그먼트의 인사이트 제공."""
        try:
            query = """
            SELECT 
                COUNT(*) as total_customers,
                AVG(total_visits) as avg_visits,
                AVG(total_amount) as avg_amount,
                AVG(days_since_last_visit) as avg_days_since_visit,
                MIN(total_amount) as min_amount,
                MAX(total_amount) as max_amount,
                AVG(visits_3m) as avg_recent_visits
            FROM customer_analytics 
            WHERE segment = ?
            """
            
            result = self.analytics_db.execute(query, [segment]).fetchone()
            
            if not result or result[0] == 0:
                return {"error": f"No customers found in segment: {segment}"}
            
            insights = {
                "segment": segment,
                "total_customers": result[0],
                "avg_visits": round(result[1], 1) if result[1] else 0,
                "avg_amount": round(result[2], 2) if result[2] else 0,
                "avg_days_since_visit": round(result[3], 1) if result[3] else 0,
                "amount_range": {
                    "min": result[4] if result[4] else 0,
                    "max": result[5] if result[5] else 0
                },
                "avg_recent_visits": round(result[6], 1) if result[6] else 0
            }
            
            # 세그먼트별 권장 액션
            insights["recommended_actions"] = self._get_segment_recommendations(segment)
            
            return insights
            
        except Exception as e:
            self.logger.error(f"Failed to get insights for segment {segment}: {e}")
            raise
    
    def _get_segment_recommendations(self, segment: str) -> List[str]:
        """세그먼트별 권장 마케팅 액션."""
        recommendations = {
            CustomerSegment.NEW: [
                "환영 메시지 및 첫 방문 할인 제공",
                "서비스 만족도 조사 실시",
                "다음 방문 예약 유도 캠페인",
                "신규 고객 전용 패키지 상품 제안"
            ],
            CustomerSegment.GROWING: [
                "정기 방문 할인 혜택 제공",
                "서비스 업그레이드 제안",
                "멤버십 가입 유도",
                "친구 추천 이벤트 참여 유도"
            ],
            CustomerSegment.LOYAL: [
                "VIP 등급 승급 혜택 안내",
                "프리미엄 서비스 체험 기회 제공",
                "생일/기념일 특별 할인",
                "충성 고객 감사 이벤트 초대"
            ],
            CustomerSegment.VIP: [
                "개인 맞춤 서비스 제공",
                "우선 예약 서비스",
                "독점 신제품/서비스 체험",
                "VIP 전용 이벤트 초대"
            ],
            CustomerSegment.DORMANT: [
                "컴백 할인 이벤트",
                "신규 서비스 소개 및 체험 기회",
                "고객 상담 전화",
                "재활성화 특별 패키지 제안"
            ]
        }
        
        return recommendations.get(segment, ["개별 맞춤 상담 제공"])
    
    def analyze_segment_trends(self, days: int = 30) -> Dict:
        """세그먼트 변화 트렌드 분석."""
        try:
            # 현재 세그먼트 분포
            current_distribution = self.get_segment_distribution()
            
            # 과거 데이터와 비교 (segment_updated_at 기준)
            past_date = datetime.now() - timedelta(days=days)
            
            query = """
            SELECT segment, COUNT(*) as count
            FROM customer_analytics 
            WHERE segment_updated_at >= ?
            GROUP BY segment
            """
            
            recent_changes = self.analytics_db.execute(query, [past_date]).fetchall()
            
            trend_analysis = {
                "current_distribution": current_distribution,
                "recent_changes": {row[0]: row[1] for row in recent_changes},
                "analysis_period_days": days,
                "total_customers": sum(dist['count'] for dist in current_distribution.values())
            }
            
            return trend_analysis
            
        except Exception as e:
            self.logger.error(f"Failed to analyze segment trends: {e}")
            raise 