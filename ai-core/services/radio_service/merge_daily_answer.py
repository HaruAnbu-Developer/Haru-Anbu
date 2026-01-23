# services/radio_srvice/scripts_generator.py
from database.schema import ConversationAnalysis, CommunityRadioTopic
from datetime import datetime, timedelta
from sqlalchemy.sql import func

class MergeDailyAnswer:
    def __init__(self,llm_service=None):
        self.llm = llm_service
    
    #어제 날짜의 모든 분석 결과 갖고와서 라디오에 쓰일 재로 모으기(이관)
    # question_generator.py보다 먼저 불려야함.
    async def migrate_daily_answers_to_radio_topics(self,db_session):
    # 1. 어제 날짜의 모든 분석 결과 가져오기
        yesterday = datetime.now().date() - timedelta(days=1)
        daily_results = db_session.query(ConversationAnalysis).filter(
            func.date(ConversationAnalysis.analyzed_at) == yesterday
        ).all()

        for result in daily_results:
            # 2. 라디오용 전용 테이블에 삽입
            new_topic = CommunityRadioTopic(
                user_id=result.user_id,
                answer_text=result.daily_answer,  # 핵심 재료
                created_at=result.analyzed_at
            )
            db_session.add(new_topic)
        
        db_session.commit()
        print(f"✅ {len(daily_results)}건의 답변이 라디오 저장소로 이관되었습니다.")
 
        

