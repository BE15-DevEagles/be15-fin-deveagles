#!/usr/bin/env python3
"""
코호트 리텐션 분석 테스트 스크립트

DevEagles Analytics 프로젝트의 코호트 리텐션 분석 기능을 테스트합니다.
"""

import sys
import os
from pathlib import Path

# 프로젝트 경로 설정 (tests/services에서 프로젝트 루트로 이동)
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root / "src"))

from analytics.core.config import settings
from analytics.core.database import get_crm_db, get_analytics_db
from analytics.services.cohort_retention import CohortRetentionAnalyzer

def main():
    """코호트 리텐션 분석 테스트 메인 함수"""
    
    print("🚀 DevEagles 코호트 리텐션 분석 테스트")
    print("=" * 60)
    
    try:
        # 데이터베이스 엔진 생성
        print("📊 데이터베이스 연결 중...")
        crm_engine = get_crm_db()
        analytics_engine = get_analytics_db()
        
        print("✅ 데이터베이스 연결 성공")
        
        # 코호트 리텐션 분석기 생성
        print("\n🎯 코호트 리텐션 분석기 초기화 중...")
        analyzer = CohortRetentionAnalyzer(crm_engine, analytics_engine)
        
        # 전체 분석 실행
        print("\n🔍 전체 코호트 리텐션 분석 실행 중...")
        results = analyzer.run_full_analysis()
        
        if results:
            print("\n✅ 코호트 리텐션 분석 완료!")
            print(f"📈 분석 결과:")
            print(f"   - 총 고객 수: {results['total_customers']:,}명")
            print(f"   - 총 예약 수: {results['total_reservations']:,}건")
            print(f"   - 분석 매장 수: {len(results['shop_analysis']) if not results['shop_analysis'].empty else 0}개")
            print(f"   - 성별 그룹 수: {len(results['gender_analysis'])}개")
            print(f"   - 연령대 그룹 수: {len(results['age_analysis'])}개")
            
            # 상위 매장 표시
            if not results['shop_analysis'].empty:
                print(f"\n🏆 리텐션율 상위 3개 매장:")
                top_shops = results['shop_analysis'].head(3)
                for i, (_, shop) in enumerate(top_shops.iterrows(), 1):
                    print(f"   {i}. {shop['shop_name']}: {shop['month1_retention']:.1%} "
                          f"({shop['total_customers']:,}명)")
            
            # 성별 분석 결과
            if results['gender_analysis']:
                print(f"\n👥 성별 분석 결과:")
                for gender, data in results['gender_analysis'].items():
                    gender_name = '남성' if gender == 'M' else '여성'
                    print(f"   {gender_name}: {data['total_customers']:,}명, "
                          f"1개월 리텐션 {data['month1_retention']:.1%}")
            
            # 연령대별 분석 결과
            if results['age_analysis']:
                print(f"\n🎂 연령대별 분석 결과:")
                for age_group, data in results['age_analysis'].items():
                    print(f"   {age_group}세: {data['total_customers']:,}명, "
                          f"1개월 리텐션 {data['month1_retention']:.1%}")
            
            # 생성된 파일 확인
            print(f"\n📁 생성된 파일 확인:")
            generated_files = [
                "overall_cohort_heatmap.png",
                "shop_cohort_heatmaps.png", 
                "retention_curves.png"
            ]
            
            for file_name in generated_files:
                if os.path.exists(file_name):
                    print(f"   ✅ {file_name}")
                else:
                    print(f"   ❌ {file_name} (생성되지 않음)")
            
            print(f"\n🎉 코호트 리텐션 분석 테스트 완료!")
            return True
            
        else:
            print("❌ 코호트 리텐션 분석 실패")
            return False
            
    except Exception as e:
        print(f"❌ 테스트 중 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    finally:
        # 리소스 정리
        if 'crm_engine' in locals():
            crm_engine.dispose()
        if 'analytics_engine' in locals():
            analytics_engine.close()

def test_dashboard():
    """대시보드 테스트 함수"""
    
    print("\n🌐 대시보드 테스트 시작...")
    
    try:
        # 대시보드 모듈 임포트
        from analytics.dashboard import BusinessIntelligenceDashboard
        
        print("✅ 대시보드 모듈 임포트 성공")
        
        # 대시보드 인스턴스 생성
        dashboard = BusinessIntelligenceDashboard()
        
        print("✅ 대시보드 인스턴스 생성 성공")
        print("\n📋 대시보드 실행 방법:")
        print("   python -m analytics.dashboard")
        print("   또는")
        print("   python src/analytics/dashboard/bi_dashboard.py")
        
        return True
        
    except Exception as e:
        print(f"❌ 대시보드 테스트 중 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("DevEagles 코호트 리텐션 분석 테스트 스크립트")
    print("=" * 60)
    
    # 코호트 리텐션 분석 테스트
    analysis_success = main()
    
    # 대시보드 테스트
    dashboard_success = test_dashboard()
    
    print("\n" + "=" * 60)
    print("📊 테스트 결과 요약:")
    print(f"   코호트 리텐션 분석: {'✅ 성공' if analysis_success else '❌ 실패'}")
    print(f"   대시보드 테스트: {'✅ 성공' if dashboard_success else '❌ 실패'}")
    
    if analysis_success and dashboard_success:
        print("\n🎉 모든 테스트 통과!")
        print("\n🚀 대시보드 실행 방법:")
        print("   cd be15_DevEagles_DA")
        print("   python test_cohort_retention.py")
        print("   또는 대시보드만 실행:")
        print("   python src/analytics/dashboard/bi_dashboard.py")
    else:
        print("\n❌ 일부 테스트 실패")
        
    print("=" * 60) 