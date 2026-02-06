#server/connect_back/controller.py
import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from fastapi import FastAPI, BackgroundTasks, Depends, APIRouter, HTTPException, UploadFile, File
from services.voice_training_service.voice_processor import VoiceProcessor
from services.voice_training_service.latent_manager import get_latent_manager
from database.schema import VoiceProfile, StatusEnum 
from database.database import get_db ,SessionLocal
from sqlalchemy.orm import Session
from services.radio_service.question_generator import QuestionGenerator
from services.llm.llm_service_Gemma_stream import get_llm_service
from services.radio_service.radio_pipeline import RadioPipeline
from services.radio_service.question_generator import QuestionGenerator
from services.radio_service.merge_daily_answer import MergeDailyAnswer
from datetime import datetime, date, timedelta

app = FastAPI()
voice_processor = VoiceProcessor() # 전용 객체 생성
latent_manager = get_latent_manager()

@app.post("/voice/upload/{user_id}")
async def upload_voice(user_id: str, file: UploadFile = File(...)):
    """
    사용자의 목소리 파일을 업로드합니다.
    기존 파일이 있다면 덮어씌워집니다 (user_id 하나당 하나의 목소리 파일).
    """
    try:
        # processor의 업로드 메서드 호출
        s3_path = await voice_processor.upload_raw_voice(user_id, file.file, file.filename)
        
        return {
            "status": "success", 
            "message": "목소리 파일이 업로드되었습니다.",
            "path": s3_path
        }
    except Exception as e:
        return {"status": "error", "message": f"업로드 실패: {str(e)}"}
    
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

@app.post("/force-midnight-job")
async def force_midnight_job(db: Session = Depends(get_db)):
    try:
        print("🚀 [테스트] 자정 통합 작업 강제 시작...")
        llm = get_llm_service()
        
        # 테스트를 위해 오늘 날짜 설정
        today = datetime.now().date()
        
        # 1. 데이터 이관 (오늘 날짜 데이터를 강제로 이관하도록 수정 가능)
        sg = MergeDailyAnswer(llm)
        await sg.migrate_daily_answers_to_radio_topics(db) # 내부에서 yesterday를 오늘로 잠시 수정해서 테스트하세요!
        
        # 2. 라디오 대본 생성 (target_date 인자 추가)
        rp = RadioPipeline(llm)
        # 여기서 today를 넘겨줍니다.
        script = await rp.build_daily_radio(db, target_date=today) 
        
        # 3. 새로운 질문 생성
        qg = QuestionGenerator(llm)
        new_question = await qg.generate_and_save_daily_question()
        
        return {
            "status": "success",
            "script": script,
            "new_question": new_question
        }
        
    except Exception as e:
        print(f"❌ 테스트 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))