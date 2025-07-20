#!/usr/bin/env python3
"""
DevEagles Analytics 테스트 실행 스크립트

모든 테스트를 체계적으로 실행하고 결과를 출력합니다.
"""

from __future__ import annotations

import os
import sys
import subprocess
from pathlib import Path
import time
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Ensure pytest is available -------------------------------------------
def _ensure_pytest() -> None:
    """Ensure that `pytest` is installed in the current environment.

    If the module cannot be imported, it will attempt to install it
    via `pip` silently (only stderr/stdout on failure).
    """

    try:
        import importlib
        importlib.import_module("pytest")
        try:
            importlib.import_module("pytest_cov")
        except ModuleNotFoundError:
            print("⚙️  pytest-cov 플러그인이 없어 자동 설치합니다…")
            subprocess.check_call([sys.executable, "-m", "pip", "install", "pytest-cov>=4.1.0"])

        # pytest-asyncio for async test support
        try:
            importlib.import_module("pytest_asyncio")
        except ModuleNotFoundError:
            print("⚙️  pytest-asyncio 플러그인이 없어 자동 설치합니다…")
            subprocess.check_call([sys.executable, "-m", "pip", "install", "pytest-asyncio>=0.23.0"])
    except ModuleNotFoundError:
        print("⚙️  pytest 가 설치되지 않아 자동으로 설치합니다…")
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", "pytest>=7.4.3"])
        except subprocess.CalledProcessError as exc:
            print("❌ pytest 자동 설치 실패:", exc)
            sys.exit(1)

# Run once at import
_ensure_pytest()

def run_test(test_path: str, test_name: str, timeout: int = 30) -> bool:
    """테스트 실행 함수."""
    print(f"\n{'='*60}")
    print(f"🧪 {test_name} 테스트 실행")
    print(f"{'='*60}")

    try:
        project_root = Path(__file__).parent
        src_path = project_root / "src"
        env = {
            "PYTHONIOENCODING": "utf-8",
            "PYTHONUTF8": "1",
            "PYTHONPATH": f"{src_path}{os.pathsep}" + os.environ.get("PYTHONPATH", ""),
            **dict(os.environ),
        }

        result = subprocess.run(
            [sys.executable, "-m", "pytest", test_path, "-v"],
            capture_output=True,
            text=True,
            encoding='utf-8',
            env=env,
            timeout=timeout
        )

        if result.returncode == 0:
            print(f"✅ {test_name} 테스트 성공")
            return True
        else:
            print(f"❌ {test_name} 테스트 실패")
            print("🚨 에러:")
            print(result.stdout)
            print(result.stderr)
            return False

    except subprocess.TimeoutExpired:
        print(f"⏰ {test_name} 테스트 타임아웃")
        return False
    except Exception as e:
        print(f"❌ {test_name} 테스트 중 예외 발생: {str(e)}")
        return False

def main() -> bool:
    """메인 테스트 실행 함수."""
    test_dir = Path(__file__).parent / "tests"
    success = True

    # 코호트 리텐션 분석 테스트
    cohort_test = test_dir / "services" / "test_cohort_retention.py"
    if not run_test(str(cohort_test), "코호트 리텐션 분석"):
        success = False

    # 세그멘테이션 분석 테스트
    segmentation_test = test_dir / "services" / "test_segmentation.py"
    if not run_test(str(segmentation_test), "세그멘테이션 분석"):
        success = False

    # 리스크 태깅 테스트
    risk_test = test_dir / "services" / "test_risk_tagging.py"
    if not run_test(str(risk_test), "리스크 태깅"):
        success = False

    # 선호도 분석 테스트
    preference_test = test_dir / "services" / "test_preference.py"
    if not run_test(str(preference_test), "선호도 분석"):
        success = False

    # ETL 파이프라인 테스트
    etl_test = test_dir / "etl" / "test_etl.py"
    if not run_test(str(etl_test), "ETL 파이프라인", timeout=60):  # ETL 테스트는 시간이 더 필요할 수 있음
        success = False

    # 뷰티플라이 ETL 테스트
    beautifly_test = test_dir / "etl" / "test_beautifly_etl.py"
    if not run_test(str(beautifly_test), "뷰티플라이 ETL", timeout=60):  # ETL 테스트는 시간이 더 필요할 수 있음
        success = False

    return success

if __name__ == "__main__":
    import os
    os.environ["PYTHONIOENCODING"] = "utf-8"
    os.environ["PYTHONUTF8"] = "1"
    success = main()
    sys.exit(0 if success else 1) 