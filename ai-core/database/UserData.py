from sqlalchemy import Column, Integer, String, Text, Float, ForeignKey, DateTime, JSON, Boolean
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.sql import func

Base = declarative_base()

class VoiceProfile(Base):
    """자녀의 목소리 특징값 관리 (TTS용)"""
    __tablename__ = "voice_profiles"
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(50), unique=True, nullable=False) # 백엔드와 공유하는 ID
    raw_wav_path = Column(String(255))  # 자녀가 올린 원본 파일 경로
    latent_path = Column(String(255))   # XTTS 특징 추출값(.json 또는 .pth) 경로
    updated_at = Column(DateTime, onupdate=func.now())

class DialogLog(Base):
    """실시간 대화 텍스트 기록 (전체 대화 내용 저장)"""
    __tablename__ = "dialog_logs"
    
    id = Column(Integer, primary_key=True)
    conversation_id = Column(String(50), nullable=False) # 한 번의 통화 단위 ID
    user_id = Column(String(50), ForeignKey("voice_profiles.user_id"))
    role = Column(String(10)) # 'elder' 또는 'ai'
    content = Column(Text, nullable=False) # 대화 내용
    created_at = Column(DateTime, default=func.now())

class ConversationAnalysis(Base):
    """통화 종료 후 총체적 분석 (유저당 히스토리 관리)"""
    __tablename__ = "conversation_analysis"
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(50), ForeignKey("voice_profiles.user_id"))
    conversation_id = Column(String(50), unique=True)
    
    # 분석 결과
    sentiment_score = Column(Float)      # 감정 점수 (예: 1~10)
    health_flags = Column(JSON)          # 건강 이상 징후 (예: ["기침", "두통"])
    summary = Column(Text)               # 대화 요약본
    danger_level = Column(Integer)       # 위험도 (0: 정상, 1: 주의, 2: 위험)
    
    analyzed_at = Column(DateTime, default=func.now())

class UserVectorMemory(Base):
    """과거 대화 기억 (Vector Memory) - pgvector 사용 전제"""
    __tablename__ = "user_vector_memory"
    
    id = Column(Integer, primary_key=True)
    user_id = Column(String(50), ForeignKey("voice_profiles.user_id"))
    dialog_id = Column(Integer, ForeignKey("dialog_logs.id"))
    
    # 실제 벡터 데이터 (pgvector 설치 시 Vector 타입 사용 가능)
    # 지금은 개념적으로 텍스트와 함께 저장
    content_embedding = Column(JSON) # 임베딩 벡터값
    context_text = Column(Text)      # 벡터화된 원문 (검색용)