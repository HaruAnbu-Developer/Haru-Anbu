# services/radio_service/question_generator.py
import datetime
from database.database import SessionLocal
from database.schema import DailyQuestion

class QuestionGenerator:
    def __init__(self, llm_service):
        self.llm = llm_service

    async def generate_and_save_daily_question(self):
        """매일 자정 호출: 오늘의 공통 질문 생성 및 DB 저장"""
        db = SessionLocal()
        try:
            # 1. 기존 질문 확인 (단일 레코드이므로 id=1로 조회)
            existing_q = db.query(DailyQuestion).filter(DailyQuestion.id == 1).first()
            if existing_q and existing_q.updated_at.date() == datetime.date.today():
                return existing_q.question_content
            
            # 1. LLM 프롬프팅
            prompt = """
            너는 독거 어르신들을 위한 다정한 라디오 DJ이자 심리 상담사야.
            어르신들이 통화 중에 대답하기 쉽고, 나중에 라디오 방송에서 
            '다른 분들은 이렇게 지내셨군요'라고 소개하기 좋은 오늘의 공통 질문을 하나 만들어줘.
            
            조건:
            1. 계절이나 날씨, 혹은 일상적인 소재(식사, 젊었을 적 추억, 꿈 등)일 것.
            2. 답변이 단답형이 아닌 한 문장 정도로 나올 수 있는 질문일 것.
            3. 형식: {"category": "식사", "question": "오늘 점심에는 어떤 맛있는 음식을 드셨나요?"}
            4. 질문은 50자 안으로 생성할것.
            """
        
            # 2. LLM 호출 (JSON 파싱 로직 포함)
            response = await self.llm.ask_json(prompt)
            
            # 3. DB 저장 (merge를 사용하면 id=1이 있을 땐 Update, 없을 땐 Insert 합니다)
            new_q = DailyQuestion(
                id=1,
                question_content=response['question'],
                category=response['category']
            )
            
            db.merge(new_q) # 핵심: add 대신 merge 사용
            db.commit()
            
            print(f"📅 오늘의 질문 갱신 완료: {response['question']}")
            return response['question']
        except Exception as e:
            db.rollback()
            print(f"오류 발생: {e}")
        finally:
            db.close()