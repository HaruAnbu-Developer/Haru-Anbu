

# 서비스 클래스 내부 가상 코드
class ConversationSession:
    def __init__(self, user_id, conversation_id):
        self.user_id = user_id
        self.conversation_id = conversation_id
        self.history = []  # 인메모리 저장소

    def add_dialog(self, role, content):
        self.history.append(f"{role}: {content}")
        # 여기서 실시간 SOS 체크 로직만 따로 실행 (DB 저장 없이)
        if "살려줘" in content or "아프다" in content:
            self.trigger_emergency_alert()

    async def finalize(self, db_session):
        # 1. 메모리에 쌓인 대화 합치기
        full_text = "\n".join(self.history)
        
        # 2. Gemma-2에게 한 번만 요청해서 요약/분석 결과 받기
        analysis_result = await gemma_service.analyze_all(full_text)
        
        # 3. 요약 저장 (UserMemory)
        db_session.add(UserMemory(
            user_id=self.user_id,
            conversation_id=self.conversation_id,
            summary_text=analysis_result['short_summary']
        ))
        
        # 4. 분석 리포트 저장 (ConversationAnalysis)
        db_session.add(ConversationAnalysis(
            user_id=self.user_id,
            conversation_id=self.conversation_id,
            sentiment_score=analysis_result['sentiment'],
            health_flags=analysis_result['flags']
        ))
        db_session.commit()