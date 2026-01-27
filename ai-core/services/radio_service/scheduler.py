#services/radio_service/scheduler.py
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from database.database import SessionLocal
from services.radio_service.merge_daily_answer import MergeDailyAnswer
from services.radio_service.radio_pipeline import RadioPipeline
from services.radio_service.question_generator import QuestionGenerator
from services.llm.llm_service_Gemma_stream import get_llm_service
from datetime import date

scheduler = AsyncIOScheduler()

async def midnight_job():
    print("🌙 자정 스케줄러 작동 시작...")
    db = SessionLocal()
    llm = get_llm_service()
    today = date.today()
    try:
        # 1단계: 어제 답변 이관 (MergeDaliyAnswer)
        sg = MergeDailyAnswer(llm)
        await sg.migrate_daily_answers_to_radio_topics(db)
        
        # 2단계: 오늘 방송할 라디오 대본 생성 (RadioPipeline)
        rp = RadioPipeline(llm) # LLM 주입 필요
        await rp.build_daily_radio(db,today)
        
        # 3단계: 오늘 낮에 물어볼 새로운 질문 생성 (QuestionGenerator)
        qg = QuestionGenerator(llm)
        await qg.generate_and_save_daily_question()
        
        print("✅ 모든 자정 작업이 완료되었습니다.")
    except Exception as e:
        print(f"❌ 자정 작업 중 오류 발생: {e}")
    finally:
        db.close()

# 매일 00:00에 실행
scheduler.add_job(midnight_job, 'cron', hour=0, minute=0)