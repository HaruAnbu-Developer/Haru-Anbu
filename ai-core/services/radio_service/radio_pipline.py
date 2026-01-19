# services/radio_service/radio_pipline.py
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager
class RadioPipeline:
    def __init__(self):
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager()
        # 라디오용 목소리는 고정되어 있으므로 미리 prepare 해둡니다.
        self.radio_host_id = "radio_voice_1" 

    async def build_daily_radio(self, db_session, target_date):
        """1. 해당 날짜의 모든 유저 답변을 가져와서 대본 생성 및 TTS 실행"""
        
        # (1) DB에서 해당 날짜의 공통 답변들 리스트업
        answers = db_session.query(DailyRadioContent).filter(
            DailyRadioContent.target_date == target_date
        ).all()
        
        # (2) LLM에게 전달할 텍스트 구성
        # "오늘은 OO님이 김치찌개를 드셨다네요. OO님은 산책이 즐거우셨대요." 등
        context = self._format_answers_for_llm(answers)
        
        # (3) LLM 호출하여 '라디오 대본' 생성
        # script = await self.llm_service.generate_radio_script(context)
        script = "안녕하세요, 오늘의 라디오입니다. 오늘 많은 분들이..." # 예시

        # (4) TTS 생성 (라디오 목소리 .pth 사용)
        latents = self.latent_manager.get_latent(self.radio_host_id)
        if not latents:
            # S3에서 radio_voice_1_latent.pth 로드 시도
            return "Latent not ready"

        # (5) 파일로 저장 후 S3 업로드
        # audio_data = self.tts_service.synthesize(script, latents)
        # s3_url = self.upload_to_s3(audio_data)
        
        return "Radio Generation Success"