# Multi-stage build for production optimization
FROM apache/airflow:2.7.0-python3.9 AS base

USER root

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    default-libmysqlclient-dev \
    pkg-config \
    curl \
    wget \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download AWS RDS SSL certificates for production
RUN mkdir -p /opt/ssl && \
    wget -O /opt/ssl/rds-ca-2019-root.pem https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem && \
    chmod 644 /opt/ssl/rds-ca-2019-root.pem

USER airflow

# Copy requirements first for better Docker layer caching
COPY requirements.txt /tmp/requirements.txt
COPY etl_requirements.txt /tmp/etl_requirements.txt

# Install Python dependencies
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r /tmp/requirements.txt

# Development stage
FROM base AS development
COPY . /opt/airflow/src/
WORKDIR /opt/airflow

# Production stage
FROM base AS production
COPY src/ /opt/airflow/src/
COPY config/ /opt/airflow/config/
WORKDIR /opt/airflow

# 환경 변수 설정
ENV PYTHONPATH=/opt/airflow/src:$PYTHONPATH
ENV PYTHONIOENCODING=utf-8
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080 8000 