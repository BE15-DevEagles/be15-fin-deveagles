# DevEagles Analytics Service

**고객 이탈 방지를 위한 데이터 분석 서비스**

CRM 시스템에서 고객의 생애주기를 분석하고 이탈 위험을 예측하여 자동으로 태깅하는 FastAPI 기반의 분석 서비스 프로젝트 초안.

## 🎯 비즈니스 목표

- **신규 고객**: 첫 방문 후 7-30일 내 재방문 유도
- **성장 고객**: 방문 패턴 기반 이탈 위험 감지
- **충성 고객**: 예상 방문일 지연 모니터링
- **VIP 고객**: 특별 관리 대상 식별

## 🏗️ 프로젝트 구조

```
be15_DevEagles_DA/
├── src/analytics/                    # 메인 소스 코드
│   ├── __init__.py
│   ├── cli.py                       # CLI 인터페이스
│   ├── core/                        # 핵심 설정 및 공통 모듈
│   │   ├── __init__.py
│   │   ├── config.py               # 환경변수 및 설정 관리
│   │   ├── logging.py              # 구조화된 로깅
│   │   ├── database.py             # DB 연결 관리
│   │   ├── exceptions.py           # 커스텀 예외
│   │   └── middleware.py           # FastAPI 미들웨어
│   ├── models/                      # 도메인 모델 및 스키마
│   │   ├── __init__.py
│   │   ├── customer.py             # 고객 모델
│   │   ├── reservation.py          # 예약 모델
│   │   ├── analytics.py            # 분석 결과 모델
│   │   └── schemas.py              # Pydantic 스키마
│   ├── etl/                        # 데이터 추출, 변환, 적재
│   │   ├── __init__.py
│   │   ├── pipeline.py             # ETL 파이프라인 조정
│   │   ├── extractors.py           # CRM 데이터 추출
│   │   ├── transformers.py         # 데이터 변환 로직
│   │   ├── loaders.py              # DuckDB 적재 로직
│   │   └── validators.py           # 데이터 품질 검증
│   ├── ml/                         # 머신러닝 모듈
│   │   ├── __init__.py
│   │   ├── features.py             # 피처 엔지니어링
│   │   ├── models.py               # ML 모델 정의
│   │   ├── training.py             # 모델 훈련 로직
│   │   ├── inference.py            # 추론 및 예측
│   │   ├── model_manager.py        # 모델 버전 관리
│   │   └── evaluation.py           # 모델 평가 메트릭
│   ├── services/                   # 비즈니스 로직 서비스
│   │   ├── __init__.py
│   │   ├── customer_service.py     # 고객 분석 서비스
│   │   ├── segmentation_service.py # 고객 세분화 서비스
│   │   ├── tagging_service.py      # 자동 태깅 서비스
│   │   ├── prediction_service.py   # 예측 서비스
│   │   └── notification_service.py # 알림 서비스
│   ├── api/                        # REST API 엔드포인트
│   │   ├── __init__.py
│   │   ├── main.py                 # FastAPI 애플리케이션
│   │   ├── routes/                 # API 라우터
│   │   │   ├── __init__.py
│   │   │   ├── health.py           # 헬스체크
│   │   │   ├── customers.py        # 고객 분석 API
│   │   │   ├── analytics.py        # 분석 결과 API
│   │   │   ├── models.py           # 모델 관리 API
│   │   │   └── admin.py            # 관리자 API
│   │   ├── dependencies.py         # 의존성 주입
│   │   └── middleware.py           # API 미들웨어
│   ├── dashboard/                  # Dash 기반 BI 대시보드
│   │   ├── __init__.py
│   │   ├── bi_dashboard.py         # BusinessIntelligenceDashboard 구현
│   │   ├── components/             # 재사용 UI 컴포넌트
│   │   ├── utils/                  # 직렬화 등 보조 모듈
│   │   └── constants.py            # 색상, 테마 상수
│   └── utils/                      # 유틸리티 함수 (공통)
│       └── __init__.py
├── tests/                          # 테스트 코드
│   ├── __init__.py
│   ├── conftest.py                 # pytest 설정 및 픽스처
│   ├── unit/                       # 단위 테스트
│   │   ├── test_models/
│   │   ├── test_services/
│   │   ├── test_ml/
│   │   └── test_utils/
│   ├── integration/                # 통합 테스트
│   │   ├── test_api/
│   │   ├── test_etl/
│   │   └── test_database/
│   └── fixtures/                   # 테스트 데이터
│       ├── sample_data.json
│       └── models/
├── config/                         # 환경별 설정 파일
│   ├── development.yaml
│   ├── staging.yaml
│   ├── production.yaml
│   └── local.yaml.example
├── scripts/                        # 유틸리티 스크립트
│   ├── init-db.sql                # 개발 DB 초기화
│   ├── sample_data.sql            # 샘플 데이터
│   ├── deploy.sh                  # 배포 스크립트
│   └── backup.sh                  # 백업 스크립트
├── notebooks/                      # Jupyter 노트북
│   ├── exploratory/               # 탐색적 데이터 분석
│   ├── modeling/                  # 모델링 실험
│   └── reports/                   # 분석 리포트
├── monitoring/                     # 모니터링 설정
│   ├── prometheus.yml             # Prometheus 설정
│   └── grafana/                   # Grafana 대시보드
│       ├── dashboards/
│       └── datasources/
├── data/                          # 데이터 저장소 (gitignore)
│   ├── raw/                       # 원본 데이터
│   ├── processed/                 # 처리된 데이터
│   └── analytics.db               # DuckDB 파일
├── models/                        # 훈련된 모델 저장소 (gitignore)
├── logs/                          # 로그 파일 (gitignore)
├── .env.example                   # 환경변수 예시
├── .gitignore                     # Git 무시 파일
├── pyproject.toml                 # Python 프로젝트 설정
├── Dockerfile                     # Docker 이미지 빌드
├── docker-compose.yml             # 로컬 개발 환경
├── Makefile                       # 개발 작업 자동화
└── README.md                      # 프로젝트 문서
```

## 🚀 빠른 시작

### 필요 조건

- Python 3.11+
- Docker & Docker Compose
- MariaDB (CRM 데이터베이스)

### 로컬 개발 환경 설정

```bash
# 1. 저장소 클론
git clone <repository-url>
cd be15_DevEagles_DA

# 2. 개발 환경 전체 설정
make dev-setup

# 3. 환경변수 설정
cp .env.example .env
# .env 파일을 편집하여 데이터베이스 연결 정보 등을 설정

# 4. 개발 서버 시작
make run-dev
```

### Docker를 사용한 개발 환경

```bash
# 전체 개발 환경 시작 (API, DB, 모니터링)
make docker-dev

# 특정 서비스만 시작
make jupyter        # Jupyter Lab
make monitoring     # Prometheus + Grafana
```

## 📋 주요 기능

### 1. 고객 세분화

- **신규 고객**: 방문 횟수 ≤ 3회
- **성장 고객**: 방문 횟수 4-10회
- **충성 고객**: 방문 횟수 > 10회, 총 결제액 < 100만원
- **VIP 고객**: 총 결제액 ≥ 100만원

### 2. 이탈 위험 태깅

- `new_customer_followup`: 신규 고객 7일 후 팔로업 필요
- `new_customer_at_risk`: 신규 고객 20일 이상 미방문
- `reactivation_needed`: 30일 이상 미방문 고객
- `growing_delayed`: 성장 고객 예상 방문일 지연
- `loyal_delayed`: 충성 고객 예상 방문일 지연

### 3. 자동화된 ETL 파이프라인

- 증분 데이터 처리
- DuckDB를 활용한 고성능 분석
- 원자적 테이블 교체로 무중단 업데이트

### 4. ML 기반 예측

- 고객 이탈 확률 예측
- 다음 방문일 예측
- 서비스 추천 (향후 확장)

## 🛠️ 개발 도구

### CLI 명령어

```bash
# 서버 실행
analytics-server --reload --log-level debug

# ETL 파이프라인 실행
analytics-etl --full              # 전체 ETL
analytics-etl                     # 증분 ETL

# 모델 훈련
analytics-train --model-type all  # 모든 모델
analytics-train --model-type churn # 이탈 예측 모델만

# 고객 태깅
analytics-tag                     # 모든 고객
analytics-tag --customer-id 123   # 특정 고객

# 상태 확인
analytics status
analytics config
```

### Makefile 명령어

```bash
# 개발
make run-dev                      # 개발 서버 시작
make test                         # 테스트 실행
make lint                         # 코드 품질 검사
make format                       # 코드 포맷팅

# 데이터 작업
make etl-full                     # 전체 ETL
make train-all                    # 모든 모델 훈련
make tag-customers                # 고객 태깅

# Docker
make docker-dev                   # 개발 환경 시작
make docker-prod                  # 프로덕션 환경 시작
make logs                         # 로그 확인
```

## 🧪 테스트

```bash
# 모든 테스트 실행
make test

# 단위 테스트만
make test-unit

# 통합 테스트만
make test-integration

# 커버리지 리포트
make test-coverage
```

## 📊 모니터링

### 접속 정보

- **API Documentation**: http://localhost:8000/docs
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jupyter Lab**: http://localhost:8888 (token: deveagles)

### 주요 메트릭

- ETL 처리 시간 및 성공률
- 모델 예측 성능
- API 응답 시간 및 에러율
- 데이터 품질 지표

## 🗄️ 데이터베이스 구조

### CRM Database (MariaDB - 읽기 전용)

- `customers`: 고객 정보
- `reservations`: 예약/방문 기록
- `services`: 서비스 정보

### Analytics Database (DuckDB)

- 고성능 분석을 위한 최적화된 스키마
- 고객 특성 및 패턴 데이터
- ML 모델 결과 저장

## 🔧 설정

### 환경변수 예시

```bash
# Application
ANALYTICS_DEBUG=false
ANALYTICS_LOG_LEVEL=INFO

# Database
ANALYTICS_CRM_DATABASE_URL=mysql+pymysql://user:pass@host:3306/crm
ANALYTICS_ANALYTICS_DB_PATH=data/analytics.db

# ML
ANALYTICS_MODEL_STORAGE_PATH=models
ANALYTICS_FEATURE_COLUMNS=age,frequency,avg_monetary,lifecycle_days

# Scheduling
ANALYTICS_ETL_SCHEDULE_HOUR=2
ANALYTICS_TAGGING_SCHEDULE_HOUR=3
```

## 🏗️ 아키텍처 원칙

### Clean Architecture

- **Domain Layer**: 비즈니스 로직 중심
- **Service Layer**: 유스케이스 구현
- **Infrastructure Layer**: 외부 시스템 연동

### SOLID 원칙

- **Single Responsibility**: 각 모듈은 하나의 책임
- **Open/Closed**: 확장에는 열려있고 변경에는 닫혀있음
- **Liskov Substitution**: 인터페이스 기반 설계
- **Interface Segregation**: 작고 구체적인 인터페이스
- **Dependency Inversion**: 의존성 주입 활용

### 설계 패턴

- **Repository Pattern**: 데이터 접근 추상화
- **Strategy Pattern**: 알고리즘 교체 가능
- **Observer Pattern**: 이벤트 기반 처리
- **Factory Pattern**: 객체 생성 관리

## 📈 확장성 고려사항

### 성능 최적화

- DuckDB 활용한 컬럼형 분석
- 배치 처리 최적화
- 연결 풀링 및 캐싱

### 확장 가능한 구조

- 마이크로서비스 아키텍처 대응
- API 버저닝 지원
- 플러그인 아키텍처

### 운영 안정성

- 헬스체크 및 모니터링
- 그레이스풀 셧다운
- 에러 처리 및 복구

## 🚀 배포

### 스테이징 환경

```bash
make deploy-staging
```

### 프로덕션 환경

```bash
make deploy-prod
```

### 코드 품질 기준

- 테스트 커버리지 > 80%
- 타입 힌트 필수
- Black 포맷팅 적용
- Docstring 작성

## 📝 라이선스

MIT License

---

**버전**: 0.1.0  
**마지막 업데이트**: 2024년 12월
