import logging
from datetime import datetime
from sqlalchemy.orm import Session
from database.database import SessionLocal
from database.schema import DailyQuestion, CommunityRadioTopic

logger = logging.getLogger(__name__)

class RadioService:
    def __init__(self, db: Session = None):
        self.db = db if db else SessionLocal()

    def get_today_question(self):
        """오늘의 질문과 Topic ID를 반환"""
        today_str = datetime.now().strftime("%Y-%m-%d")
        return self.get_question_by_date(today_str)

    def get_question_by_date(self, date_str: str):
        """특정 날짜의 질문 조회"""
        try:
            question = self.db.query(DailyQuestion).filter(DailyQuestion.target_date == date_str).first()
            if question:
                return question.content, question.topic_id
            return None, None
        except Exception as e:
            logger.error(f"Error fetching question for {date_str}: {e}")
            return None, None

    def has_answered(self, user_id: str, topic_id: str) -> bool:
        """사용자가 해당 주제에 대해 이미 답변했는지 확인"""
        if not topic_id:
            return False
            
        try:
            exists = self.db.query(CommunityRadioTopic).filter(
                CommunityRadioTopic.user_id == user_id,
                CommunityRadioTopic.topic_id == topic_id
            ).first()
            return exists is not None
        except Exception as e:
            logger.error(f"Error checking answer status: {e}")
            return False

    def save_answer(self, user_id: str, topic_id: str, answer_text: str, is_shared: bool = True):
        """사용자 답변 저장"""
        try:
            # 중복 체크
            if self.has_answered(user_id, topic_id):
                logger.info(f"User {user_id} already answered topic {topic_id}")
                return

            new_story = CommunityRadioTopic(
                topic_id=topic_id,
                user_id=user_id,
                answer_text=answer_text,
                is_shared=is_shared,
                broadcast_date=None  # 추후 로직으로 설정
            )
            self.db.add(new_story)
            self.db.commit()
            logger.info(f"Saved answer for user {user_id}, topic {topic_id}")
        except Exception as e:
            self.db.rollback()
            logger.error(f"Error saving answer: {e}")
            raise e

    def get_answers_by_topic(self, topic_id: str):
        """해당 주제의 모든 답변 조회 (공유 동의한 것만)"""
        try:
            return self.db.query(CommunityRadioTopic).filter(
                CommunityRadioTopic.topic_id == topic_id,
                CommunityRadioTopic.is_shared == True
            ).all()
        except Exception as e:
            logger.error(f"Error fetching answers for topic {topic_id}: {e}")
            return []

    def close(self):
        self.db.close()
