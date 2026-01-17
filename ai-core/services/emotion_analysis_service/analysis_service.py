import jason
from database.schema import ConversationAnalysis, DailyRadioContent
from database.database import SessionLocal


class AnalysisService:
    def __init__(self, llm_service):
        self.llm = llm_service

    async def analyze_conversation(self, conversation_history: list):
        # 1. 메모리에 쌓인 리스트를 텍스트로 변환
        dialog_text = "\n".join(conversation_history)
        
        # 2. Gemma-2에게 분석 요청 (JSON 응답 유도)
        prompt = self.make_analysis_prompt(dialog_text)
        raw_response = await self.llm.generate(prompt)
        
        # 3. JSON 파싱
        analysis = json.loads(raw_response)
        return analysis

    def save_to_db(self, db, user_id, conv_id, analysis):
        # UserMemory 저장 (다음 통화를 위한 기억)
        memory = UserMemory(
            user_id=user_id,
            conversation_id=conv_id,
            summary_text=analysis['short_memory']
        )
        
        # ConversationAnalysis 저장 (보호자 리포트)
        report = ConversationAnalysis(
            user_id=user_id,
            conversation_id=conv_id,
            sentiment_score=analysis['sentiment'],
            summary_detail=analysis['full_summary'],
            health_flags=analysis['health_issues'],
            danger_level=analysis['danger_level']
        )
        
        db.add(memory)
        db.add(report)
        db.commit()
    
    async def process_radio_data(analysis_data: ConversationAnalysis):
        db = SessionLocal()
        try:
            # 1. question_results의 첫 번째 질문(공통질문)을 가져옴
            if analysis_data.question_results and len(analysis_data.question_results) > 0:
                common_q_result = analysis_data.question_results[0]
                
                # 2. DailyRadiocontet 테이블에 데이터 복사/삽입
                new_radio_content = DailyRadioContent(
                    user_id=analysis_data.user_id,
                    target_date=analysis_data.analyzed_at.date(),
                    question=common_q_result.get("question"),
                    answer=common_q_result.get("user_answer") # <-- 이 값이 핵심!
                )
                db.add(new_radio_content)
                db.commit()
        finally:
            db.close()