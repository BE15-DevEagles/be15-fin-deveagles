#!/usr/bin/env python3
"""
Churn Prediction Smoke Test (CRM DB)
"""
import sys
from pathlib import Path

# src 경로 추가
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "src"))

def main():
    print("DevEagles Churn Prediction (CRM DB) Smoke Test")
    print("=" * 60)
    try:
        from analytics.services.churn_prediction import ChurnPredictionService

        service = ChurnPredictionService()

        print("\n1. 전체 이탈 분석(run_full_analysis) 실행...")
        result = service.run_full_analysis()
        print("\n✅ run_full_analysis 결과:")
        print(f"  전체 고객 수: {result['customers']}")
        print(f"  이탈률: {result['churn_rate']:.2%}")
        print(f"  최고 모델: {result['best_model']}\n")
        print(f"  위험 태그 통계: {result['risk_tag_stats']}")
        print(f"  상위 위험 고객 샘플: {result['high_risk_customers'][:2]}")

        print("\n2. predict_customer_churn() 실행...")
        result2 = service.predict_customer_churn()
        print(f"  전체 고객 수: {result2['customers']}")
        print(f"  이탈률: {result2['churn_rate']:.2%}")
        print(f"  최고 모델: {result2['best_model']}")

        print("\n3. predict() 샘플 실행...")
        # 원본 데이터프레임에서 샘플 추출 (인코딩 전)
        df = service._load_dataframe()
        df = service._create_features(df)
        preds = service.predict(df.head(5))
        print(f"  예측 결과(상위 5명): {preds.tolist()}")

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