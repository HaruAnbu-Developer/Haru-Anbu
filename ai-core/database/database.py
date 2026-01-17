# database.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import logging
import os 
from dotenv import load_dotenv
load_dotenv()

# 1. RDS 접속 정보 설정
# 0.0.0.0으로 열려있으므로 호스트 주소와 계정 정보만 정확하면 됩니다. -> 이거 보안좀 그러니까 보안정책(그룹) 업데이트 해야할 듯합니다 형님
USER = os.getenv('DB_USER')
PASSWORD = os.getenv('DB_PASSWORD')
HOST = os.getenv('DB_HOST')
DB_PORT = os.getenv('DB_PORT')
DB_NAME = os.getenv('DB_NAME')

DATABASE_URL = f"mysql+pymysql://{USER}:{PASSWORD}@{HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"

# 2. SQLAlchemy 엔진 및 세션 설정
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,  # 연결 유효성 체크 (RDS 연결 유지 핵심)
    echo=True           # True로 설정 시 실행되는 SQL문이 로그에 찍힙니다.
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# 3. DB 세션 의존성 (FastAPI용)
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 4. 테이블 생성 함수 (백엔드 공유 및 초기화용)
def init_db():
    # 여기서 schema.py를 임포트해야 Base.metadata에 테이블 정보가 등록됩니다.
    from database import engine, Base
    import schema 
    try:
        # DB_NAME에 해당하는 데이터베이스가 있어야 실행
        Base.metadata.create_all(bind=engine)
        print(f"✅ Success: Tables created in RDS ({DB_NAME})")
    except Exception as e:
        print(f"❌ Error: Failed to connect or create tables: {e}")

if __name__ == "__main__":
    init_db()