# DevEagles Analytics - AWS 배포 가이드

## 시스템 구성
- **Airflow**: ETL 파이프라인 실행 및 스케줄링
- **Analytics Dashboard**: 비즈니스 인텔리전스 대시보드 (DuckDB 기반)
- **Segment Scheduler**: 자동 세그먼트 업데이트

## 배포 준비

### 1. 환경 설정
```bash
# 개발 환경 설정
make setup-env-dev

# 프로덕션 환경 설정 
make setup-env-prod

# .env 파일 편집
cp .env.example .env
# 프로덕션 값으로 수정
```

### 2. AWS 사전 준비
```bash
# AWS CLI 설정 확인
aws configure list

# Docker 확인
docker --version
docker-compose --version
```

## 로컬 개발 환경

### 전체 통합 환경 실행
```bash
# 개발 환경 (CRM DB 포함)
make docker-dev

# 서비스별 접속
# - Airflow: http://localhost:8080 (airflow/airflow)
# - Dashboard: http://localhost:8050
# - PostgreSQL: localhost:5432
# - CRM DB: localhost:3306
```

### 서비스별 실행
```bash
# Airflow만 실행
make docker-airflow-only

# 대시보드만 실행  
make docker-dashboard-only

# 모니터링 포함
make docker-monitoring
```

### 로그 확인
```bash
# 전체 로그
make logs

# 서비스별 로그
make logs-airflow
make logs-dashboard
make logs-scheduler
```

## AWS 프로덕션 배포

### 1. 이미지 빌드
```bash
# 프로덕션 이미지 빌드
make aws-build

# 또는 직접 빌드
DOCKER_TARGET=production docker-compose build
```

### 2. ECR 설정 (선택사항)
```bash
# ECR 리포지토리 생성
aws ecr create-repository --repository-name deveagles-analytics

# 로그인 및 푸시
make aws-push
```

### 3. RDS 설정 (권장)
프로덕션에서는 관리형 PostgreSQL (RDS) 사용을 권장합니다:

```bash
# RDS 인스턴스 생성 후 .env에 설정
AIRFLOW__DATABASE__SQL_ALCHEMY_CONN=postgresql+psycopg2://user:password@your-rds-endpoint:5432/airflow
```

### 4. 프로덕션 배포
```bash
# 프로덕션 배포
make deploy-prod

# 또는 직접 실행
DOCKER_TARGET=production docker-compose up -d
```

## 환경 변수 설정

### 필수 환경 변수 (.env)
```bash
# Airflow 보안
AIRFLOW_SECRET_KEY=your-secret-key-here
AIRFLOW_FERNET_KEY=your-fernet-key-here

# 데이터베이스 연결
CRM_DB_HOST=your-crm-db-host
CRM_DB_USER=your-crm-user
CRM_DB_PASSWORD=your-crm-password
CRM_DB_NAME=your-crm-database

# PostgreSQL (Airflow 메타데이터)
POSTGRES_PASSWORD=secure-password

# 서비스 포트
AIRFLOW_WEBSERVER_PORT=8080
DASHBOARD_PORT=8050
```

### AWS 환경 변수
```bash
# AWS 자격 증명 (EC2 Role 사용 권장)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_DEFAULT_REGION=ap-northeast-2
```

## 서비스 헬스체크

### Airflow
- Web UI: `http://your-host:8080/health`
- API: `curl http://your-host:8080/api/v1/health`

### Analytics Dashboard
- Health: `curl http://your-host:8050/_dash-dependencies`
- Main: `http://your-host:8050`

### 데이터베이스 연결 확인
```bash
# 컨테이너 내에서 연결 테스트
docker-compose -f docker-compose-unified.yml exec airflow-webserver python -c "
from analytics.core.database import get_crm_db
engine = get_crm_db()
print('CRM DB 연결 성공')
"
```

## 모니터링 (선택사항)

### Prometheus + Grafana 실행
```bash
make docker-monitoring

# 접속 정보
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000 (admin/admin)
```

## 트러블슈팅

### 일반적인 문제
1. **포트 충돌**: .env에서 포트 변경
2. **메모리 부족**: docker-compose.prod.yml에서 리소스 제한 조정
3. **CRM DB 연결 실패**: 네트워크 설정 및 방화벽 확인

### 로그 확인
```bash
# 전체 로그
docker-compose -f docker-compose-unified.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose-unified.yml logs -f airflow-scheduler

# 컨테이너 내부 접속
docker-compose -f docker-compose-unified.yml exec airflow-webserver bash
```

### 데이터 초기화
```bash
# 볼륨 포함 완전 삭제
make stop-all

# 처음부터 다시 시작
make docker-unified-dev
```

## 보안 고려사항

1. **Fernet Key 생성**:
   ```python
   from cryptography.fernet import Fernet
   print(Fernet.generate_key().decode())
   ```

2. **Secret Key 생성**:
   ```python
   import secrets
   print(secrets.token_urlsafe(32))
   ```

3. **네트워크 보안**: VPC, 보안 그룹 설정

4. **데이터베이스 보안**: RDS 암호화, 접근 제한

## 성능 최적화

1. **리소스 할당**: docker-compose.prod.yml에서 CPU/메모리 조정
2. **복제본 수**: 트래픽에 따라 replicas 조정
3. **데이터베이스**: RDS 인스턴스 타입 및 스토리지 최적화
4. **캐싱**: Redis 추가 고려

## 백업 및 복구

1. **DuckDB 백업**: `/opt/airflow/data` 볼륨 백업
2. **PostgreSQL 백업**: RDS 자동 백업 설정
3. **설정 백업**: `.env` 파일 및 docker-compose 파일 버전 관리