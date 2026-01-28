# services/llm/conversation_manager.py
from datetime import datetime, date
import logging
import json 
from database.database import SessionLocal 
from database.schema import UserMission, ConversationAnalysis # 스키마 추가 임포트  # 위에서 만든 스키마

logger = logging.getLogger(__name__)

class ConversationManager:
    def __init__(self, user_id: str):
        self.user_id = user_id
        
        # 1. DB에서 오늘의 미션 로드
        self.checklist = self._load_missions()
        self.current_step = 0 
        self.conversation_log = [] 

    def _load_missions(self):
        """DB에서 해당 유저의 미완료 미션을 가져옵니다. DB to memory"""
        db = SessionLocal()
        try:
            # 오늘 생성된, 아직 안 끝난 미션 조회
            missions = db.query(UserMission).filter(
                UserMission.user_id == self.user_id,
                UserMission.is_cleared == False,
                # 실제 운영시엔 날짜 필터도 필요 (오늘 날짜)
                # func.date(UserMission.created_at) == date.today() 
            ).all()

            checklist = []
            
            # 1. DB에 미션이 있으면 추가
            for m in missions:
                checklist.append({
                    "db_id": m.id,
                    "type": m.category,
                    "question": m.mission_text,
                    "answered": False,
                    "user_answer": ""
                })
                
            # 2. (안전장치) DB에 미션이 하나도 없으면 기본 질문 추가
            if not checklist:
                checklist.append({
                    "db_id": None,
                    "type": "default_health",
                    "question": "오늘 컨디션은 좀 어떠세요? 어디 불편한 곳은 없으시고요?",
                    "answered": False,
                    "user_answer": ""
                })
                
            return checklist
        finally: db.close()

    def record_user_input(self, text):
        self.conversation_log.append(f"User: {text}")
        
        # 메모리 상에서만 상태 업데이트 (DB 접근 X)
        if self.current_step < len(self.checklist):
            if len(text) > 2: # 너무 짧은 대답은 무시
                self.checklist[self.current_step]['user_answer'] = text
                self.checklist[self.current_step]['answered'] = True

    def record_ai_response(self, text):
        self.conversation_log.append(f"AI: {text}")

    def get_system_instruction(self):
        # 1. 모든 필수 질문 완료 시
        if self.current_step >= len(self.checklist):
            return "필수 질문은 끝났습니다. 이제 편안한 자식처럼 어르신의 말에 귀 기울이고 자연스럽게 대화하세요."

        current_q = self.checklist[self.current_step]
        
       # 1. 이미 답변 완료된 질문이면 -> 다음 질문으로 넘어가기
        if current_q['answered']:
            self.current_step += 1
            if self.current_step >= len(self.checklist):
                return "모든 질문이 끝났습니다.자연스럽게 공감하며 한두문장으로 대화를 이어가세요."
            current_q = self.checklist[self.current_step]
            
            # 새로운 질문 시작할 때
            return (
                f"당신은 자식입니다. 앞선 대화 내용을 자연스럽게 마무리하고, "
                f"이어서 이 질문을 던져보세요: '{current_q['question']}'"
            )

        # 2. 아직 답변 안 된 상태 (또는 답변이 너무 짧아서 재질문 필요한 상태)
        # 여기서 매번 "질문하세요!"라고 강요하면 부자연스러움.
        # "지금 대화 주제가 이 질문과 관련 없으면 자연스럽게 유도하고, 관련 있으면 깊게 물어봐" 정도가 좋음
        return (
            f"당신은 자식입니다. 어르신과 대화 중입니다. "
            f"현재 목표는 '{current_q['question']}'에 대한 답을 듣는 것입니다. "
            f"너무 급하지 않게, 자연스러운 흐름 속에서 이 질문을 꺼내보세요."
        )

    def get_analysis_data(self):
        return {
            "user_id": self.user_id,
            "checklist_results": self.checklist,
            "full_log": "\n".join(self.conversation_log)
        }
        