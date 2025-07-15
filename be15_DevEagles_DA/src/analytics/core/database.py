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
        """Get SQLAlchemy engine for CRM database (MariaDB)."""
        if self._crm_engine is None:
            logger.info("Creating CRM DA database engine")
            
            connect_args = {
                "charset": "utf8mb4",
                "autocommit": True,
                "connect_timeout": 60,
                "read_timeout": 60,
                "write_timeout": 60,
            }
            
            try:
                url = settings.crm_database_url
                if "?" in url:
                    url += "&auth_plugin_map=auth_gssapi_client:mysql_native_password"
                else:
                    url += "?auth_plugin_map=auth_gssapi_client:mysql_native_password"
                
                self._crm_engine = create_engine(
                    url,
                    pool_size=settings.crm_pool_size,
                    max_overflow=settings.crm_max_overflow,
                    pool_pre_ping=True,
                    echo=settings.debug,
                    connect_args=connect_args,
                    pool_recycle=3600,  # 1시간마다 연결 재활용
                    pool_timeout=30,
                )
                
                # 연결 테스트
                with self._crm_engine.connect() as conn:
                    conn.execute("SELECT 1")
                    logger.info("CRM database connection test successful")
                    
            except Exception as e:
                logger.error(f"Failed to create CRM engine: {e}")
                backup_url = settings.crm_database_url.split('?')[0]  # 파라미터 제거
                logger.info(f"Trying backup connection without auth plugin: {backup_url}")
                
                try:
                    if "?" in backup_url:
                        backup_url += "&auth_plugin_map=auth_gssapi_client:mysql_native_password"
                    else:
                        backup_url += "?auth_plugin_map=auth_gssapi_client:mysql_native_password"
                    
                    self._crm_engine = create_engine(
                        backup_url,
                        pool_size=settings.crm_pool_size,
                        max_overflow=settings.crm_max_overflow,
                        pool_pre_ping=True,
                        echo=settings.debug,
                        connect_args={"charset": "utf8mb4"},
                        pool_recycle=3600,
                        pool_timeout=30,
                    )
                    
                    # 백업 연결 테스트
                    with self._crm_engine.connect() as conn:
                        conn.execute("SELECT 1")
                        logger.info("CRM database backup connection successful")
                        
                except Exception as backup_error:
                    logger.error(f"Backup connection also failed: {backup_error}")
                    raise backup_error
                    
        return self._crm_engine

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
            
            # Execute all table creation queries
            self._analytics_conn.execute(create_customer_analytics_sql)
            self._analytics_conn.execute(create_visit_analytics_sql)
            self._analytics_conn.execute(create_etl_metadata_sql)
            self._analytics_conn.execute(create_preferences_sql)
            self._analytics_conn.execute(create_tags_sql)
            
            # Initialize metadata
            self._analytics_conn.execute("""
                INSERT OR REPLACE INTO etl_metadata (table_name, status) 
                VALUES 
                    ('customer_analytics', 'initialized'),
                    ('visit_analytics', 'initialized'),
                    ('customer_service_preferences', 'initialized'),
                    ('customer_service_tags', 'initialized')
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