#!/bin/bash
# DevEagles Analytics EC2 배포 스크립트

set -e

echo "🚀 DevEagles Analytics EC2 배포 시작..."

# 1. Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "📦 Docker 설치 중..."
    sudo yum update -y
    sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -a -G docker $USER
    echo "Docker 설치 완료"
fi

# 2. Docker Compose 설치 확인
if ! command -v docker-compose &> /dev/null; then
    echo "📦 Docker Compose 설치 중..."
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose 설치 완료"
fi

# 3. AWS CLI 설치 확인
if ! command -v aws &> /dev/null; then
    echo "📦 AWS CLI 설치 중..."
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
    echo "AWS CLI 설치 완료"
fi

# 4. 작업 디렉터리 생성
mkdir -p ~/deveagles-analytics
cd ~/deveagles-analytics

# 5. ECR 로그인
echo "🔐 ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com

# 6. 이미지 풀
echo "📥 Docker 이미지 다운로드 중..."
docker pull 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com/deveagles-analytics:latest

# 7. Docker Compose 파일 생성
cat > docker-compose.prod.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_USER: airflow
      POSTGRES_PASSWORD: deveagles_postgres_2025
      POSTGRES_DB: airflow
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U airflow"]
      interval: 10s
      timeout: 5s
      retries: 5

  analytics:
    image: 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com/deveagles-analytics:latest
    ports:
      - "8080:8080"
      - "8050:8050"
    environment:
      - AIRFLOW__CORE__EXECUTOR=LocalExecutor
      - AIRFLOW__DATABASE__SQL_ALCHEMY_CONN=postgresql+psycopg2://airflow:deveagles_postgres_2025@postgres:5432/airflow
      - CRM_DATABASE_URL=mysql+pymysql://admin:eagles1234!@project-db.crmia2oe03uh.ap-northeast-2.rds.amazonaws.com:3306/beautifly?charset=utf8mb4&connect_timeout=60&read_timeout=60&write_timeout=60
      - LOG_LEVEL=INFO
      - DEBUG=false
      - ANALYTICS_DB_PATH=data/analytics.duckdb
    volumes:
      - analytics_data:/opt/airflow/data
      - analytics_logs:/opt/airflow/logs
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres_data:
  analytics_data:
  analytics_logs:
EOF

# 8. 서비스 시작
echo "🚀 서비스 시작 중..."
docker-compose -f docker-compose.prod.yml up -d

# 9. 서비스 상태 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "🔍 서비스 상태 확인..."
docker-compose -f docker-compose.prod.yml ps

# 10. 헬스체크
echo "🏥 헬스체크 중..."
for i in {1..10}; do
    if curl -f http://localhost:8080/health; then
        echo "✅ Airflow 서비스 정상 동작"
        break
    else
        echo "⏳ Airflow 시작 대기 중... ($i/10)"
        sleep 10
    fi
done

if curl -f http://localhost:8050; then
    echo "✅ Dashboard 서비스 정상 동작"
else
    echo "⚠️ Dashboard 서비스 확인 필요"
fi

echo ""
echo "🎉 배포 완료!"
echo "📊 Airflow: http://54.181.1.209:8080 (admin/deveagles2025!)"
echo "📈 Dashboard: http://54.181.1.209:8050"
echo ""
echo "로그 확인: docker-compose -f docker-compose.prod.yml logs -f"
echo "서비스 중지: docker-compose -f docker-compose.prod.yml down"