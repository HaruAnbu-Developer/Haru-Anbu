# services/radio_service/question_generator.py
import datetime
from database.database import SessionLocal
from database.schema import DailyQuestion, UserMemory , UserMission
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
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
            
            logger.info(f"📅 오늘의 질문 갱신 완료: {response['question']}")
            return response['question']
        except Exception as e:
            db.rollback()
            logger.info(f"오류 발생: {e}")
        finally:
            db.close()
    async def generate_user_missions(self):
        """
        매일 새벽 실행: UserMemory(최근 3개)를 기반으로 개인별 맞춤 질문(UserMission) 생성
        """
        logger.info("🚀 개인별 맞춤 미션 생성 시작 (최근 3개 기억 반영)...")
        db = SessionLocal()
        
        try:
            # 1. 기억(UserMemory)이 존재하는 모든 유저 ID 조회
            distinct_users = db.query(UserMemory.user_id).distinct().all()
            
            count = 0
            for row in distinct_users:
                user_id = row.user_id
                
                # ★ [수정 1] 가장 최신 기억 3개를 가져옵니다. (내림차순 정렬 후 3개 자름)
                recent_memories = db.query(UserMemory).filter(
                    UserMemory.user_id == user_id
                ).order_by(UserMemory.id.desc()).limit(3).all()
                
                if not recent_memories:
                    continue

                # ★ [수정 2] LLM에게는 시간 순서대로(과거 -> 최신) 보여주는 것이 맥락 파악에 좋습니다.
                # db에서 최신순으로 가져왔으므로 reversed()를 사용해 뒤집습니다.
                memory_texts = [f"- {m.summary_text}" for m in reversed(recent_memories)]
                combined_memory_context = "\n".join(memory_texts)

                # 3. 이미 오늘 생성된 미션이 있는지 확인 (중복 생성 방지용, 필요시 추가)
                
                # 4. LLM 프롬프트: 3개의 기억을 바탕으로 안부 질문 생성
                prompt = f"""
                당신은 이 어르신({user_id})의 다정한 손주입니다.
                아래는 어르신과의 최근 대화 기록들입니다. 흐름을 파악하여 자연스럽고 따뜻한 안부 질문을 하나 만들어주세요.
                
                [최근 대화 기록 (과거 -> 현재 순)]
                {combined_memory_context}
                
                [조건]
                1. 최근 대화의 맥락(건강 변화, 기분 변화, 했던 일 등)을 반영한 질문일 것.
                2. 질문은 단 한 문장으로, 50자 이내.
                3. 반드시 JSON 형식으로 출력: {{"category": "health/memory/meal", "question": "..."}}
                """

                try:
                    # 5. LLM 호출
                    response = await self.llm.ask_json(prompt)
                    
                    # 6. UserMission DB 저장
                    new_mission = UserMission(
                        user_id=user_id,
                        mission_text=response['question'],
                        category=response.get('category', 'memory'),
                        is_cleared=False, # 아직 질문 안 함
                        created_at=datetime.datetime.now()
                    )
                    db.add(new_mission)
                    db.commit() 
                    count += 1
                    logger.info(f"✅ {user_id} 미션 생성: {response['question']}")

                except Exception as e:
                    logger.error(f"❌ {user_id} 미션 생성 실패: {e}")
                    db.rollback()
                    continue
            
            logger.info(f"🎉 총 {count}명의 맞춤 미션 생성 완료")

        except Exception as e:
            logger.error(f"🔥 미션 생성 배치 실패: {e}")
        finally:
            db.close()