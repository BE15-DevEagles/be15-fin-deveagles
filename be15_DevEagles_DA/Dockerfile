FROM apache/airflow:2.7.0-python3.9
USER airflow

# 필수 패키지 설치
RUN pip install --no-cache-dir \
    pandas==2.0.3 \
    duckdb==0.8.1 \
    psycopg2-binary==2.9.9 \
    pydantic-settings \
    pymysql==1.1.0 \
    cryptography==41.0.7 \
    sqlalchemy==1.4.53 \
    scikit-learn==1.3.0 \
    numpy==1.24.3 \
    matplotlib==3.7.2 \
    seaborn==0.12.2 \
    pyarrow==10.0.1 \
    plotly==5.15.0 
    

# 환경 변수 설정
ENV PYTHONPATH=/opt/airflow/src:$PYTHONPATH
ENV PYTHONIOENCODING=utf-8 