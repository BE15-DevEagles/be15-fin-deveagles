"""Database connection management."""

from contextlib import contextmanager
from typing import Generator

import duckdb
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import sessionmaker

from analytics.core.config import settings
from analytics.core.logging import get_logger

logger = get_logger("database")


class DatabaseManager:
    """Database connection manager for CRM and Analytics databases."""

    def __init__(self) -> None:
        self._crm_engine: Engine | None = None
        self._analytics_conn: duckdb.DuckDBPyConnection | None = None

    def get_crm_engine(self) -> Engine:
        """Get SQLAlchemy engine for CRM database (MariaDB/RDS)."""
        if self._crm_engine is None:
            logger.info("Creating CRM database engine")
            
            import os
            
            # AWS RDS 환경 확인
            is_aws_rds = any([
                "rds.amazonaws.com" in settings.crm_database_url,
                os.getenv("AWS_DEFAULT_REGION"),
                os.getenv("RDS_ENDPOINT")
            ])
            
            # 기본 연결 설정
            connect_args = {
                "charset": "utf8mb4",
                "autocommit": True,
                "connect_timeout": 60,
                "read_timeout": 60,
                "write_timeout": 60,
            }
            
            # AWS RDS SSL 설정
            if is_aws_rds:
                ssl_ca_path = os.getenv("SSL_CA_CERT_PATH")
                if ssl_ca_path and os.path.exists(ssl_ca_path):
                    connect_args.update({
                        "ssl_ca": ssl_ca_path,
                        "ssl_verify_cert": os.getenv("SSL_VERIFY_CERT", "false").lower() == "true",
                        "ssl_verify_identity": False,  # RDS는 보통 False
                    })
                    logger.info("Using SSL connection for RDS")
                else:
                    # SSL 인증서가 없어도 RDS는 기본적으로 SSL 지원
                    connect_args["ssl_disabled"] = False
                    logger.info("Using default SSL for RDS")
            
            # 연결 시도
            connection_attempts = [
                # 1차: 원본 URL 그대로
                settings.crm_database_url,
                # 2차: auth plugin 추가
                self._add_auth_plugin(settings.crm_database_url),
                # 3차: 파라미터 제거 후 재시도
                self._clean_url_and_add_auth(settings.crm_database_url)
            ]
            
            last_error = None
            
            for attempt, url in enumerate(connection_attempts, 1):
                try:
                    logger.info(f"Database connection attempt {attempt}")
                    
                    self._crm_engine = create_engine(
                        url,
                        pool_size=settings.crm_pool_size,
                        max_overflow=settings.crm_max_overflow,
                        pool_pre_ping=True,
                        echo=settings.debug,
                        connect_args=connect_args,
                        pool_recycle=3600,  # AWS RDS 권장: 1시간
                        pool_timeout=30,
                        # AWS RDS 최적화
                        pool_reset_on_return="commit" if is_aws_rds else "rollback",
                    )
                    
                    # 연결 테스트
                    with self._crm_engine.connect() as conn:
                        result = conn.execute("SELECT 1 as test, @@version as version")
                        row = result.fetchone()
                        logger.info(f"CRM database connection successful: {row}")
                        return self._crm_engine
                        
                except Exception as e:
                    last_error = e
                    logger.warning(f"Connection attempt {attempt} failed: {e}")
                    if self._crm_engine:
                        self._crm_engine.dispose()
                        self._crm_engine = None
            
            # 모든 시도 실패
            logger.error(f"All connection attempts failed. Last error: {last_error}")
            raise last_error
                    
        return self._crm_engine
    
    def _add_auth_plugin(self, url: str) -> str:
        """Add auth plugin to URL if not present."""
        if "auth_plugin_map" in url:
            return url
        
        separator = "&" if "?" in url else "?"
        return f"{url}{separator}auth_plugin_map=auth_gssapi_client:mysql_native_password"
    
    def _clean_url_and_add_auth(self, url: str) -> str:
        """Clean URL parameters and add auth plugin."""
        base_url = url.split('?')[0]
        return f"{base_url}?charset=utf8mb4&auth_plugin_map=auth_gssapi_client:mysql_native_password"

    def get_analytics_connection(self) -> duckdb.DuckDBPyConnection:
        """Get DuckDB connection for analytics database."""
        if self._analytics_conn is None:
            logger.info(f"Creating Analytics database connection: {settings.analytics_db_path}")
            try:
                # Create directory if it doesn't exist
                import os
                db_path = settings.analytics_db_path
                db_dir = os.path.dirname(db_path)
                if db_dir and not os.path.exists(db_dir):
                    os.makedirs(db_dir, exist_ok=True)
                
                # Create DuckDB connection with UTF-8 encoding
                self._analytics_conn = duckdb.connect(
                    db_path,
                    config={
                        "threads": settings.analytics_db_threads,
                        "memory_limit": "2GB",
                        "max_memory": "4GB",
                    }
                )
                
                # Initialize basic tables if they don't exist
                self._initialize_analytics_tables()
                
            except Exception as e:
                logger.error(f"Failed to create analytics database connection: {e}")
                # Create in-memory database as fallback
                logger.info("Using in-memory DuckDB as fallback")
                self._analytics_conn = duckdb.connect(
                    ":memory:",
                    config={
                        "threads": settings.analytics_db_threads,
                        "memory_limit": "2GB",
                        "max_memory": "4GB",
                    }
                )
                self._initialize_analytics_tables()
                
        return self._analytics_conn

    def _initialize_analytics_tables(self) -> None:
        """Initialize basic analytics tables if they don't exist."""
        try:
            # Create customer_analytics table with basic structure
            create_customer_analytics_sql = """
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
            )
            """
            
            # Create visit_analytics table
            create_visit_analytics_sql = """
            CREATE TABLE IF NOT EXISTS visit_analytics (
                visit_id INTEGER PRIMARY KEY,
                customer_id INTEGER NOT NULL,
                employee_id INTEGER,
                visit_date TIMESTAMP NOT NULL,
                total_amount DECIMAL(15,2) NOT NULL,
                discount_amount DECIMAL(15,2) DEFAULT 0.0,
                final_amount DECIMAL(15,2) NOT NULL,
                service_count INTEGER DEFAULT 0,
                service_categories VARCHAR,
                service_names VARCHAR,
                duration_minutes INTEGER,
                is_first_visit BOOLEAN DEFAULT FALSE,
                days_since_previous_visit INTEGER,
                visit_sequence INTEGER DEFAULT 1,
                visit_hour INTEGER DEFAULT 0,
                visit_weekday INTEGER DEFAULT 0,
                visit_month INTEGER DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
            
            # Create etl_metadata table
            create_etl_metadata_sql = """
            CREATE TABLE IF NOT EXISTS etl_metadata (
                table_name VARCHAR PRIMARY KEY,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                records_processed INTEGER DEFAULT 0,
                records_inserted INTEGER DEFAULT 0,
                records_updated INTEGER DEFAULT 0,
                records_deleted INTEGER DEFAULT 0,
                processing_time_seconds DECIMAL(10,2) DEFAULT 0.0,
                status VARCHAR DEFAULT 'completed',
                error_message VARCHAR
            )
            """
            
            # Create customer_service_preferences table
            create_preferences_sql = """
            CREATE TABLE IF NOT EXISTS customer_service_preferences (
                customer_id INTEGER NOT NULL,
                service_id INTEGER NOT NULL,
                service_name VARCHAR NOT NULL,
                service_category VARCHAR,
                total_visits INTEGER DEFAULT 0,
                total_amount DECIMAL(15,2) DEFAULT 0.0,
                avg_amount DECIMAL(15,2) DEFAULT 0.0,
                first_service_date TIMESTAMP,
                last_service_date TIMESTAMP,
                preference_rank INTEGER DEFAULT 1,
                visit_ratio DECIMAL(5,2) DEFAULT 0.0,
                amount_ratio DECIMAL(5,2) DEFAULT 0.0,
                recent_visits_3m INTEGER DEFAULT 0,
                days_since_last_service INTEGER,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (customer_id, service_id)
            )
            """
            
            # Create customer_service_tags table
            create_tags_sql = """
            CREATE TABLE IF NOT EXISTS customer_service_tags (
                customer_id INTEGER PRIMARY KEY,
                top_service_1 VARCHAR,
                top_service_2 VARCHAR,
                top_service_3 VARCHAR,
                preferred_categories VARCHAR,
                service_variety_score DECIMAL(5,2) DEFAULT 0.0,
                loyalty_services VARCHAR,
                avg_service_price DECIMAL(15,2) DEFAULT 0.0,
                preferred_price_range VARCHAR DEFAULT 'medium',
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
            
            # Create original source tables for ETL consistency
            create_customer_sql = """
            CREATE TABLE IF NOT EXISTS customer (
                customer_id BIGINT PRIMARY KEY, 
                customer_name VARCHAR(100), 
                phone_number VARCHAR(11),
                visit_count INTEGER, 
                total_revenue INTEGER, 
                recent_visit_date DATE, 
                birthdate DATE,
                noshow_count INTEGER, 
                gender VARCHAR(1), 
                marketing_consent BOOLEAN, 
                channel_id BIGINT,
                created_at TIMESTAMP, 
                modified_at TIMESTAMP, 
                shop_id BIGINT, 
                shop_name VARCHAR(255),
                industry_id BIGINT, 
                extracted_at TIMESTAMP
            )
            """
            
            create_shop_sql = """
            CREATE TABLE IF NOT EXISTS shop (
                shop_id BIGINT PRIMARY KEY, 
                shop_name VARCHAR(255), 
                owner_id BIGINT, 
                address VARCHAR(500),
                detail_address VARCHAR(500), 
                phone_number VARCHAR(20), 
                business_number VARCHAR(20),
                industry_id BIGINT, 
                incentive_status BOOLEAN, 
                reservation_term INTEGER,
                shop_description TEXT, 
                created_at TIMESTAMP, 
                modified_at TIMESTAMP, 
                extracted_at TIMESTAMP
            )
            """
            
            create_reservation_sql = """
            CREATE TABLE IF NOT EXISTS reservation (
                reservation_id BIGINT PRIMARY KEY, 
                staff_id BIGINT, 
                shop_id BIGINT, 
                customer_id BIGINT,
                reservation_status_name VARCHAR(20), 
                staff_memo TEXT, 
                reservation_memo TEXT,
                reservation_start_at TIMESTAMP, 
                reservation_end_at TIMESTAMP, 
                created_at TIMESTAMP,
                modified_at TIMESTAMP, 
                shop_name VARCHAR(255), 
                customer_name VARCHAR(100), 
                extracted_at TIMESTAMP
            )
            """
            
            create_sales_sql = """
            CREATE TABLE IF NOT EXISTS sales (
                sales_id BIGINT PRIMARY KEY, 
                customer_id BIGINT, 
                staff_id BIGINT, 
                shop_id BIGINT,
                reservation_id BIGINT, 
                discount_rate INTEGER, 
                retail_price INTEGER, 
                discount_amount INTEGER,
                total_amount INTEGER, 
                sales_memo VARCHAR(255), 
                sales_date TIMESTAMP, 
                is_refunded BOOLEAN,
                created_at TIMESTAMP, 
                modified_at TIMESTAMP, 
                customer_name VARCHAR(100), 
                gender VARCHAR(1),
                birthdate DATE, 
                shop_name VARCHAR(255), 
                extracted_at TIMESTAMP
            )
            """
            
            # Execute all table creation queries
            self._analytics_conn.execute(create_customer_analytics_sql)
            self._analytics_conn.execute(create_visit_analytics_sql)
            self._analytics_conn.execute(create_etl_metadata_sql)
            self._analytics_conn.execute(create_preferences_sql)
            self._analytics_conn.execute(create_tags_sql)
            self._analytics_conn.execute(create_customer_sql)
            self._analytics_conn.execute(create_shop_sql)
            self._analytics_conn.execute(create_reservation_sql)
            self._analytics_conn.execute(create_sales_sql)
            
            # Initialize metadata
            self._analytics_conn.execute("""
                INSERT OR REPLACE INTO etl_metadata (table_name, status) 
                VALUES 
                    ('customer_analytics', 'initialized'),
                    ('visit_analytics', 'initialized'),
                    ('customer_service_preferences', 'initialized'),
                    ('customer_service_tags', 'initialized'),
                    ('customer', 'initialized'),
                    ('shop', 'initialized'),
                    ('reservation', 'initialized'),
                    ('sales', 'initialized')
            """)
            
            logger.info("Analytics tables initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize analytics tables: {e}")

    @contextmanager
    def crm_session(self) -> Generator:
        """Context manager for CRM database sessions."""
        engine = self.get_crm_engine()
        Session = sessionmaker(bind=engine)
        session = Session()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    @contextmanager
    def analytics_transaction(self) -> Generator:
        """Context manager for Analytics database transactions."""
        conn = self.get_analytics_connection()
        try:
            conn.begin()
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise

    def close_connections(self) -> None:
        """Close all database connections."""
        if self._crm_engine:
            self._crm_engine.dispose()
            self._crm_engine = None

        if self._analytics_conn:
            self._analytics_conn.close()
            self._analytics_conn = None

        logger.info("All database connections closed")


# Global database manager instance
db_manager = DatabaseManager()


def get_crm_db() -> Engine:
    """Dependency function for CRM database engine."""
    return db_manager.get_crm_engine()


def get_analytics_db() -> duckdb.DuckDBPyConnection:
    """Dependency function for Analytics database connection."""
    return db_manager.get_analytics_connection() 