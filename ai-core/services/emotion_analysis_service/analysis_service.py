import json
from database.schema import ConversationAnalysis , DailyQuestion
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
    
            
    async def get_combined_data_for_script(db_session):
        # 1. 오늘의 질문 가져오기
        daily_q = db_session.query(DailyQuestion).filter(DailyQuestion.id == 1).first()
        
        # 2. 어르신들의 답변 결과들 가져오기
        analyses = db_session.query(ConversationAnalysis).all() # 실제로는 날짜 필터링 필요
        
        collected_answers = []
        for row in analyses:
            if row.question_results:
                # 첫 번째 요소의 today_answer 추출
                answer_text = row.question_results[0].get("today_answer")
                if answer_text:
                    collected_answers.append(f"{row.user_id} 어르신: {answer_text}")
                    
        return daily_q.question_content, collected_answers