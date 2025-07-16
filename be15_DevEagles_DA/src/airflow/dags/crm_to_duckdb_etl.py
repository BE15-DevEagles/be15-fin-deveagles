from datetime import datetime, timedelta
from typing import Dict, Any
import pandas as pd
import duckdb
import logging
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.dummy import DummyOperator
from airflow.providers.mysql.hooks.mysql import MySqlHook
from airflow.models import Variable

import sys
import os
sys.path.append('/opt/airflow/src')

default_args = {
    'owner': 'deveagles-da-team',
    'depends_on_past': False,
    'start_date': datetime(2024, 1, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 2,
    'retry_delay': timedelta(minutes=5),
    'catchup': False
}

dag = DAG(
    'crm_to_duckdb_etl',
    default_args=default_args,
    description='CRM to DuckDB ETL Pipeline',
    schedule_interval='0 2 * * *',
    max_active_runs=1,
    tags=['etl', 'duckdb', 'crm', 'analytics']
)

DUCKDB_PATH = '/opt/airflow/data/analytics.duckdb'
PARQUET_BASE_PATH = '/opt/airflow/data/parquet'
CRM_CONN_ID = 'crm_mariadb'

def get_crm_connection():
    """CRM MariaDB 데이터베이스 연결 생성"""
    try:
        from sqlalchemy import create_engine
        
        host = os.getenv('CRM_DB_HOST', 'host.docker.internal')
        port = os.getenv('CRM_DB_PORT', '3306')
        user = os.getenv('CRM_DB_USER', 'swcamp')
        password = os.getenv('CRM_DB_PASSWORD', 'swcamp')
        database = os.getenv('CRM_DB_NAME', 'beautifly')
        
        host_options = [host, 'host.docker.internal', 'localhost', '172.17.0.1']
        
        engine = None
        last_error = None
        
        for attempt_host in host_options:
            try:
                connection_url = f"mysql+pymysql://{user}:{password}@{attempt_host}:{port}/{database}?charset=utf8mb4"
                engine = create_engine(
                    connection_url,
                    pool_pre_ping=True,
                    pool_recycle=3600,
                    echo=False,
                    connect_args={"connect_timeout": 10}
                )
                with engine.connect() as conn:
                    conn.execute("SELECT 1")
                logging.info(f"CRM 데이터베이스 연결 성공: {attempt_host}")
                return engine
            except Exception as e:
                last_error = e
                logging.warning(f"호스트 {attempt_host} 연결 실패: {e}")
                if engine:
                    engine.dispose()
                engine = None
                continue
        
        raise last_error or Exception("모든 호스트 연결 옵션 실패")
        
    except Exception as e:
        logging.error(f"CRM 데이터베이스 연결 실패: {e}")
        try:
            from analytics.core.database import get_crm_db
            return get_crm_db()
        except Exception as fallback_error:
            logging.error(f"Fallback 연결도 실패: {fallback_error}")
            raise

def extract_customer_data(**context) -> str:
    """고객 데이터 추출 (Full Load)"""
    logging.info("고객 데이터 추출 시작")
    query = """
    SELECT 
        c.customer_id, c.customer_name, c.phone_number, c.visit_count,
        c.total_revenue, c.recent_visit_date, c.birthdate, c.noshow_count,
        c.gender, c.marketing_consent, c.channel_id, c.created_at,
        c.modified_at, c.shop_id, s.shop_name, s.industry_id,
        NOW() as extracted_at
    FROM customer c
    LEFT JOIN shop s ON c.shop_id = s.shop_id
    WHERE c.deleted_at IS NULL
    """
    try:
        engine = get_crm_connection()
        df = pd.read_sql_query(query, engine)
        output_path = f"{PARQUET_BASE_PATH}/customer/customer_{context['ds']}.parquet"
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)
        df.to_parquet(output_path, index=False)
        logging.info(f"고객 데이터 {len(df):,}건 추출 완료: {output_path}")
        return output_path
    except Exception as e:
        logging.error(f"고객 데이터 추출 실패: {e}")
        raise

def extract_shop_data(**context) -> str:
    """매장 데이터 추출 (Full Load)"""
    logging.info("매장 데이터 추출 시작")
    query = """
    SELECT 
        s.shop_id, s.shop_name, s.owner_id, s.address, s.detail_address,
        s.phone_number, s.business_number, s.industry_id, s.incentive_status,
        s.reservation_term, s.shop_description, s.created_at, s.modified_at,
        NOW() as extracted_at
    FROM shop s
    """
    try:
        engine = get_crm_connection()
        df = pd.read_sql_query(query, engine)
        output_path = f"{PARQUET_BASE_PATH}/shop/shop_{context['ds']}.parquet"
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)
        df.to_parquet(output_path, index=False)
        logging.info(f"매장 데이터 {len(df):,}건 추출 완료: {output_path}")
        return output_path
    except Exception as e:
        logging.error(f"매장 데이터 추출 실패: {e}")
        raise

def extract_reservation_data(**context) -> str:
    """예약 데이터 추출 (Incremental Load - 7 days)"""
    logging.info("예약 데이터 추출 시작")
    query = """
    SELECT 
        r.reservation_id, r.staff_id, r.shop_id, r.customer_id,
        r.reservation_status_name, r.staff_memo, r.reservation_memo,
        r.reservation_start_at, r.reservation_end_at, r.created_at,
        r.modified_at, s.shop_name, c.customer_name,
        NOW() as extracted_at
    FROM reservation r
    LEFT JOIN shop s ON r.shop_id = s.shop_id
    LEFT JOIN customer c ON r.customer_id = c.customer_id
    WHERE r.deleted_at IS NULL
      AND r.reservation_start_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    """
    try:
        engine = get_crm_connection()
        df = pd.read_sql_query(query, engine)
        output_path = f"{PARQUET_BASE_PATH}/reservation/reservation_{context['ds']}.parquet"
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)
        df.to_parquet(output_path, index=False)
        logging.info(f"예약 데이터 {len(df):,}건 추출 완료: {output_path}")
        return output_path
    except Exception as e:
        logging.error(f"예약 데이터 추출 실패: {e}")
        raise

def extract_sales_data(**context) -> str:
    """매출 데이터 추출 (Incremental Load - 7 days)"""
    logging.info("매출 데이터 추출 시작")
    query = """
    SELECT 
        sl.sales_id, sl.customer_id, sl.staff_id, sl.shop_id,
        sl.reservation_id, sl.discount_rate, sl.retail_price,
        sl.discount_amount, sl.total_amount, sl.sales_memo,
        sl.sales_date, sl.is_refunded, sl.created_at, sl.modified_at,
        c.customer_name, c.gender, c.birthdate, s.shop_name,
        NOW() as extracted_at
    FROM sales sl
    LEFT JOIN customer c ON sl.customer_id = c.customer_id
    LEFT JOIN shop s ON sl.shop_id = s.shop_id
    WHERE sl.deleted_at IS NULL
      AND sl.sales_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    """
    try:
        engine = get_crm_connection()
        df = pd.read_sql_query(query, engine)
        output_path = f"{PARQUET_BASE_PATH}/sales/sales_{context['ds']}.parquet"
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)
        df.to_parquet(output_path, index=False)
        logging.info(f"매출 데이터 {len(df):,}건 추출 완료: {output_path}")
        return output_path
    except Exception as e:
        logging.error(f"매출 데이터 추출 실패: {e}")
        raise

def create_duckdb_tables(**context):
    """DuckDB에 필요한 모든 테이블을 생성합니다."""
    logging.info("DuckDB 테이블 생성 시작")
    create_statements = """
    CREATE TABLE IF NOT EXISTS customer (
        customer_id BIGINT PRIMARY KEY, customer_name VARCHAR(100), phone_number VARCHAR(11),
        visit_count INTEGER, total_revenue INTEGER, recent_visit_date DATE, birthdate DATE,
        noshow_count INTEGER, gender VARCHAR(1), marketing_consent BOOLEAN, channel_id BIGINT,
        created_at TIMESTAMP, modified_at TIMESTAMP, shop_id BIGINT, shop_name VARCHAR(255),
        industry_id BIGINT, extracted_at TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS shop (
        shop_id BIGINT PRIMARY KEY, shop_name VARCHAR(255), owner_id BIGINT, address VARCHAR(500),
        detail_address VARCHAR(500), phone_number VARCHAR(20), business_number VARCHAR(20),
        industry_id BIGINT, incentive_status BOOLEAN, reservation_term INTEGER,
        shop_description TEXT, created_at TIMESTAMP, modified_at TIMESTAMP, extracted_at TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS reservation (
        reservation_id BIGINT PRIMARY KEY, staff_id BIGINT, shop_id BIGINT, customer_id BIGINT,
        reservation_status_name VARCHAR(20), staff_memo TEXT, reservation_memo TEXT,
        reservation_start_at TIMESTAMP, reservation_end_at TIMESTAMP, created_at TIMESTAMP,
        modified_at TIMESTAMP, shop_name VARCHAR(255), customer_name VARCHAR(100), extracted_at TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS sales (
        sales_id BIGINT PRIMARY KEY, customer_id BIGINT, staff_id BIGINT, shop_id BIGINT,
        reservation_id BIGINT, discount_rate INTEGER, retail_price INTEGER, discount_amount INTEGER,
        total_amount INTEGER, sales_memo VARCHAR(255), sales_date TIMESTAMP, is_refunded BOOLEAN,
        created_at TIMESTAMP, modified_at TIMESTAMP, customer_name VARCHAR(100), gender VARCHAR(1),
        birthdate DATE, shop_name VARCHAR(255), extracted_at TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS customer_analytics (
        customer_id INTEGER PRIMARY KEY,
        name VARCHAR,
        phone VARCHAR,
        email VARCHAR,
        birth_date DATE,
        gender VARCHAR,
        first_visit_date DATE,
        last_visit_date DATE,
        total_visits INTEGER DEFAULT 0,
        total_amount DECIMAL(15,2) DEFAULT 0.0,
        avg_visit_amount DECIMAL(15,2) DEFAULT 0.0,
        lifecycle_days INTEGER DEFAULT 0,
        days_since_last_visit INTEGER DEFAULT 0,
        visit_frequency DECIMAL(5,2) DEFAULT 0.0,
        preferred_services VARCHAR,
        preferred_employees VARCHAR,
        visits_3m INTEGER DEFAULT 0,
        amount_3m DECIMAL(15,2) DEFAULT 0.0,
        segment VARCHAR DEFAULT 'new',
        segment_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        churn_risk_score DECIMAL(5,2) DEFAULT 0.0,
        churn_risk_level VARCHAR DEFAULT 'low',
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS etl_metadata (
        table_name VARCHAR(50), last_updated TIMESTAMP, records_count BIGINT,
        etl_date DATE, status VARCHAR(20), PRIMARY KEY (table_name, etl_date)
    );
    """
    try:
        conn = duckdb.connect(DUCKDB_PATH)
        conn.executemany(create_statements)
        conn.close()
        logging.info("DuckDB 테이블 생성 완료")
    except Exception as e:
        logging.error(f"DuckDB 테이블 생성 실패: {e}")
        raise

def load_to_duckdb(**context):
    """Parquet 파일을 DuckDB에 적재합니다."""
    logging.info("DuckDB 적재 시작")
    etl_date = context['ds']
    conn = None
    
    create_metadata_table_query = """
    CREATE TABLE IF NOT EXISTS etl_metadata (
        table_name VARCHAR(50), last_updated TIMESTAMP, records_count BIGINT,
        etl_date DATE, status VARCHAR(20), PRIMARY KEY (table_name, etl_date)
    );
    """

    try:
        conn = duckdb.connect(DUCKDB_PATH)
        conn.execute(create_metadata_table_query)

        tables = {
            'customer': f"{PARQUET_BASE_PATH}/customer/customer_{etl_date}.parquet",
            'shop': f"{PARQUET_BASE_PATH}/shop/shop_{etl_date}.parquet", 
            'reservation': f"{PARQUET_BASE_PATH}/reservation/reservation_{etl_date}.parquet",
            'sales': f"{PARQUET_BASE_PATH}/sales/sales_{etl_date}.parquet"
        }
        for table_name, parquet_path in tables.items():
            if Path(parquet_path).exists():
                if table_name in ['customer', 'shop']:
                    conn.execute(f"DELETE FROM {table_name}")
                    logging.info(f"{table_name} 기존 데이터 삭제 완료 (Full Load)")
                else:
                    date_column = 'reservation_start_at' if table_name == 'reservation' else 'sales_date'
                    conn.execute(f"DELETE FROM {table_name} WHERE {date_column} >= (CURRENT_DATE - INTERVAL '7' DAY)")
                    logging.info(f"{table_name} 최근 7일 데이터 삭제 완료 (Incremental Load)")
                
                conn.execute(f"INSERT INTO {table_name} SELECT * FROM read_parquet('{parquet_path}')")
                
                record_count = conn.execute(f"SELECT COUNT(*) FROM read_parquet('{parquet_path}')").fetchone()[0]
                conn.execute("""
                INSERT INTO etl_metadata (table_name, last_updated, records_count, etl_date, status)
                VALUES (?, NOW(), ?, ?, 'SUCCESS')
                ON CONFLICT (table_name, etl_date) DO UPDATE SET
                    last_updated = excluded.last_updated,
                    records_count = excluded.records_count,
                    status = excluded.status;
                """, [table_name, record_count, etl_date])
                logging.info(f"{table_name} 적재 완료: {record_count:,}건")
            else:
                logging.warning(f"파일이 존재하지 않음: {parquet_path}")
    except Exception as e:
        logging.error(f"DuckDB 적재 실패: {e}")
        if conn:
            try:
                conn.execute("""
                INSERT INTO etl_metadata (table_name, last_updated, records_count, etl_date, status)
                VALUES ('ETL_PIPELINE', NOW(), 0, ?, 'FAILED')
                ON CONFLICT (table_name, etl_date) DO UPDATE SET
                    last_updated = excluded.last_updated,
                    status = excluded.status;
                """, [etl_date])
            except Exception as meta_e:
                logging.error(f"실패 메타데이터 업데이트 중 오류 발생: {meta_e}")
        raise
    finally:
        if conn:
            conn.close()
            logging.info("DuckDB 연결 종료")

def build_customer_analytics(**context):
    """원본 데이터에서 customer_analytics 테이블 생성/업데이트"""
    logging.info("customer_analytics 테이블 생성 시작")
    
    create_table_query = """
    CREATE TABLE IF NOT EXISTS customer_analytics (
        customer_id INTEGER PRIMARY KEY,
        name VARCHAR,
        phone VARCHAR,
        email VARCHAR,
        birth_date DATE,
        gender VARCHAR,
        first_visit_date DATE,
        last_visit_date DATE,
        total_visits INTEGER DEFAULT 0,
        total_amount DECIMAL(15,2) DEFAULT 0.0,
        avg_visit_amount DECIMAL(15,2) DEFAULT 0.0,
        lifecycle_days INTEGER DEFAULT 0,
        days_since_last_visit INTEGER DEFAULT 0,
        visit_frequency DECIMAL(5,2) DEFAULT 0.0,
        preferred_services VARCHAR,
        preferred_employees VARCHAR,
        visits_3m INTEGER DEFAULT 0,
        amount_3m DECIMAL(15,2) DEFAULT 0.0,
        segment VARCHAR DEFAULT 'new',
        segment_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        churn_risk_score DECIMAL(5,2) DEFAULT 0.0,
        churn_risk_level VARCHAR DEFAULT 'low',
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    """

    analytics_query = """
    INSERT INTO customer_analytics
    WITH customer_sales AS (
        SELECT
            customer_id,
            MIN(sales_date) as first_visit_date,
            MAX(sales_date) as last_visit_date,
            COUNT(DISTINCT sales_date) as total_visits,
            SUM(total_amount) as total_amount,
            SUM(CASE WHEN sales_date >= (CURRENT_DATE - INTERVAL '90' DAY) THEN total_amount ELSE 0 END) as amount_3m,
            COUNT(CASE WHEN sales_date >= (CURRENT_DATE - INTERVAL '90' DAY) THEN 1 END) as visits_3m
        FROM sales
        WHERE is_refunded = false
        GROUP BY customer_id
    ),
    customer_preferences AS (
        SELECT
            customer_id,
            STRING_AGG(DISTINCT CASE WHEN total_amount > 0 THEN 'service_' || sales_id END, ',') as preferred_services,
            STRING_AGG(DISTINCT CASE WHEN staff_id IS NOT NULL THEN 'staff_' || staff_id END, ',') as preferred_employees
        FROM sales
        WHERE is_refunded = false
        GROUP BY customer_id
    )
    SELECT 
        c.customer_id,
        c.customer_name as name,
        c.phone_number as phone,
        '' as email,
        c.birthdate as birth_date,
        c.gender,
        cs.first_visit_date,
        cs.last_visit_date,
        COALESCE(cs.total_visits, 0) as total_visits,
        COALESCE(cs.total_amount, 0) as total_amount,
        CASE WHEN COALESCE(cs.total_visits, 0) > 0 THEN cs.total_amount / cs.total_visits ELSE 0 END as avg_visit_amount,
        DATE_DIFF('day', c.created_at, CURRENT_DATE) as lifecycle_days,
        COALESCE(DATE_DIFF('day', cs.last_visit_date, CURRENT_DATE), 9999) as days_since_last_visit,
        CASE 
            WHEN DATE_DIFF('day', cs.first_visit_date, cs.last_visit_date) > 0 
            THEN (cs.total_visits - 1) * 30.0 / DATE_DIFF('day', cs.first_visit_date, cs.last_visit_date)
            ELSE 0 
        END as visit_frequency,
        COALESCE(cp.preferred_services, '') as preferred_services,
        COALESCE(cp.preferred_employees, '') as preferred_employees,
        COALESCE(cs.visits_3m, 0) as visits_3m,
        COALESCE(cs.amount_3m, 0) as amount_3m,
        'new' as segment,
        CURRENT_TIMESTAMP as segment_updated_at,
        0.0 as churn_risk_score,
        'low' as churn_risk_level,
        CURRENT_TIMESTAMP as updated_at
    FROM customer c
    LEFT JOIN customer_sales cs ON c.customer_id = cs.customer_id
    LEFT JOIN customer_preferences cp ON c.customer_id = cp.customer_id;
    """
    
    try:
        conn = duckdb.connect(DUCKDB_PATH)
        conn.execute(create_table_query)
        conn.execute("DELETE FROM customer_analytics")
        conn.execute(analytics_query)
        
        record_count = conn.execute("SELECT COUNT(*) FROM customer_analytics").fetchone()[0]
        
        conn.execute("""
        INSERT INTO etl_metadata (table_name, last_updated, records_count, etl_date, status)
        VALUES ('customer_analytics', NOW(), ?, ?, 'SUCCESS')
        ON CONFLICT (table_name, etl_date) DO UPDATE SET
            last_updated = excluded.last_updated,
            records_count = excluded.records_count,
            status = excluded.status;
        """, [record_count, context['ds']])
        
        conn.close()
        logging.info(f"customer_analytics 테이블 생성 완료: {record_count:,}건")
        
    except Exception as e:
        logging.error(f"customer_analytics 생성 실패: {e}")
        raise

def validate_data_quality(**context):
    """데이터 품질 검증"""
    logging.info("데이터 품질 검증 시작")
    try:
        conn = duckdb.connect(DUCKDB_PATH, read_only=True)
        validation_queries = [
            ("고객 데이터 PK 중복 체크", "SELECT COUNT(*) - COUNT(DISTINCT customer_id) FROM customer"),
            ("매장 데이터 PK 중복 체크", "SELECT COUNT(*) - COUNT(DISTINCT shop_id) FROM shop"),
            ("예약 데이터 FK null 체크 (customer_id)", "SELECT COUNT(*) FROM reservation WHERE customer_id IS NULL"),
            ("매출 데이터 음수 금액 체크", "SELECT COUNT(*) FROM sales WHERE total_amount < 0"),
            ("customer_analytics 레코드 수 체크", "SELECT COUNT(*) < 1 FROM customer_analytics")
        ]
        issues = []
        for check_name, query in validation_queries:
            result = conn.execute(query).fetchone()
            value = result[0]
            if value:
                issues.append(f"{check_name}: {value}건")
                logging.warning(f"데이터 품질 이슈 발견 - {check_name}: {value}")
            else:
                logging.info(f"✓ {check_name}: 정상")
        conn.close()
        if issues:
            raise ValueError(f"데이터 품질 검증 실패: {', '.join(issues)}")
        logging.info("데이터 품질 검증 완료 - 모든 검증 통과")
    except Exception as e:
        logging.error(f"데이터 품질 검증 실패: {e}")
        raise

start_task = DummyOperator(task_id='start', dag=dag)
create_tables_task = PythonOperator(task_id='create_duckdb_tables', python_callable=create_duckdb_tables, dag=dag)
extract_customer_task = PythonOperator(task_id='extract_customer_data', python_callable=extract_customer_data, dag=dag)
extract_shop_task = PythonOperator(task_id='extract_shop_data', python_callable=extract_shop_data, dag=dag)
extract_reservation_task = PythonOperator(task_id='extract_reservation_data', python_callable=extract_reservation_data, dag=dag)
extract_sales_task = PythonOperator(task_id='extract_sales_data', python_callable=extract_sales_data, dag=dag)
load_task = PythonOperator(task_id='load_to_duckdb', python_callable=load_to_duckdb, dag=dag)
build_analytics_task = PythonOperator(task_id='build_customer_analytics', python_callable=build_customer_analytics, dag=dag)
validate_task = PythonOperator(task_id='validate_data_quality', python_callable=validate_data_quality, dag=dag)
end_task = DummyOperator(task_id='end', dag=dag)

start_task >> create_tables_task
create_tables_task >> [
    extract_customer_task,
    extract_shop_task,
    extract_reservation_task,
    extract_sales_task
]
[
    extract_customer_task,
    extract_shop_task,
    extract_reservation_task,
    extract_sales_task
] >> load_task
load_task >> build_analytics_task >> validate_task >> end_task