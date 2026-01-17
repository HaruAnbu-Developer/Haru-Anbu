# services/llm/conversation_manager.py
from database.schema import DailyQuestion, UserMemory


class ConversationManager:
    def __init__(self, user_id, db_session):
        self.user_id = user_id
        self.db = db_session
        # 1. 필수 질문 큐 생성
        self.question_queue = self._prepare_questions()
        self.asked_count = 0

    def _prepare_questions(self):
        # (1) 오늘의 라디오 질문 (DailyQuestion id=1)
        dq = self.db.query(DailyQuestion).filter(DailyQuestion.id == 1).first()
        # (2) 이전 대화 요약 기반 추적 질문 (UserMemory)
        memo = self.db.query(UserMemory).filter(UserMemory.user_id == self.user_id).first()
        
        queue = []
        if dq: queue.append({"type": "RADIO", "q": dq.question_content})
        queue.append({"type": "HEALTH", "q": "어르신, 오늘 어디 아프신 곳은 없으세요?"})
        if memo: queue.append({"type": "MEMORY", "q": f"저번에 말씀하신 {memo.summary_text[:10]}... 그건 좀 어떠세요?"})
        
        return queue

    def get_next_instruction(self):
        """LLM에게 이번 턴에 무엇을 할지 지시사항 전달"""
        if self.asked_count < len(self.question_queue):
            next_q = self.question_queue[self.asked_count]
            self.asked_count += 1
            return f"사용자의 말에 공감한 뒤, 자연스럽게 다음 질문을 던지세요: '{next_q['q']}'"
        return "사용자와 자유롭게 다정한 대화를 이어가세요."