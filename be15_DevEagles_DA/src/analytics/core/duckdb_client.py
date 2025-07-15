"""
DuckDB 클라이언트 모듈

ETL로 적재된 DuckDB 데이터에 접근하기 위한 클라이언트 클래스
"""

import pandas as pd
from typing import Optional, Dict, List, Any
import logging
from datetime import datetime, timedelta

from analytics.core.database import get_analytics_db, get_crm_db
from analytics.core.logging import get_logger


class DuckDBClient:
    """DuckDB 연결 및 쿼리 실행 클라이언트"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self._connection = None
        
    def connect(self):
        """기존 database.py의 DuckDB 연결 재사용"""
        try:
            if self._connection is None:
                self._connection = get_analytics_db()
                self.logger.info("DuckDB 연결 성공 (기존 database.py 재사용)")
            return self._connection
        except Exception as e:
            self.logger.error(f"DuckDB 연결 실패: {e}")
            raise
            
    def close(self):
        """DuckDB 연결 해제 (기존 연결 관리는 database.py에서 처리)"""
        # 기존 database.py의 global manager가 연결을 관리하므로 여기서는 참조만 해제
        self._connection = None
        self.logger.info("DuckDB 연결 참조 해제")
            
    def execute_query(self, query: str, params: List[Any] = None) -> pd.DataFrame:
        """쿼리 실행하여 DataFrame 반환"""
        try:
            conn = self.connect()
            if params:
                result = conn.execute(query, params).fetchdf()
            else:
                result = conn.execute(query).fetchdf()
            
            self.logger.debug(f"쿼리 실행 완료: {len(result)}건 조회")
            return result
            
        except Exception as e:
            self.logger.error(f"쿼리 실행 실패: {e}")
            raise
            
    def execute_sql(self, query: str, params: List[Any] = None) -> Any:
        """SQL 실행 (SELECT 외 명령용)"""
        try:
            conn = self.connect()
            if params:
                result = conn.execute(query, params)
            else:
                result = conn.execute(query)
            
            self.logger.debug("SQL 실행 완료")
            return result
            
        except Exception as e:
            self.logger.error(f"SQL 실행 실패: {e}")
            raise
            
    def get_table_info(self, table_name: str) -> Dict[str, Any]:
        """테이블 정보 조회"""
        try:
            # 테이블 존재 여부 확인
            exists_query = """
            SELECT COUNT(*) as count 
            FROM information_schema.tables 
            WHERE table_name = ?
            """
            exists_result = self.execute_query(exists_query, [table_name])
            
            if exists_result['count'].iloc[0] == 0:
                return {"exists": False, "message": f"테이블 '{table_name}'이 존재하지 않습니다."}
            
            # 테이블 스키마 정보
            schema_query = f"DESCRIBE {table_name}"
            schema_df = self.execute_query(schema_query)
            
            # 레코드 수
            count_query = f"SELECT COUNT(*) as record_count FROM {table_name}"
            count_result = self.execute_query(count_query)
            
            # 최근 업데이트 시간
            update_query = f"SELECT MAX(extracted_at) as last_updated FROM {table_name}"
            update_result = self.execute_query(update_query)
            
            return {
                "exists": True,
                "schema": schema_df.to_dict('records'),
                "record_count": count_result['record_count'].iloc[0],
                "last_updated": update_result['last_updated'].iloc[0]
            }
            
        except Exception as e:
            self.logger.error(f"테이블 정보 조회 실패: {e}")
            return {"exists": False, "error": str(e)}
            
    def get_etl_status(self) -> pd.DataFrame:
        """ETL 실행 상태 조회"""
        try:
            query = """
            SELECT 
                table_name,
                last_updated,
                records_count,
                etl_date,
                status
            FROM etl_metadata
            ORDER BY last_updated DESC
            """
            return self.execute_query(query)
            
        except Exception as e:
            self.logger.error(f"ETL 상태 조회 실패: {e}")
            return pd.DataFrame()
            
    def check_data_freshness(self, hours: int = 24) -> Dict[str, Any]:
        """데이터 신선도 체크 (최근 N시간 내 업데이트 여부)"""
        try:
            cutoff_time = datetime.now() - timedelta(hours=hours)
            
            tables = ['customers', 'shops', 'reservations', 'sales']
            freshness_report = {}
            
            for table in tables:
                query = f"""
                SELECT 
                    COUNT(*) as total_records,
                    MAX(extracted_at) as last_updated,
                    CASE 
                        WHEN MAX(extracted_at) > ? THEN 'FRESH'
                        ELSE 'STALE'
                    END as freshness_status
                FROM {table}
                """
                
                result = self.execute_query(query, [cutoff_time])
                if not result.empty:
                    freshness_report[table] = {
                        'total_records': result['total_records'].iloc[0],
                        'last_updated': result['last_updated'].iloc[0],
                        'freshness_status': result['freshness_status'].iloc[0]
                    }
                    
            return freshness_report
            
        except Exception as e:
            self.logger.error(f"데이터 신선도 체크 실패: {e}")
            return {}


class AnalyticsDataService:
    """분석 데이터 접근을 위한 서비스 클래스"""
    
    def __init__(self, duckdb_client: Optional[DuckDBClient] = None):
        self.logger = get_logger(__name__)
        self.db_client = duckdb_client or DuckDBClient()
        
    def get_customer_data(self, shop_id: Optional[int] = None, 
                         include_deleted: bool = False) -> pd.DataFrame:
        """고객 데이터 조회"""
        query = """
        SELECT 
            customer_id,
            customer_name,
            phone_number,
            visit_count,
            total_revenue,
            recent_visit_date,
            birthdate,
            noshow_count,
            gender,
            marketing_consent,
            channel_id,
            created_at,
            shop_id,
            shop_name,
            industry_id
        FROM customers
        WHERE 1=1
        """
        
        params = []
        
        if shop_id:
            query += " AND shop_id = ?"
            params.append(shop_id)
            
        query += " ORDER BY created_at DESC"
        
        return self.db_client.execute_query(query, params if params else None)
        
    def get_reservation_data(self, days_back: int = 30,
                           shop_id: Optional[int] = None) -> pd.DataFrame:
        """예약 데이터 조회"""
        query = """
        SELECT 
            reservation_id,
            staff_id,
            shop_id,
            customer_id,
            reservation_status_name,
            reservation_start_at,
            reservation_end_at,
            created_at,
            shop_name,
            customer_name
        FROM reservations
        WHERE reservation_start_at >= CURRENT_DATE - INTERVAL '{days}' DAY
        """.format(days=days_back)
        
        params = []
        
        if shop_id:
            query += " AND shop_id = ?"
            params.append(shop_id)
            
        query += " ORDER BY reservation_start_at DESC"
        
        return self.db_client.execute_query(query, params if params else None)
        
    def get_sales_data(self, days_back: int = 30,
                      shop_id: Optional[int] = None) -> pd.DataFrame:
        """매출 데이터 조회"""
        query = """
        SELECT 
            sales_id,
            customer_id,
            staff_id,
            shop_id,
            total_amount,
            sales_date,
            is_refunded,
            customer_name,
            gender,
            birthdate,
            shop_name
        FROM sales
        WHERE sales_date >= CURRENT_DATE - INTERVAL '{days}' DAY
          AND is_refunded = false
        """.format(days=days_back)
        
        params = []
        
        if shop_id:
            query += " AND shop_id = ?"
            params.append(shop_id)
            
        query += " ORDER BY sales_date DESC"
        
        return self.db_client.execute_query(query, params if params else None)
        
    def get_cohort_analysis_data(self) -> pd.DataFrame:
        """코호트 분석용 데이터 조회"""
        query = """
        SELECT 
            c.customer_id,
            c.customer_name,
            c.created_at as customer_created_at,
            c.shop_id,
            c.shop_name,
            c.gender,
            c.birthdate,
            c.total_revenue,
            c.visit_count,
            c.recent_visit_date,
            r.reservation_id,
            r.reservation_start_at,
            r.reservation_status_name
        FROM customers c
        LEFT JOIN reservations r ON c.customer_id = r.customer_id
        WHERE r.reservation_status_name IN ('CONFIRMED', 'PAID')
        ORDER BY c.created_at, r.reservation_start_at
        """
        
        return self.db_client.execute_query(query)
        
    def get_churn_analysis_data(self) -> pd.DataFrame:
        """이탈 분석용 데이터 조회"""
        query = """
        SELECT 
            c.customer_id,
            c.customer_name,
            c.phone_number,
            c.visit_count,
            c.total_revenue,
            c.recent_visit_date,
            c.birthdate,
            c.noshow_count,
            c.gender,
            c.marketing_consent,
            c.channel_id,
            c.created_at,
            c.shop_id,
            c.shop_name,
            COUNT(r.reservation_id) as total_reservations,
            COUNT(CASE WHEN r.reservation_status_name = 'PAID' THEN 1 END) as paid_reservations,
            COUNT(CASE WHEN r.reservation_status_name = 'NO_SHOW' THEN 1 END) as noshow_reservations,
            COUNT(CASE WHEN r.reservation_status_name IN ('CBC', 'CBS') THEN 1 END) as cancelled_reservations,
            AVG(s.total_amount) as avg_order_value,
            SUM(s.total_amount) as total_sales_amount,
            COUNT(s.sales_id) as total_sales_count
        FROM customers c
        LEFT JOIN reservations r ON c.customer_id = r.customer_id
        LEFT JOIN sales s ON c.customer_id = s.customer_id
        GROUP BY c.customer_id
        """
        
        return self.db_client.execute_query(query)
        
    def get_shop_performance_summary(self) -> pd.DataFrame:
        """매장별 성과 요약"""
        query = """
        SELECT 
            s.shop_id,
            s.shop_name,
            s.industry_id,
            COUNT(DISTINCT c.customer_id) as total_customers,
            COUNT(r.reservation_id) as total_reservations,
            COUNT(CASE WHEN r.reservation_status_name = 'PAID' THEN 1 END) as paid_reservations,
            SUM(sl.total_amount) as total_revenue,
            AVG(sl.total_amount) as avg_order_value,
            COUNT(DISTINCT DATE(r.reservation_start_at)) as active_days
        FROM shops s
        LEFT JOIN customers c ON s.shop_id = c.shop_id
        LEFT JOIN reservations r ON s.shop_id = r.shop_id
        LEFT JOIN sales sl ON s.shop_id = sl.shop_id
        GROUP BY s.shop_id, s.shop_name, s.industry_id
        ORDER BY total_revenue DESC
        """
        
        return self.db_client.execute_query(query)
        
    def close(self):
        """연결 해제"""
        if self.db_client:
            self.db_client.close()


def get_duckdb_client() -> DuckDBClient:
    """DuckDB 클라이언트 인스턴스 생성"""
    return DuckDBClient()


def get_analytics_data_service() -> AnalyticsDataService:
    """분석 데이터 서비스 인스턴스 생성"""
    return AnalyticsDataService()