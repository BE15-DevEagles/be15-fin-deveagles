#!/usr/bin/env python3
"""
Segmentation Smoke Test (CRM DB)
"""
import sys
from pathlib import Path

# src 경로 추가
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "src"))

def main():
    print("DevEagles Segmentation (CRM DB) Smoke Test")
    print("=" * 60)
    try:
        from analytics.services.segmentation import CustomerSegmentationService

        service = CustomerSegmentationService()

        print("\n1. 전체 세그멘테이션(segment_all_customers) 실행...")
        seg_counts = service.segment_all_customers()
        print(f"  세그먼트별 고객 수: {seg_counts}")

        print("\n2. get_segment_distribution() 실행...")
        dist = service.get_segment_distribution()
        print(f"  분포 통계: {dist}")

        print("\n3. get_segment_insights('vip') 실행...")
        insights = service.get_segment_insights('vip')
        print(f"  VIP 인사이트: {insights}")

        print("\n🎉 모든 smoke test 통과!")
        return True
    except Exception as e:
        print(f"❌ 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 