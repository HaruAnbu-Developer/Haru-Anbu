import sys
import os
import logging
from datetime import datetime

# ai-core 경로 추가
sys.path.append(os.path.join(os.getcwd(), 'ai-core'))

from services.radio.radio_service import RadioService
from database.schema import DailyQuestion, CommunityRadioTopic
from database.database import SessionLocal, init_db

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("TestRadio")

def test_radio_flow():
    db = SessionLocal()
    
    try:
        # 1. DB 초기화 (테이블 생성 등)
        # init_db() # 기존 스크립트 활용
        
        # 2. 오늘 날짜의 질문 생성
        today = datetime.now().strftime("%Y-%m-%d")
        topic_id = "TEST_TOPIC_001"
        
        # 기존 데이터 정리
        db.query(DailyQuestion).filter(DailyQuestion.target_date == today).delete()
        db.query(CommunityRadioTopic).filter(CommunityRadioTopic.topic_id == topic_id).delete()
        db.commit()

        # 새 질문 추가
        new_q = DailyQuestion(
            content="오늘 점심 뭐 드셨어요?",
            target_date=today,
            topic_id=topic_id
        )
        db.add(new_q)
        db.commit()
        logger.info(f"Created DailyQuestion for {today}")

        # 3. RadioService 테스트
        service = RadioService(db)
        
        # 3-1. 질문 조회
        q_content, q_topic = service.get_today_question()
        assert q_content == "오늘 점심 뭐 드셨어요?"
        assert q_topic == topic_id
        logger.info("Passed: get_today_question")

        # 3-2. 답변 확인 (아직 없음)
        user_id = "user_123"
        assert service.has_answered(user_id, topic_id) == False
        logger.info("Passed: has_answered (False)")

        # 3-3. 답변 저장
        service.save_answer(user_id, topic_id, "김치찌개 먹었어.", is_shared=True)
        assert service.has_answered(user_id, topic_id) == True
        logger.info("Passed: save_answer & has_answered (True)")

        # 3-4. 답변 목록 조회 (스크립트 생성용)
        answers = service.get_answers_by_topic(topic_id)
        assert len(answers) == 1
        assert answers[0].answer_text == "김치찌개 먹었어."
        logger.info(f"Passed: get_answers_by_topic ({len(answers)} answers)")

        print("✅ Radio Service Logic Verification SUCCESS!")

    except Exception as e:
        print(f"❌ Verification FAILED: {e}")
        db.rollback()
    finally:
        # 테스트 데이터 정리 (선택사항)
        # db.query(DailyQuestion).filter(DailyQuestion.target_date == today).delete()
        # db.query(CommunityRadioTopic).filter(CommunityRadioTopic.topic_id == topic_id).delete()
        # db.commit()
        db.close()

if __name__ == "__main__":
    test_radio_flow()
