import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from fastapi import FastAPI, BackgroundTasks, Depends
from sqlalchemy.orm import Session
from services.voice_training_service.voice_processor import VoiceProcessor
from database.database import get_db

# 1. FastAPI 앱 객체 생성 (이게 빠져있었습니다!)
app = FastAPI()
voice_processor = VoiceProcessor() # 전용 객체 생성

@app.post("/voice/register/{user_id}")
async def register_voice(
    user_id: str, 
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db)
):
    # 1. DB에서 raw_wav_path 확인
    # 2. 백그라운드 작업 예약 (즉시 return 가능하게 함)
    background_tasks.add_task(voice_processor.extract_and_save_latent, db, user_id)
    
    return {"message": "목소리 분석을 시작합니다. 완료 후 상태가 업데이트됩니다."}