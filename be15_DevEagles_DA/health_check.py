#!/usr/bin/env python3
"""
AWS 환경용 헬스체크 스크립트
RDS 연결, 설정 로딩, 서비스 상태를 점검합니다.
"""

import sys
import os
import json
from pathlib import Path
from datetime import datetime

# 프로젝트 경로 설정
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root / "src"))

def health_check():
    """종합 헬스체크 수행"""
    print("🏥 DevEagles Analytics - AWS 헬스체크")
    print(f"🕐 시간: {datetime.now().isoformat()}")
    print("=" * 50)
    
    health_status = {
        "timestamp": datetime.now().isoformat(),
        "status": "healthy",
        "checks": {}
    }
    
    # 1. 환경변수 체크
    print("\n📋 1. 환경변수 체크")
    required_env_vars = [
        "CRM_DATABASE_URL",
        "ANALYTICS_DB_PATH", 
        "LOG_LEVEL"
    ]
    
    aws_env_vars = [
        "AWS_DEFAULT_REGION",
        "CRM_DB_HOST",
        "CRM_DB_USER",
        "CRM_DB_PASSWORD"
    ]
    
    env_check = {"status": "pass", "missing": [], "aws_detected": False}
    
    for var in required_env_vars:
        if not os.getenv(var):
            env_check["missing"].append(var)
            print(f"  ❌ {var}: Missing")
        else:
            print(f"  ✅ {var}: Set")
    
    # AWS 환경 감지
    aws_vars_present = sum(1 for var in aws_env_vars if os.getenv(var))
    if aws_vars_present >= 2:
        env_check["aws_detected"] = True
        print(f"  🌐 AWS 환경 감지됨 ({aws_vars_present}/{len(aws_env_vars)} vars)")
    
    if env_check["missing"]:
        env_check["status"] = "warning"
    
    health_status["checks"]["environment"] = env_check
    
    # 2. 설정 로딩 체크
    print("\n⚙️  2. 설정 로딩 체크")
    config_check = {"status": "fail", "error": None}
    
    try:
        from analytics.core.config import settings
        print(f"  ✅ 설정 로딩 성공")
        print(f"  📱 App: {settings.app_name}")
        print(f"  🐞 Debug: {settings.debug}")
        print(f"  📊 Log Level: {settings.log_level}")
        config_check["status"] = "pass"
    except Exception as e:
        print(f"  ❌ 설정 로딩 실패: {e}")
        config_check["error"] = str(e)
        health_status["status"] = "unhealthy"
    
    health_status["checks"]["configuration"] = config_check
    
    # 3. 데이터베이스 연결 체크
    print("\n🗄️  3. 데이터베이스 연결 체크")
    db_checks = {}
    
    # Analytics DB (DuckDB)
    analytics_check = {"status": "fail", "error": None, "path": None}
    try:
        from analytics.core.database import get_analytics_db
        db = get_analytics_db()
        result = db.execute("SELECT 1 as test, current_timestamp as ts").fetchone()
        print(f"  ✅ Analytics DB: 연결 성공 ({result})")
        analytics_check["status"] = "pass"
        analytics_check["path"] = str(settings.analytics_db_path)
    except Exception as e:
        print(f"  ❌ Analytics DB: 연결 실패 ({e})")
        analytics_check["error"] = str(e)
        health_status["status"] = "unhealthy"
    
    db_checks["analytics"] = analytics_check
    
    # CRM DB (MariaDB/RDS)
    crm_check = {"status": "fail", "error": None, "host": None, "version": None}
    try:
        from analytics.core.database import get_crm_db
        engine = get_crm_db()
        with engine.connect() as conn:
            result = conn.execute("SELECT 1 as test, @@version as version, @@hostname as host").fetchone()
            print(f"  ✅ CRM DB: 연결 성공")
            print(f"    - 버전: {result[1]}")
            print(f"    - 호스트: {result[2]}")
            crm_check["status"] = "pass"
            crm_check["host"] = str(result[2])
            crm_check["version"] = str(result[1])
            
            # RDS 확인
            if "rds.amazonaws.com" in settings.crm_database_url:
                print(f"    - 🌐 AWS RDS 감지됨")
                crm_check["aws_rds"] = True
            
    except Exception as e:
        print(f"  ❌ CRM DB: 연결 실패 ({e})")
        crm_check["error"] = str(e)
        if "rds.amazonaws.com" in str(e) or "timeout" in str(e).lower():
            print(f"    💡 RDS 연결 문제일 수 있습니다. 보안그룹/VPC 설정을 확인하세요.")
        health_status["status"] = "unhealthy"
    
    db_checks["crm"] = crm_check
    health_status["checks"]["databases"] = db_checks
    
    # 4. 파일 시스템 체크
    print("\n📁 4. 파일 시스템 체크")
    fs_check = {"status": "pass", "files": {}}
    
    important_files = {
        "config.yaml": project_root / "config.yaml",
        ".env": project_root / ".env",
        "data_dir": Path(settings.analytics_db_path).parent,
        "logs_dir": project_root / "logs"
    }
    
    for name, path in important_files.items():
        exists = path.exists()
        readable = exists and os.access(path, os.R_OK)
        writable = exists and os.access(path, os.W_OK) if path.is_dir() or not exists else os.access(path.parent, os.W_OK)
        
        status = "✅" if exists and readable else "❌"
        print(f"  {status} {name}: {path}")
        if not exists and name in ["data_dir", "logs_dir"]:
            try:
                path.mkdir(parents=True, exist_ok=True)
                print(f"    📂 디렉토리 생성됨: {path}")
            except Exception as e:
                print(f"    ❌ 디렉토리 생성 실패: {e}")
                fs_check["status"] = "warning"
        
        fs_check["files"][name] = {
            "path": str(path),
            "exists": exists,
            "readable": readable,
            "writable": writable
        }
    
    health_status["checks"]["filesystem"] = fs_check
    
    # 5. 최종 상태 출력
    print(f"\n🏁 최종 상태: {health_status['status'].upper()}")
    
    if health_status["status"] == "healthy":
        print("  🎉 모든 검사 통과! 서비스 준비 완료")
        exit_code = 0
    elif health_status["status"] == "warning":
        print("  ⚠️  경고사항이 있지만 서비스 실행 가능")
        exit_code = 0
    else:
        print("  💥 심각한 문제 발견! 서비스 실행 불가")
        exit_code = 1
    
    # JSON 출력 (모니터링 도구용)
    if "--json" in sys.argv:
        print("\n" + "="*50)
        print(json.dumps(health_status, indent=2, ensure_ascii=False))
    
    return exit_code

if __name__ == "__main__":
    try:
        exit_code = health_check()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n👋 헬스체크 중단됨")
        sys.exit(130)
    except Exception as e:
        print(f"\n💥 헬스체크 실행 중 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)