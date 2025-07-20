#!/usr/bin/env python3
"""Database connection test script."""

import os
import sys
from pathlib import Path
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError

# 프로젝트 루트를 Python 경로에 추가
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "src"))

def test_mariadb_connection():
    """MariaDB 연결 테스트."""
    print("🔍 Testing MariaDB connection...")
    print("=" * 50)
    
    # 여러 가능한 연결 정보 시도
    connection_configs = [
        {
            "name": "swcamp/swcamp",
            "url": "mysql+pymysql://swcamp:swcamp@localhost:3306/beautifly"
        },
        {
            "name": "root (no password)",
            "url": "mysql+pymysql://root@localhost:3306/beautifly"
        },
        {
            "name": "root/root",
            "url": "mysql+pymysql://root:root@localhost:3306/beautifly"
        },
        {
            "name": "Environment Variable",
            "url": os.getenv("CRM_DATABASE_URL", "mysql+pymysql://swcamp:swcamp@localhost:3306/beautifly")
        }
    ]
    
    for config in connection_configs:
        print(f"\nTesting {config['name']}:")
        print(f"URL: {config['url']}")
        
        try:
            engine = create_engine(config['url'], echo=False)
            
            with engine.connect() as conn:
                # 기본 연결 테스트
                result = conn.execute(text("SELECT 1 as test"))
                test_result = result.fetchone()
                
                if test_result and test_result[0] == 1:
                    print("✅ Connection successful!")
                    
                    # 데이터베이스 목록 확인
                    databases = conn.execute(text("SHOW DATABASES"))
                    db_list = [row[0] for row in databases.fetchall()]
                    print(f"Available databases: {db_list}")
                    
                    # beautifly 데이터베이스 존재 확인
                    if 'beautifly' in db_list:
                        print("✅ beautifly database found!")
                        
                        # 테이블 목록 확인
                        conn.execute(text("USE beautifly"))
                        tables = conn.execute(text("SHOW TABLES"))
                        table_list = [row[0] for row in tables.fetchall()]
                        print(f"Tables in beautifly: {table_list}")
                        
                        return True, config['url'], table_list
                    else:
                        print("❌ beautifly database not found")
                        return False, config['url'], []
                else:
                    print("❌ Connection test failed")
                    
        except SQLAlchemyError as e:
            print(f"❌ SQLAlchemy error: {e}")
        except Exception as e:
            print(f"❌ Unexpected error: {e}")
    
    print("\n❌ All connection attempts failed")
    return False, None, []

def main():
    """메인 함수."""
    try:
        success, working_url, tables = test_mariadb_connection()
        
        if success:
            print(f"\n🎉 Database connection successful!")
            print(f"Working URL: {working_url}")
            print(f"Found {len(tables)} tables in beautifly database")
            
            if tables:
                print("\n📋 Tables found:")
                for table in tables:
                    print(f"  - {table}")
            
            return True
        else:
            print("\n❌ Database connection failed")
            print("\nPlease check:")
            print("1. MariaDB/MySQL server is running")
            print("2. Database 'beautifly' exists")
            print("3. User credentials are correct")
            print("4. Required Python packages are installed (pymysql)")
            return False
            
    except Exception as e:
        print(f"❌ Test failed with error: {e}")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 