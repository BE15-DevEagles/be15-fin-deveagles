#!/usr/bin/env python3
"""
DevEagles 비즈니스 인텔리전스 대시보드 실행 스크립트 (DuckDB 버전)

DevEagles Analytics 프로젝트의 종합 비즈니스 분석 대시보드를 실행합니다.
ETL 파이프라인을 통해 DuckDB에서 데이터를 불러와 분석합니다.
"""

import sys
import os
from pathlib import Path

# 프로젝트 경로 설정
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root / "src"))

def main():
    """DuckDB 대시보드 실행 메인 함수"""
    
    print("🚀 DevEagles 비즈니스 인텔리전스 대시보드 시작 (DuckDB 버전)")
    print("=" * 70)
    
    try:
        from analytics.dashboard.bi_dashboard_duckdb import BusinessIntelligenceDashboardDuckDB
        
        # DuckDB 대시보드 인스턴스 생성
        dashboard = BusinessIntelligenceDashboardDuckDB()
        
        print("✅ DuckDB 대시보드 초기화 완료")
        print("🌐 대시보드 서버 시작 중...")
        print("📱 브라우저에서 http://localhost:8050 으로 접속하세요")
        print("💾 데이터 소스: ETL Pipeline → DuckDB")
        print("⚠️  종료하려면 Ctrl+C를 누르세요")
        print("=" * 70)
        
        # DuckDB 대시보드 실행
        dashboard.run_server(debug=True, host='0.0.0.0', port=8050)
        
    except KeyboardInterrupt:
        print("\n👋 DuckDB 대시보드 서버 종료")
    except Exception as e:
        print(f"❌ DuckDB 대시보드 실행 중 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 