# services/radio_service/radio_pipline.py
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager
from database.schema import DailyQuestion , CommunityRadioTopic
from datetime import datetime

class RadioPipeline:
    def __init__(self, llm_service):
        self.llm_service = llm_service
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager()
        # 라디오용 목소리는 고정되어 있으므로 미리 prepare 해둡니다.
        self.radio_host_id = "radio_voice_1" 
        
    

    async def build_daily_radio(self, db_session, target_date):
        
        # 1. 오늘의 질문(주제) 가져오기
        q_info = db_session.query(DailyQuestion).filter(DailyQuestion.id == 1).first()
        # 2. 이관된 라디오 답변들 가져오기
        topics = db_session.query(CommunityRadioTopic).all() # 실제론 날짜 필터링
        if not topics: return "No topics today" # 이건 진짜 ㅈ댔을때
        
        context = "\n".join([f"- {t.user_id}: {t.answer_text}" for t in topics])
        # RadioPipeline 내부에서 처리
        weekday_map = {"Monday": "월요일", "Tuesday": "화요일", "Wednesday": "수요일", 
                    "Thursday": "목요일", "Friday": "금요일", "Saturday": "토요일", "Sunday": "일요일"}
        current_weekday = weekday_map[datetime.now().strftime("%A")]
        formatted_date = datetime.now().strftime(f"%Y년 %m월 %d일, {current_weekday}")

        # 3. 프롬프팅
        prompt = f"""
        너는 혼자 지내시는 70~80대 어르신들을 위한 라디오 DJ '하루'야.  
        외롭고 조용한 하루를 보내시는 어르신들이, 네 목소리를 통해 따뜻한 위로와 공감을 받을 수 있도록 라디오를 진행해줘.

        ---

        🎙️ 방송 구성

        1. **도입 멘트**
        - 현재 날짜 기준으로 “오늘은 1월 18일, 목요일입니다.”처럼 시작해줘. (오늘 날짜: {formatted_date})
        - 계절이나 날씨, 분위기를 짧게 언급하며 다정한 인사를 건네줘.
        - 오늘의 주제 소개: "{q_info.question_content}" (카테고리: {q_info.category})

        2. **사연 소개**
        - 아래 어르신들의 응답 내용을 토대로 사연을 소개해줘:
        
        {context}

        - 사연은 “어떤 분은…”, “한 어르신께서는…”, “또 다른 분은 이렇게 말씀해주셨어요” 같은 자연스러운 표현으로 연결해줘.
        - 각 사연마다 하루 DJ가 따뜻한 리액션이나 공감 멘트를 함께 말해줘. (예: “그런 기억, 참 소중하죠.”, “저도 마음이 따뜻해지네요.” 등)
        - 기계적으로 나열하지 말고, 감정 흐름이 부드럽게 이어지도록 구성해줘.

        3. **마무리 멘트**
        - 오늘 들려드린 이야기들을 되짚으며 따뜻한 감상으로 정리해줘.
        - 하루를 마무리하는 위로의 말 한마디로 마무리하되, 너무 길지 않게.
        - 방송을 끝낼 땐 꼭 아래 문장으로 마무리해줘:

        💬 시그니처 마무리:
        “내일도 하루는 여러분 곁에 있겠습니다. 지금까지 라디오 하루였습니다.”

        ---

        📌 작성 방식 지시
        - 스크립트 전체는 하루 DJ 혼자 이야기하는 **라디오 대본처럼 쭉 이어지도록** 작성해줘.
        - “user”라는 말은 절대 사용하지 말고, 사연 속 인물은 전부 익명성과 따뜻함이 느껴지게 소개해줘.
        - 사연 사이에는 연결 멘트(“또 이런 이야기도 있었어요.”, “한편 이런 추억을 말씀해주신 분도 계셨죠.” 등)를 넣어서 끊김 없는 흐름을 유지해줘.
        """
        # 3. LLM에게 대본 요청 (ask_plain_text 메서드 필요)
        script = await self.llm_service.ask_plain_text(prompt)
        # [수정] TTS 로직 이전에 대본을 먼저 프린트하거나 저장해서 확인합시다.
        print(f"✨ 생성된 대본: \n{script}")

        # (4) TTS 생성 부분 (테스트를 위해 잠시 리턴값을 script로 변경)
        latents = self.latent_manager.get_latent(self.radio_host_id)
        if not latents:
            # 목소리가 준비 안 됐어도, 일단 쓴 대본은 보여달라고 합니다.
            return {
                "message": "Latent not ready, but script generated",
                "script": script
            }
            
        # (4) TTS 생성 (라디오 목소리 .pth 사용)
        # latents = self.latent_manager.get_latent(self.radio_host_id)
        # if not latents:
        #     # S3에서 radio_voice_1_latent.pth 로드 시도
        #     return "Latent not ready"

        # (5) 파일로 저장 후 S3 업로드
        # audio_data = self.tts_service.synthesize(script, latents)
        # s3_url = self.upload_to_s3(audio_data)
        
        return "Radio Generation Success"