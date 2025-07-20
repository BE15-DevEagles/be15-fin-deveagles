"""세그먼트 자동 업데이트 스케줄러
이탈 예측 분석 결과를 바탕으로 VIP_ATTENTION과 CHURN_RISK_HIGH 세그먼트를 자동 업데이트
"""

import sys
import os
from datetime import datetime
import schedule
import time

# 프로젝트 루트를 Python 경로에 추가
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

from analytics.core.logging import get_logger
from analytics.services.segment_management import SegmentManagementService


class SegmentScheduler:
    """세그먼트 업데이트 스케줄러"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.segment_service = SegmentManagementService()
        
    def run_daily_segment_update(self):
        """일일 세그먼트 업데이트 실행"""
        start_time = datetime.now()
        self.logger.info(f"=== 일일 세그먼트 업데이트 시작: {start_time.strftime('%Y-%m-%d %H:%M:%S')} ===")
        
        try:
            # 1. 이탈 위험 세그먼트 업데이트
            self.logger.info("1. 이탈 위험 세그먼트 업데이트 실행 중...")
            update_result = self.segment_service.update_churn_risk_segments()
            
            if update_result['success']:
                self.logger.info("[SUCCESS] 세그먼트 업데이트 성공")
                self._log_update_summary(update_result)
            else:
                self.logger.error(f"[ERROR] 세그먼트 업데이트 실패: {update_result.get('error', 'Unknown error')}")
                return False
            
            # 2. 업데이트 후 통계 조회
            self.logger.info("2. 업데이트된 세그먼트 통계 조회 중...")
            stats_result = self.segment_service.get_segment_statistics()
            
            if stats_result['success']:
                self.logger.info("[SUCCESS] 세그먼트 통계 조회 성공")
                self._log_segment_statistics(stats_result)
            else:
                self.logger.warning(f"[WARNING] 세그먼트 통계 조회 실패: {stats_result.get('error', 'Unknown error')}")
            
            # 3. 실행 시간 로깅
            end_time = datetime.now()
            duration = end_time - start_time
            self.logger.info(f"=== 일일 세그먼트 업데이트 완료: {end_time.strftime('%Y-%m-%d %H:%M:%S')} ===")
            self.logger.info(f"총 실행 시간: {duration.total_seconds():.2f}초")
            
            return True
            
        except Exception as e:
            self.logger.error(f"[ERROR] 일일 세그먼트 업데이트 중 예외 발생: {str(e)}")
            return False
    
    def _log_update_summary(self, result):
        """업데이트 결과 요약 로깅"""
        analysis = result['analysis_summary']
        updates = result['segment_updates']
        
        self.logger.info("[ANALYSIS] 분석 결과 요약:")
        self.logger.info(f"  - 분석 대상 고객: {analysis['total_customers']:,}명")
        self.logger.info(f"  - 전체 이탈률: {analysis['churn_rate']:.2%}")
        self.logger.info(f"  - 사용 모델: {analysis['best_model']}")
        
        self.logger.info("[UPDATE] 세그먼트 업데이트 결과:")
        self.logger.info(f"  - 제거된 기존 세그먼트: {updates['removed_count']:,}건")
        self.logger.info(f"  - 새로 할당된 세그먼트: {updates['assigned_count']:,}건")
        self.logger.info(f"  - VIP_ATTENTION: {updates['vip_attention_count']:,}명")
        self.logger.info(f"  - CHURN_RISK_HIGH: {updates['churn_risk_high_count']:,}명")
        
        # 변화량 로깅
        prev = result['previous_assignments']
        new = result['new_assignments']
        
        vip_change = new['VIP_ATTENTION'] - prev['VIP_ATTENTION']
        churn_change = new['CHURN_RISK_HIGH'] - prev['CHURN_RISK_HIGH']
        
        self.logger.info("[TREND] 전일 대비 변화:")
        self.logger.info(f"  - VIP_ATTENTION: {vip_change:+d}명 ({prev['VIP_ATTENTION']} → {new['VIP_ATTENTION']})")
        self.logger.info(f"  - CHURN_RISK_HIGH: {churn_change:+d}명 ({prev['CHURN_RISK_HIGH']} → {new['CHURN_RISK_HIGH']})")
    
    def _log_segment_statistics(self, stats):
        """세그먼트 통계 로깅"""
        self.logger.info("[STATS] 전체 세그먼트 현황:")
        
        # 이탈 위험 세그먼트만 먼저 로깅
        risk_segments = ['VIP_ATTENTION', 'CHURN_RISK_HIGH']
        for segment in stats['all_segments']:
            if segment['segment_tag'] in risk_segments:
                self.logger.info(f"  🚨 {segment['segment_title']} ({segment['segment_tag']}): {segment['customer_count']:,}명")
        
        # 나머지 세그먼트들
        self.logger.info("  기타 세그먼트:")
        for segment in stats['all_segments']:
            if segment['segment_tag'] not in risk_segments and segment['customer_count'] > 0:
                self.logger.info(f"    - {segment['segment_title']} ({segment['segment_tag']}): {segment['customer_count']:,}명")
        
        # 이탈 위험 세그먼트 상위 고객 로깅
        for segment_tag, customers in stats['risk_segment_details'].items():
            if customers:
                self.logger.info(f"[TOP] {segment_tag} 상위 고객 (매출 기준):")
                for i, customer in enumerate(customers[:3], 1):  # 상위 3명만
                    self.logger.info(
                        f"    {i}. {customer['customer_name']} "
                        f"(방문: {customer['visit_count']}회, 매출: {customer['total_revenue']:,.0f}원)"
                    )
    
    def run_once(self):
        """즉시 한 번 실행 (테스트용)"""
        self.logger.info("세그먼트 업데이트 즉시 실행")
        return self.run_daily_segment_update()
    
    def start_scheduler(self):
        """스케줄러 시작 - 매일 오전 4시에 실행"""
        self.logger.info("세그먼트 업데이트 스케줄러 시작")
        self.logger.info("스케줄: 매일 오전 4시")
        
        # 스케줄 등록
        schedule.every().day.at("04:00").do(self.run_daily_segment_update)
        
        # 다음 실행 시간 로깅
        next_run = schedule.next_run()
        if next_run:
            self.logger.info(f"다음 실행 예정: {next_run.strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 스케줄러 실행
        try:
            while True:
                schedule.run_pending()
                time.sleep(60)  # 1분마다 체크
        except KeyboardInterrupt:
            self.logger.info("스케줄러 종료됨")
        except Exception as e:
            self.logger.error(f"스케줄러 실행 중 오류: {str(e)}")


def main():
    """메인 실행 함수"""
    import argparse
    
    parser = argparse.ArgumentParser(description="세그먼트 자동 업데이트 스케줄러")
    parser.add_argument(
        '--mode', 
        choices=['schedule', 'once'], 
        default='schedule',
        help="실행 모드: schedule(스케줄러 시작), once(즉시 실행)"
    )
    
    args = parser.parse_args()
    
    scheduler = SegmentScheduler()
    
    if args.mode == 'once':
        # 즉시 실행
        success = scheduler.run_once()
        sys.exit(0 if success else 1)
    else:
        # 스케줄러 시작
        scheduler.start_scheduler()


if __name__ == "__main__":
    main()