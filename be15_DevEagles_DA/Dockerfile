# Multi-stage build for production optimization
FROM apache/airflow:2.7.0-python3.9 AS base

USER root

# Install system dependencies including supervisord
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    default-libmysqlclient-dev \
    pkg-config \
    curl \
    wget \
    ca-certificates \
    supervisor \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Download AWS RDS SSL certificates for production
RUN mkdir -p /opt/ssl && \
    wget -O /opt/ssl/rds-ca-2019-root.pem https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem && \
    chmod 644 /opt/ssl/rds-ca-2019-root.pem

USER airflow

# Copy requirements first for better Docker layer caching
COPY requirements.txt /tmp/requirements.txt

# Install Python dependencies (excluding apache-airflow to avoid conflicts)
RUN pip install --no-cache-dir --upgrade pip && \
    grep -v "^apache-airflow" /tmp/requirements.txt > /tmp/requirements_filtered.txt && \
    pip install --no-cache-dir -r /tmp/requirements_filtered.txt

# Production stage
FROM base AS production

USER root

# Copy application code and necessary scripts/configs
COPY --chown=airflow:root src/ /opt/airflow/src/
COPY --chown=airflow:root config.yaml /opt/airflow/config.yaml
COPY --chown=airflow:root supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY --chown=airflow:root scripts/ /opt/airflow/scripts/
COPY --chown=airflow:root run_dashboard.py /opt/airflow/
COPY --chown=airflow:root segment_update.py /opt/airflow/

# Make scripts executable and create log directories
RUN chmod +x /opt/airflow/scripts/*.sh && \
    mkdir -p /opt/airflow/logs

USER airflow

WORKDIR /opt/airflow

# Set environment variables
ENV PYTHONPATH=/opt/airflow/src:$PYTHONPATH
ENV PYTHONIOENCODING=utf-8
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

# Expose ports for Airflow, Dashboard, and App
EXPOSE 8080 8050 8000

# Health check for Airflow webserver
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Override the default airflow entrypoint and start supervisord
ENTRYPOINT []
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
