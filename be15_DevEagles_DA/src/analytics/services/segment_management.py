"""CRM 세그먼트 관리 서비스
"""

from datetime import datetime
from typing import Dict, List, Optional, Tuple

import pandas as pd
from sqlalchemy import text

from analytics.core.database import get_crm_db
from analytics.core.logging import get_logger
from analytics.services.churn_prediction import ChurnPredictionService


class SegmentManagementService:
    """세그먼트 자동 관리 서비스"""
    
    def __init__(self, crm_engine=None):
        self.logger = get_logger(__name__)
        if crm_engine is None:
            self.crm_engine = get_crm_db()
        else:
            self.crm_engine = crm_engine
        
        self.churn_service = ChurnPredictionService(crm_engine)
        
        # 관리할 세그먼트 정의
        self.target_segments = {
            'VIP_ATTENTION': {
                'title': 'VIP 고객 패턴 변화/이상',
                'color_code': '#FF6B35'
            },
            'CHURN_RISK_HIGH': {
                'title': '모델이 예측한 이탈 위험 고객',
                'color_code': '#DC143C'
            }
        }

    def _get_or_create_segment(self, segment_tag: str, segment_title: str, color_code: str) -> int:
        """세그먼트가 없으면 생성하고 segment_id를 반환"""
        with self.crm_engine.begin() as conn:
            # 기존 세그먼트 조회
            result = conn.execute(
                text("SELECT segment_id FROM segment WHERE segment_tag = :tag"),
                {"tag": segment_tag}
            ).fetchone()
            
            if result:
                return result[0]
            
            # 새 세그먼트 생성
            result = conn.execute(
                text("""
                    INSERT INTO segment (segment_tag, segment_title, color_code, created_at, modified_at)
                    VALUES (:tag, :title, :color, NOW(), NOW())
                """),
                {
                    "tag": segment_tag,
                    "title": segment_title,
                    "color": color_code
                }
            )
            
            # 생성된 ID 조회
            segment_id_result = conn.execute(
                text("SELECT segment_id FROM segment WHERE segment_tag = :tag"),
                {"tag": segment_tag}
            ).fetchone()
            
            segment_id = segment_id_result[0]
            self.logger.info(f"새 세그먼트 생성: {segment_tag} (ID: {segment_id})")
            return segment_id

    def _remove_existing_segments(self, customer_ids: List[int], segment_tags: List[str]) -> int:
        """고객들로부터 기존 세그먼트 제거"""
        if not customer_ids:
            return 0
            
        with self.crm_engine.begin() as conn:
            # 세그먼트 ID 조회
            segment_ids = []
            for tag in segment_tags:
                result = conn.execute(
                    text("SELECT segment_id FROM segment WHERE segment_tag = :tag"),
                    {"tag": tag}
                ).fetchone()
                if result:
                    segment_ids.append(result[0])
            
            if not segment_ids:
                return 0
            
            # 기존 세그먼트 삭제
            customer_ids_str = ','.join(map(str, customer_ids))
            segment_ids_str = ','.join(map(str, segment_ids))
            
            result = conn.execute(
                text(f"""
                    DELETE FROM segment_by_customer 
                    WHERE customer_id IN ({customer_ids_str}) 
                    AND segment_id IN ({segment_ids_str})
                """)
            )
            
            removed_count = result.rowcount
            self.logger.info(f"기존 세그먼트 제거: {removed_count}건")
            return removed_count

    def _assign_segments_to_customers(self, assignments: List[Tuple[int, str]]) -> int:
        """고객들에게 세그먼트 할당"""
        if not assignments:
            return 0
            
        assigned_count = 0
        with self.crm_engine.begin() as conn:
            for customer_id, segment_tag in assignments:
                # 세그먼트 정보 가져오기
                segment_info = self.target_segments[segment_tag]
                segment_id = self._get_or_create_segment(
                    segment_tag, 
                    segment_info['title'], 
                    segment_info['color_code']
                )
                
                # 중복 체크
                existing = conn.execute(
                    text("""
                        SELECT 1 FROM segment_by_customer 
                        WHERE customer_id = :customer_id AND segment_id = :segment_id
                    """),
                    {"customer_id": customer_id, "segment_id": segment_id}
                ).fetchone()
                
                if not existing:
                    # 세그먼트 할당
                    conn.execute(
                        text("""
                            INSERT INTO segment_by_customer (customer_id, segment_id, assigned_at)
                            VALUES (:customer_id, :segment_id, NOW())
                        """),
                        {"customer_id": customer_id, "segment_id": segment_id}
                    )
                    assigned_count += 1
        
        self.logger.info(f"새 세그먼트 할당: {assigned_count}건")
        return assigned_count

    def _get_current_segment_assignments(self) -> Dict[str, List[int]]:
        """현재 VIP_ATTENTION, CHURN_RISK_HIGH 세그먼트가 할당된 고객 목록 조회"""
        assignments = {}
        
        with self.crm_engine.begin() as conn:
            for segment_tag in self.target_segments.keys():
                result = conn.execute(
                    text("""
                        SELECT sbc.customer_id
                        FROM segment_by_customer sbc
                        JOIN segment s ON sbc.segment_id = s.segment_id
                        WHERE s.segment_tag = :tag
                    """),
                    {"tag": segment_tag}
                ).fetchall()
                
                assignments[segment_tag] = [row[0] for row in result]
        
        return assignments

    def update_churn_risk_segments(self) -> Dict:
        """이탈 위험 세그먼트 업데이트 실행"""
        self.logger.info("이탈 위험 세그먼트 업데이트 시작")
        
        try:
            # 1. 이탈 예측 분석 실행
            analysis_result = self.churn_service.run_full_analysis()
            predictions = analysis_result['predictions']
            
            # 2. 현재 세그먼트 할당 상태 조회
            current_assignments = self._get_current_segment_assignments()
            
            # 3. 새로운 세그먼트 할당 대상 결정
            new_vip_attention = []
            new_churn_risk_high = []
            
            for pred in predictions:
                customer_id = pred['customer_id']
                churn_risk_tag = pred['churn_risk_tag']
                
                if churn_risk_tag == 'VIP_ATTENTION':
                    new_vip_attention.append(customer_id)
                elif churn_risk_tag == 'CHURN_RISK_HIGH':
                    new_churn_risk_high.append(customer_id)
            
            # 4. 모든 고객으로부터 기존 세그먼트 제거 (깔끔한 업데이트를 위해)
            all_customers = [pred['customer_id'] for pred in predictions]
            removed_count = self._remove_existing_segments(
                all_customers, 
                list(self.target_segments.keys())
            )
            
            # 5. 새로운 세그먼트 할당
            assignments = []
            for customer_id in new_vip_attention:
                assignments.append((customer_id, 'VIP_ATTENTION'))
            for customer_id in new_churn_risk_high:
                assignments.append((customer_id, 'CHURN_RISK_HIGH'))
            
            assigned_count = self._assign_segments_to_customers(assignments)
            
            # 6. 결과 정리
            result = {
                'success': True,
                'timestamp': datetime.now().isoformat(),
                'analysis_summary': {
                    'total_customers': analysis_result['customers'],
                    'churn_rate': analysis_result['churn_rate'],
                    'best_model': analysis_result['best_model']
                },
                'segment_updates': {
                    'removed_count': removed_count,
                    'assigned_count': assigned_count,
                    'vip_attention_count': len(new_vip_attention),
                    'churn_risk_high_count': len(new_churn_risk_high)
                },
                'previous_assignments': {
                    'VIP_ATTENTION': len(current_assignments.get('VIP_ATTENTION', [])),
                    'CHURN_RISK_HIGH': len(current_assignments.get('CHURN_RISK_HIGH', []))
                },
                'new_assignments': {
                    'VIP_ATTENTION': len(new_vip_attention),
                    'CHURN_RISK_HIGH': len(new_churn_risk_high)
                }
            }
            
            self.logger.info(
                f"세그먼트 업데이트 완료 - 제거: {removed_count}, 할당: {assigned_count}"
            )
            
            return result
            
        except Exception as e:
            self.logger.error(f"세그먼트 업데이트 실패: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }

    def get_segment_statistics(self) -> Dict:
        """현재 세그먼트 통계 조회"""
        try:
            with self.crm_engine.begin() as conn:
                # 전체 세그먼트별 고객 수
                result = conn.execute(
                    text("""
                        SELECT s.segment_tag, s.segment_title, COUNT(sbc.customer_id) as customer_count
                        FROM segment s
                        LEFT JOIN segment_by_customer sbc ON s.segment_id = sbc.segment_id
                        GROUP BY s.segment_id, s.segment_tag, s.segment_title
                        ORDER BY customer_count DESC
                    """)
                ).fetchall()
                
                segment_stats = []
                for row in result:
                    segment_stats.append({
                        'segment_tag': row[0],
                        'segment_title': row[1],
                        'customer_count': row[2]
                    })
                
                # 이탈 위험 세그먼트 상세 정보
                risk_segments = {}
                for segment_tag in self.target_segments.keys():
                    customers_result = conn.execute(
                        text("""
                            SELECT c.customer_id, c.customer_name, c.visit_count, c.total_revenue
                            FROM customer c
                            JOIN segment_by_customer sbc ON c.customer_id = sbc.customer_id
                            JOIN segment s ON sbc.segment_id = s.segment_id
                            WHERE s.segment_tag = :tag
                            ORDER BY c.total_revenue DESC
                            LIMIT 10
                        """),
                        {"tag": segment_tag}
                    ).fetchall()
                    
                    risk_segments[segment_tag] = [
                        {
                            'customer_id': row[0],
                            'customer_name': row[1],
                            'visit_count': row[2],
                            'total_revenue': float(row[3]) if row[3] else 0
                        }
                        for row in customers_result
                    ]
                
                return {
                    'success': True,
                    'timestamp': datetime.now().isoformat(),
                    'all_segments': segment_stats,
                    'risk_segment_details': risk_segments
                }
                
        except Exception as e:
            self.logger.error(f"세그먼트 통계 조회 실패: {str(e)}")
            return {
                'success': False,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }