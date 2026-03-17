# services/radio_service/radio_pipeline.py
import os
import io
import boto3
import uuid
from datetime import datetime
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager
from database.schema import DailyQuestion, CommunityRadioTopic

class RadioPipeline:
    def __init__(self, llm_service):
        self.llm_service = llm_service
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager()
        
        # 라디오 DJ '하루'의 목소리 ID (DB/S3에 이 ID로 학습된 데이터가 있어야 함)
        self.radio_host_id = "radio_voice_1" 
        
        # S3 클라이언트 설정
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.getenv('S3_ACCESS_KEY_ID'),
            aws_secret_access_key=os.getenv('S3_SECRET_ACCESS_KEY'),
            region_name=os.getenv('S3_REGION', 'ap-northeast-2')
        )
        self.bucket_name = os.getenv('S3_BUCKET_NAME')

    async def build_daily_radio(self, db_session, target_date):
        print(f"📻 [Radio] {target_date} 라디오 방송 생성 시작...")

        # 1. 오늘의 질문(주제) 가져오기
        q_info = db_session.query(DailyQuestion).filter(DailyQuestion.updated_at == target_date).first()
        if not q_info:
            # 테스트용 fallback: 없으면 ID 1번 가져오기
            q_info = db_session.query(DailyQuestion).filter(DailyQuestion.id == 1).first()

        # 2. 이관된 라디오 답변들 가져오기
        # 실제 로직: CommunityRadioTopic에서 해당 날짜(혹은 어제)의 베스트 답변 조회
        topics = db_session.query(CommunityRadioTopic).limit(5).all() 
        if not topics: 
            print("⚠️ 라디오 주제가 없습니다.")
            return None
        
        context = "\n".join([f"- 사연: {t.answer_text}" for t in topics])
        
        # 날짜 포맷팅
        weekday_map = {"Monday": "월요일", "Tuesday": "화요일", "Wednesday": "수요일", 
                    "Thursday": "목요일", "Friday": "금요일", "Saturday": "토요일", "Sunday": "일요일"}
        current_weekday = weekday_map[datetime.now().strftime("%A")]
        formatted_date = datetime.now().strftime(f"%Y년 %m월 %d일, {current_weekday}")

        # 3. 프롬프팅 (LLM 대본 생성)
        prompt = f"""
        너는 혼자 지내시는 70~80대 어르신들을 위한 라디오 DJ '하루'야.  
        외롭고 조용한 하루를 보내시는 어르신들이, 네 목소리를 통해 따뜻한 위로와 공감을 받을 수 있도록 라디오를 진행해줘.

        ---
        🎙️ 방송 구성
        1. **도입 멘트**: 날짜({formatted_date})와 날씨/계절 인사. 오늘의 주제: "{q_info.question_content}"
        2. **사연 소개**: 
        {context}
        (각 사연을 자연스럽게 연결하고 따뜻한 공감 멘트 추가)
        3. **마무리 멘트**: 따뜻한 끝인사.
        💬 시그니처 엔딩: "내일도 하루는 여러분 곁에 있겠습니다. 지금까지 라디오 하루였습니다."
        
        📌 작성 지시:
        - DJ 혼자 진행하는 자연스러운 구어체 대본.
        - 지문(괄호)이나 "User:" 같은 표시 없이 오직 대사만 출력할 것.
        """
        
        # LLM에게 대본 요청
        script = await self.llm_service.ask_plain_text(prompt)
        print(f"📜 [Radio] 생성된 대본:\n{script[:100]}... (생략)")

        # 4. Latent 준비 (라디오 DJ 목소리)
        # LatentManager를 통해 S3에서 다운로드 및 로드
        # 주의: radio_voice_1_latent.pth가 S3의 latents/radio_voice_1/ 경로에 있어야 함
        s3_key = f"latents/{self.radio_host_id}/{self.radio_host_id}_latent.pth"
        success = self.latent_manager.prepare_user(self.radio_host_id, s3_key)
        
        if not success:
            print(f"❌ [Radio] DJ 목소리({self.radio_host_id})를 로드할 수 없습니다.")
            return {"status": "failed", "reason": "Latent load failed", "script": script}

        latents = self.latent_manager.get_latent(self.radio_host_id)

        # 5. TTS 합성 (전체 스크립트 -> 오디오 변환)
        print("🎙️ [Radio] 오디오 합성 중... (시간이 걸릴 수 있습니다)")
        # 라디오는 천천히 또박또박 읽는 것이 좋으므로 speed=0.9 권장
        audio_bytes = self.tts_service.synthesize(script, latents, speed=0.9)
        
        if not audio_bytes:
            print("❌ [Radio] 오디오 합성에 실패했습니다.")
            return {"status": "failed", "reason": "TTS synthesis failed"}

        # 6. S3 업로드
        filename = f"daily_radio_{target_date}_{uuid.uuid4().hex[:8]}.wav"
        s3_url = self.upload_to_s3(audio_bytes, filename)
        
        print(f"✅ [Radio] 생성 완료! URL: {s3_url}")
        
        # Latent 해제 (메모리 절약)
        self.latent_manager.release_user(self.radio_host_id)

        return {
            "status": "success",
            "date": target_date,
            "audio_url": s3_url,
            "script": script
        }

    def upload_to_s3(self, audio_bytes: bytes, filename: str) -> str:
        """
        생성된 오디오 바이트를 S3 'radio' 폴더에 업로드하고 URL을 반환합니다.
        """
        try:
            s3_key = f"radio/{filename}"
            # BytesIO로 래핑하여 업로드
            file_obj = io.BytesIO(audio_bytes)
            
            self.s3_client.upload_fileobj(
                file_obj,
                self.bucket_name,
                s3_key,
                ExtraArgs={'ContentType': 'audio/wav'}
            )
            
            # S3 URL 생성 (Region에 따라 형식 확인 필요)
            region = os.getenv('S3_REGION', 'ap-northeast-2')
            url = f"https://{self.bucket_name}.s3.{region}.amazonaws.com/{s3_key}"
            return url
            
        except Exception as e:
            print(f"❌ [S3 Upload Error] {e}")
            return None