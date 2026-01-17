import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from fastapi import FastAPI, BackgroundTasks, Depends
from services.voice_training_service.voice_processor import VoiceProcessor
from services.voice_training_service.latent_manager import get_latent_manager
from database.schema import VoiceProfile, StatusEnum 
from database.database import get_db ,SessionLocal
from sqlalchemy.orm import Session
from services.radio_service.question_generator import QuestionGenerator
from services.llm.llm_service_Gemma_stream import get_llm_service

app = FastAPI()
voice_processor = VoiceProcessor() # 전용 객체 생성
latent_manager = get_latent_manager()
@app.post("/voice/register/{user_id}")
async def register_voice(
    user_id: str, 
    background_tasks: BackgroundTasks
):
    # 1. 백그라운드 작업 예약 (즉시 return 가능하게 함)
    background_tasks.add_task(voice_processor.extract_and_save_latent, user_id)
    
    return {"message": "목소리 분석을 시작합니다. 완료 후 상태가 업데이트됩니다. PENDING -> READY"}


@app.post("/voice/prepare/{user_id}")
async def prepare_voice(user_id: str, db: Session = Depends(get_db)):
    try:
        # 1. DB 조회 (status와 latent_path를 함께 확인)
        profile = db.query(VoiceProfile).filter(VoiceProfile.user_id == user_id).first()
        
        # 2. 예외 케이스 처리
        if not profile:
            return {"status": "error", "message": "등록된 목소리 프로필이 없습니다."}
        
        if profile.status == StatusEnum.PENDING:
            return {"status": "busy", "message": "현재 목소리 분석이 진행 중입니다. 잠시 후 다시 시도해주세요."}
            
        if profile.status == StatusEnum.FAILED:
            return {"status": "error", "message": "목소리 분석에 실패한 프로필입니다. 재등록이 필요합니다."}
            
        if profile.status != StatusEnum.READY or not profile.latent_path:
            return {"status": "error", "message": "목소리 복제가 완료되지 않았습니다."}

        # 3. 모든 검증 통과 시 메모리에 로드
        success = latent_manager.prepare_user(user_id, profile.latent_path)
        
        if success:
            return {"status": "success", "message": "자녀 목소리 준비 완료"}
        else:
            return {"status": "failed", "message": "S3 데이터 로드 중 오류가 발생했습니다."}
            
    except Exception as e:
        return {"status": "error", "message": str(e)}
    finally:
        db.close()

@app.post("/voice/release/{user_id}")
async def release_voice(user_id: str):
    # 4. 통화 종료 후 메모리 반환
    latent_manager.release_user(user_id)
    return {"status": "released"}

@app.post("/test/daily-question")
async def test_question():
    llm_service = get_llm_service()
    
    generator = QuestionGenerator(llm_service) # 이미 생성된 llm_service 주입
    question = await generator.generate_and_save_daily_question()
    return {"today_question": question}