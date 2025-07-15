"""
Customer Segmentation Service - DuckDB 버전

DuckDB에서 데이터를 조회하여 고객 세그멘테이션을 수행합니다.
"""

import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
from enum import Enum

import pandas as pd
import numpy as np
from analytics.core.config import get_settings
from analytics.core.logging import get_logger
from analytics.core.database import get_analytics_db, get_crm_db


class CustomerSegment(str, Enum):
    """고객 세그먼트 정의."""
    NEW = "new"
    GROWING = "growing"
    LOYAL = "loyal"
    VIP = "vip"
    DORMANT = "dormant"


class CustomerSegmentationServiceDuckDB:
    """고객 세그멘테이션 서비스 - DuckDB 버전"""
    
    def __init__(self):
        self.settings = get_settings()
        self.logger = get_logger("segmentation_duckdb")
        self.analytics_db = get_analytics_db()
        self.crm_db = get_crm_db()
        
        # 세그멘테이션 기준값들
        self.new_customer_days = 30
        self.new_customer_visits = 3
        self.growing_customer_visits = 10
        self.vip_amount_threshold = 500000  # 50만원
        self.dormant_days = 180
        
    def segment_all_customers(self) -> Dict[str, int]:
        """모든 고객을 세그멘테이션하고 결과를 반환."""
        self.logger.info("DuckDB 기반 고객 세그멘테이션 시작")
        
        try:
            # 고객 분석 데이터 조회
            customers_df = self._get_customer_analytics_data()
            
            if customers_df.empty:
                self.logger.warning("세그멘테이션을 위한 고객 데이터가 없습니다")
                return {}
            
            # 세그멘테이션 수행
            segmented_customers = self._perform_segmentation(customers_df)
            
            # 결과 집계
            segment_counts = segmented_customers['segment'].value_counts().to_dict()
            
            self.logger.info(f"세그멘테이션 완료. 총 {len(segmented_customers)}명 처리")
            for segment, count in segment_counts.items():
                self.logger.info(f"{segment}: {count}명")
            
            return segment_counts
            
        except Exception as e:
            self.logger.error(f"고객 세그멘테이션 실패: {e}")
            raise
        finally:
            self.data_service.close()
    
    def segment_customer(self, customer_id: int) -> str:
        """특정 고객의 세그먼트를 계산하고 반환."""
        try:
            customer_data = self._get_customer_data(customer_id)
            
            if customer_data is None:
                raise ValueError(f"고객 {customer_id}을 찾을 수 없습니다")
            
            segment = self._calculate_segment(customer_data)
            
            self.logger.info(f"고객 {customer_id} 세그먼트: {segment}")
            return segment
            
        except Exception as e:
            self.logger.error(f"고객 {customer_id} 세그멘테이션 실패: {e}")
            raise
        finally:
            self.data_service.close()
    
    def get_segment_distribution(self) -> Dict[str, Dict[str, int]]:
        """세그먼트별 고객 분포 통계."""
        try:
            # 고객 데이터와 세그먼트 정보 조회
            customers_df = self._get_customer_analytics_data()
            
            if customers_df.empty:
                return {}
                
            # 세그멘테이션 수행
            segmented_customers = self._perform_segmentation(customers_df)
            
            # 세그먼트별 통계 계산
            distribution = {}
            
            for segment in segmented_customers['segment'].unique():
                segment_data = segmented_customers[segmented_customers['segment'] == segment]
                
                distribution[segment] = {
                    'count': len(segment_data),
                    'avg_visits': round(segment_data['visit_count'].mean(), 1),
                    'avg_amount': round(segment_data['total_revenue'].mean(), 2),
                    'avg_days_since_visit': round(segment_data['days_since_last_visit'].mean(), 1)
                }
            
            return distribution
            
        except Exception as e:
            self.logger.error(f"세그먼트 분포 조회 실패: {e}")
            raise
        finally:
            self.data_service.close()
    
    def _get_customer_analytics_data(self) -> pd.DataFrame:
        """고객 분석 데이터 조회 (DuckDB의 customer_analytics 테이블 사용)."""
        try:
            # DuckDB의 customer_analytics 테이블에서 직접 조회
            query = """
            SELECT 
                customer_id,
                name,
                total_visits as visit_count,
                total_amount as total_revenue,
                first_visit_date,
                last_visit_date,
                days_since_last_visit,
                lifecycle_days,
                visits_3m,
                amount_3m
            FROM customer_analytics
            """
            
            customers_df = self.analytics_db.execute(query).fetchdf()
            
            if customers_df.empty:
                self.logger.warning("customer_analytics 테이블이 비어있습니다")
                return pd.DataFrame()
            
            # 결측값 처리
            customers_df['visits_3m'] = customers_df['visits_3m'].fillna(0)
            customers_df['amount_3m'] = customers_df['amount_3m'].fillna(0)
            customers_df['days_since_last_visit'] = customers_df['days_since_last_visit'].fillna(9999)
            
            self.logger.info(f"customer_analytics에서 {len(customers_df):,}명 고객 데이터 조회")
            return customers_df
            
        except Exception as e:
            self.logger.error(f"customer_analytics 데이터 조회 실패: {e}")
            raise
    
    def _get_customer_data(self, customer_id: int) -> Optional[Dict]:
        """특정 고객의 데이터 조회."""
        try:
            customers_df = self._get_customer_analytics_data()
            customer_row = customers_df[customers_df['customer_id'] == customer_id]
            
            if customer_row.empty:
                return None
                
            return customer_row.iloc[0].to_dict()
            
        except Exception as e:
            self.logger.error(f"고객 {customer_id} 데이터 조회 실패: {e}")
            return None
    
    def _perform_segmentation(self, customers_df: pd.DataFrame) -> pd.DataFrame:
        """데이터프레임의 모든 고객에 대해 세그멘테이션 수행."""
        customers_df['segment'] = customers_df.apply(
            lambda row: self._calculate_segment(row.to_dict()), axis=1
        )
        
        return customers_df
    
    def _calculate_segment(self, customer_data: Dict) -> str:
        """개별 고객의 세그먼트 계산."""
        visit_count = customer_data.get('visit_count', 0) or customer_data.get('total_visits', 0)
        total_revenue = customer_data.get('total_revenue', 0) or customer_data.get('total_amount', 0)
        days_since_last_visit = customer_data.get('days_since_last_visit', 0)
        lifecycle_days = customer_data.get('lifecycle_days', 0)
        visits_3m = customer_data.get('visits_3m', 0)
        
        # 1. 휴면 고객 (6개월 이상 미방문)
        if days_since_last_visit >= self.dormant_days:
            return CustomerSegment.DORMANT
        
        # 2. VIP 고객 (고액 결제 + 충성도)
        if (total_revenue >= self.vip_amount_threshold and 
            visit_count >= self.growing_customer_visits and
            days_since_last_visit <= 30):
            return CustomerSegment.VIP
        
        # 3. 충성 고객 (방문 10회 이상, 정기적 방문)
        if (visit_count >= self.growing_customer_visits and 
            days_since_last_visit <= 45):
            return CustomerSegment.LOYAL
        
        # 4. 성장 고객 (방문 3-9회, 꾸준한 활동)
        if (visit_count >= self.new_customer_visits and 
            visit_count < self.growing_customer_visits and
            visits_3m > 0):
            return CustomerSegment.GROWING
        
        # 5. 신규 고객 (첫 방문 후 30일 이내 또는 방문 3회 미만)
        if (lifecycle_days <= self.new_customer_days or 
            visit_count < self.new_customer_visits):
            return CustomerSegment.NEW
        
        # 기본값: 성장 고객
        return CustomerSegment.GROWING
    
    def get_segment_insights(self, segment: str) -> Dict:
        """특정 세그먼트의 인사이트 제공."""
        try:
            customers_df = self._get_customer_analytics_data()
            
            if customers_df.empty:
                return {"error": "고객 데이터가 없습니다"}
            
            # 세그멘테이션 수행
            segmented_customers = self._perform_segmentation(customers_df)
            segment_data = segmented_customers[segmented_customers['segment'] == segment]
            
            if segment_data.empty:
                return {"error": f"세그먼트 '{segment}'에 해당하는 고객이 없습니다"}
            
            insights = {
                "segment": segment,
                "total_customers": len(segment_data),
                "avg_visits": round(segment_data['visit_count'].mean(), 1),
                "avg_amount": round(segment_data['total_revenue'].mean(), 2),
                "avg_days_since_visit": round(segment_data['days_since_last_visit'].mean(), 1),
                "amount_range": {
                    "min": segment_data['total_revenue'].min(),
                    "max": segment_data['total_revenue'].max()
                },
                "avg_recent_visits": round(segment_data['visits_3m'].mean(), 1)
            }
            
            # 세그먼트별 권장 액션
            insights["recommended_actions"] = self._get_segment_recommendations(segment)
            
            return insights
            
        except Exception as e:
            self.logger.error(f"세그먼트 {segment} 인사이트 조회 실패: {e}")
            raise
        finally:
            self.data_service.close()
    
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
            
            # 매장별 세그먼트 분포
            customers_df = self._get_customer_analytics_data()
            segmented_customers = self._perform_segmentation(customers_df)
            
            shop_segment_analysis = segmented_customers.groupby(['shop_name', 'segment']).size().unstack(fill_value=0)
            
            trend_analysis = {
                "current_distribution": current_distribution,
                "shop_segment_distribution": shop_segment_analysis.to_dict(),
                "analysis_period_days": days,
                "total_customers": len(segmented_customers),
                "data_source": "DuckDB"
            }
            
            return trend_analysis
            
        except Exception as e:
            self.logger.error(f"세그먼트 트렌드 분석 실패: {e}")
            raise
        finally:
            self.data_service.close()
    
    def run_full_analysis(self) -> Dict:
        """전체 세그멘테이션 분석 실행"""
        try:
            self.logger.info("DuckDB 기반 고객 세그멘테이션 분석 시작")
            
            # 1. 전체 고객 세그멘테이션
            segment_counts = self.segment_all_customers()
            
            # 2. 세그먼트 분포 통계
            distribution = self.get_segment_distribution()
            
            # 3. 트렌드 분석
            trends = self.analyze_segment_trends()
            
            # 4. 각 세그먼트별 인사이트 (주요 세그먼트만)
            segment_insights = {}
            for segment in [CustomerSegment.VIP, CustomerSegment.LOYAL, CustomerSegment.DORMANT]:
                try:
                    insights = self.get_segment_insights(segment)
                    if "error" not in insights:
                        segment_insights[segment] = insights
                except:
                    continue
            
            result = {
                "segment_counts": segment_counts,
                "distribution": distribution,
                "trends": trends,
                "insights": segment_insights,
                "data_source": "DuckDB",
                "analysis_timestamp": datetime.now().isoformat()
            }
            
            self.logger.info("DuckDB 기반 고객 세그멘테이션 분석 완료")
            return result
            
        except Exception as e:
            self.logger.error(f"전체 세그멘테이션 분석 실패: {e}")
            raise