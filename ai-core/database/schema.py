import enum
from sqlalchemy import Column, Integer, String, Text, Float, DateTime, JSON, Boolean, Enum as SqlEnum
from sqlalchemy.sql import func
from database.database import Base # database.py의 Base 상속

class StatusEnum(enum.Enum): # 파이썬 enum.Enum 상속
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    READY = "READY"
    FAILED = "FAILED"

class VoiceProfile(Base):
    """자녀 목소리 관리 (학습용)"""
    __tablename__ = "voice_profiles"
    id = Column(Integer, primary_key=True, autoincrement=True) # DB 내부 인덱싱 id ++
    user_id = Column(String(50), unique=True, nullable=False, index=True) # 백엔드와 공유하는 ID
    raw_wav_path = Column(String(255), nullable=False) # 자녀가 올린 원본 파일 경로
    latent_path = Column(String(255)) # XTTS 특징 추출값(.json 또는 .pth) 경로
    status = Column(SqlEnum(StatusEnum), default=StatusEnum.PENDING) 
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now()) # 목소리 등록된 시간 저장 시점

class UserMemory(Base):
    """다음 대화를 위한 요약본 (Gemma-2 주입용)"""
    __tablename__ = "user_memories"
    id = Column(Integer, primary_key=True, autoincrement=True) # DB 내부 인덱싱 id ++
    user_id = Column(String(50), nullable=False, index=True) 
    conversation_id = Column(String(50), unique=True, nullable=False)
    summary_text = Column(Text, nullable=False) # "어르신이 오늘 기분이 좋으셨음"
    created_at = Column(DateTime, default=func.now())

class ConversationAnalysis(Base):
    """보호자용 통화 리포트 (통화 종료 시 1회 저장)"""
    __tablename__ = "conversation_analysis"
    id = Column(Integer, primary_key=True, autoincrement=True)
    conversation_id = Column(String(50), unique=True, nullable=False)  # 대화 id 
    user_id = Column(String(50), nullable=False)
    # 1. 종합 점수 및 위험도
    chi_score = Column(Integer)          # 인지 기능 종합 지수 (예: 85)
    danger_level = Column(Integer)       # 0: 양호, 1: 주의, 2: 위험
    
    # 2. UI의 5대 세부 지표 (0~20점 기준)
    recall_score = Column(Integer)       # 기억 상기율
    coherence_score = Column(Integer)    # 대화 일관성
    orientation_score = Column(Integer)  # 시간 지향성
    stability_score = Column(Integer)    # 감정 안정성
    engagement_score = Column(Integer)   # 참여 적극성
    
    # 3. 질문 내용 및 정답 여부 (JSON 리스트)
    # 예: [{"question": "어제 뭐 드셨나요?", "is_correct": True}, ...]
    question_results = Column(JSON)      
    
    # 4. 분석 결과 텍스트
    summary = Column(Text)               # 보호자에게 보여줄 요약
    health_flags = Column(JSON)          # ["두통", "기침"] 등 이상 징후
    
    analyzed_at = Column(DateTime, default=func.now())

# 라디오 스키마(서준영)
class CommunityRadioTopic(Base):
    """라디오 방송용 공통 질문 및 답변 저장소"""
    __tablename__ = "community_radio_topics"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    topic_id = Column(String(50), index=True)      # 예: "FIRST_SALARY" (첫 월급 주제)
    user_id = Column(String(50), nullable=False)   # 답변한 어르신 ID
    answer_text = Column(Text, nullable=False)     # "나는 정장을 맞췄어" (원본 혹은 정제된 답변)
    is_shared = Column(Boolean, default=False)     # 방송 공유 동의 여부 (개인정보 보호 )
    broadcast_date = Column(DateTime)              # 방송 예정일 (다음날 아침 등)
    created_at = Column(DateTime, default=func.now())
