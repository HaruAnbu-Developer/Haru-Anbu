from datetime import datetime, date
import logging
import json

from database.database import SessionLocal 
from database.schema import UserMission, UserMemory

logger = logging.getLogger(__name__)

class ConversationManager:
    def __init__(self, user_id: str):
        self.user_id = user_id
        
        # 1. DB에서 오늘의 미션 로드
        self.checklist = self._load_missions()
        self.current_step = 0 
        self.conversation_log = []

        # 오프닝 상태 플래그
        self.in_opening_sequence = True

    def _load_missions(self):
        """DB에서 해당 유저의 미완료 미션을 가져옵니다."""
        db = SessionLocal()
        try:
            missions = db.query(UserMission).filter(
                UserMission.user_id == self.user_id,
                UserMission.is_cleared == False,
            ).all()

            checklist = []
            if missions:
                for m in missions:
                    checklist.append({
                        "db_id": m.id,
                        "type": m.category,
                        "question": m.mission_text,
                        "answered": False,
                        "user_answer": ""
                    })
            else:
                # 미션 없으면 기본 미션 생성
                default_mission = UserMission(
                    user_id=self.user_id,
                    mission_text="오늘 컨디션은 좀 어떠세요? 어디 불편한 곳은 없으시고요?",
                    category="default_health",
                    is_cleared=False
                )
                db.add(default_mission)
                db.commit()
                db.refresh(default_mission)

                checklist.append({
                    "db_id": default_mission.id,
                    "type": default_mission.category,
                    "question": default_mission.mission_text,
                    "answered": False,
                    "user_answer": ""
                })
            return checklist
        finally: db.close()
        
    def get_opening_remark(self):
        """통화 시작 시 첫 인사말 생성"""
        db = SessionLocal()
        try:
            last_memory = db.query(UserMemory).filter(
                UserMemory.user_id == self.user_id
            ).order_by(UserMemory.id.desc()).first()
            
            if last_memory and last_memory.summary_text:
                return f"안녕하세요~ 지난번에 {last_memory.summary_text}라고 하셨는데, 오늘은 좀 어떠세요?"
            
            hour = datetime.now().hour
            if 6 <= hour < 11: return "안녕하세요 어르신! 상쾌한 아침이에요. 아침 식사는 하셨나요?"
            elif 11 <= hour < 14: return "안녕하세요 어르신! 점심 식사는 맛있게 하셨나요?"
            elif 17 <= hour < 21: return "안녕하세요 어르신! 저녁 시간 다 되었네요. 오늘 하루 어떠셨어요?"
            else: return "안녕하세요 어르신! 밤이 늦었네요. 별일 없으신가요?"
        except Exception as e:
            logger.error(f"오프닝 멘트 생성 실패: {e}")
            return "여보세요~ 어르신! 오늘 하루는 어떠셨나요?"
        finally:
            db.close()
                    
    def record_user_input(self, text):
        self.conversation_log.append(f"User: {text}")
        
    def record_ai_response(self, text):
        self.conversation_log.append(f"AI: {text}")
        
    # _check_answer_relevance 삭제됨 (이제 안 씀)

    def get_system_instruction(self):
        """LLM에게 현재 상황에 맞는 지시사항 제공"""
        
        # 1. 모든 미션 완료 시 -> 자유 대화 모드
        if self.current_step >= len(self.checklist):
            return "자연스럽게 공감하며 한 문장으로 대화를 이어나가세요. 질문은 한 번에 하나만 하세요."

        current_q = self.checklist[self.current_step]
    
        # 2. 오프닝 상태이거나, 방금 질문이 완료되었을 때 -> 다음 질문 던지기
        # (이 경우엔 판단할 필요 없이 질문만 던지면 됨)
        if self.in_opening_sequence or current_q['answered']:
            # 오프닝 플래그 해제
            if self.in_opening_sequence:
                self.in_opening_sequence = False
            
            # 이전 질문이 완료되었다면 다음 스텝으로 이동
            if current_q['answered']:
                self.current_step += 1
                if self.current_step >= len(self.checklist):
                    return "모든 필수 질문이 끝났습니다. 자연스럽게 공감하며 한 문장으로 대화를 이어나가세요."
                current_q = self.checklist[self.current_step]

            # ★ [중요] 새로운 질문을 시작하라는 지시 (태깅 불필요)
            return (
                f"당신은 자식입니다. 앞선 대화 내용을 자연스럽게 마무리하고, "
                f"이어서 이 질문을 던져보세요: '{current_q['question']}'"
            )

        # 3. 질문을 던진 후 답변을 기다리는 상태 -> 태깅([1]) 요구
        # (오프닝도 아니고, answered도 False인 상태)
        last_user_msg = self.conversation_log[-1] if self.conversation_log else ""
        return (
            f"당신은 자식입니다. 방금 어르신께 '{current_q['question']}'라고 물어봤습니다.\n"
            f"지금 사용자의 말('{last_user_msg}')이 이 질문에 대한 **적절한 대답**이라면, "
            f"반드시 답변 맨 앞에 '[1]'를 붙여서 말씀하세요.\n"
            f"만약 동문서답하거나 대답을 회피했다면, 태그 없이 다시 자연스럽게 되물어보세요.\n"
            f"예시(성공): [1] 아, 김치찌개 드셨군요! 맛있으셨어요?\n"
            f"예시(실패): 아, 날씨 이야기도 좋지만 식사는 하셨나요?"
        )

    def get_analysis_data(self):
        return {
            "user_id": self.user_id,
            "checklist_results": self.checklist,
            "full_log": "\n".join(self.conversation_log)
        }