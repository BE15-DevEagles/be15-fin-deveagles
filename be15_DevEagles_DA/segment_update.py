#!/usr/bin/env python3
"""세그먼트 업데이트 실행 스크립트

이 스크립트는 시스템의 cron이나 작업 스케줄러를 통해 실행될 수 있습니다.
프로젝트 루트에서 실행하여 이탈 위험 세그먼트를 자동으로 업데이트합니다.

사용법:
    # 즉시 한 번 실행
    python segment_update.py --mode once
    
    # 스케줄러 시작 (매일 오전 4시)
    python segment_update.py --mode schedule
    
    # 기본값은 즉시 실행
    python segment_update.py

cron 설정 예시:
    # 매일 오전 4시에 실행
    0 4 * * * cd /path/to/deveagles-fin-repo/be15_DevEagles_DA && python segment_update.py --mode once >> logs/segment_update.log 2>&1
"""

import sys
import os

# 프로젝트 루트 디렉토리를 Python 경로에 추가
current_dir = os.path.dirname(os.path.abspath(__file__))
src_dir = os.path.join(current_dir, 'src')
sys.path.insert(0, src_dir)

try:
    from analytics.services.segment_scheduler import SegmentScheduler
except ImportError as e:
    print(f"모듈 임포트 실패: {e}")
    print(f"현재 작업 디렉토리: {os.getcwd()}")
    print(f"Python 경로: {sys.path[:3]}")
    sys.exit(1)


def main():
    """메인 실행 함수"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="세그먼트 자동 업데이트 스크립트",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
사용 예시:
  python segment_update.py --mode once     # 즉시 한 번 실행
  python segment_update.py --mode schedule # 스케줄러 시작
  
cron 설정 예시:
  0 4 * * * cd /path/to/deveagles-fin-repo/be15_DevEagles_DA && python segment_update.py --mode once
        """
    )
    
    parser.add_argument(
        '--mode', 
        choices=['schedule', 'once'], 
        default='once',
        help="실행 모드 (기본값: once)"
    )
    
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help="상세 로그 출력"
    )
    
    args = parser.parse_args()
    
    try:
        scheduler = SegmentScheduler()
        
        if args.mode == 'once':
            print("세그먼트 업데이트를 즉시 실행합니다...")
            success = scheduler.run_once()
            if success:
                print("✅ 세그먼트 업데이트가 성공적으로 완료되었습니다.")
                sys.exit(0)
            else:
                print("❌ 세그먼트 업데이트 중 오류가 발생했습니다.")
                sys.exit(1)
        else:
            print("세그먼트 업데이트 스케줄러를 시작합니다...")
            print("매일 오전 4시에 자동으로 실행됩니다.")
            print("종료하려면 Ctrl+C를 누르세요.")
            scheduler.start_scheduler()
            
    except KeyboardInterrupt:
        print("\n스케줄러가 사용자에 의해 종료되었습니다.")
        sys.exit(0)
    except Exception as e:
        print(f"❌ 실행 중 오류 발생: {str(e)}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()