from fastapi import BackgroundTasks, Depends
from services.voice_processor import VoiceProcessor

voice_processor = VoiceProcessor() # 전용 객체 생성

@app.post("/voice/register/{user_id}")
async def register_voice(
    user_id: str, 
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db)
):
    # 1. DB에서 raw_wav_path 확인
    # 2. 백그라운드 작업 예약 (즉시 return 가능하게 함)
    background_tasks.add_task(voice_processor.process_user_voice, db, user_id)
    
    return {"message": "목소리 분석을 시작합니다. 완료 후 상태가 업데이트됩니다."}