#!/usr/bin/env python3
"""
Segmentation DuckDB Smoke Test
"""
import sys
from pathlib import Path

# src 경로 추가
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "src"))

def main():
    print("DevEagles Segmentation DuckDB Smoke Test")
    print("=" * 60)
    try:
        from analytics.services.segmentation_duckdb import CustomerSegmentationServiceDuckDB

        service = CustomerSegmentationServiceDuckDB()

        print("\n1. 전체 세그멘테이션(run_full_analysis) 실행...")
        result = service.run_full_analysis()
        print("\n✅ run_full_analysis 결과:")
        print(f"  세그먼트별 집계: {result['segment_counts']}")
        print(f"  분포 통계: {result['distribution']}")
        print(f"  트렌드: {result['trends']}")
        print(f"  인사이트: {result['insights']}")

        print("\n2. segment_all_customers() 실행...")
        seg_counts = service.segment_all_customers()
        print(f"  세그먼트별 고객 수: {seg_counts}")

        print("\n3. get_customer_segments() 실행...")
        segs = service.get_customer_segments()
        print(f"  전체 고객 수: {segs['total_customers']}")
        print(f"  샘플: {segs['segments'][:2]}")

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