# services/voice_processor.py
import os
import torch
import boto3
import time
from dotenv import load_dotenv

# .env 파일 위치를 명확히 지정하거나 실행 경로에서 읽도록 로드
load_dotenv()
from services.tts.tts_service import get_tts_service # 기존 서비스 임포트

class VoiceProcessor:
    def __init__(self):
        access_key = os.getenv('S3_ACCESS_KEY_ID')
        secret_key = os.getenv('S3_SECRET_ACCESS_KEY')
        region = os.getenv('S3_REGION', 'ap-northeast-2')
        self.bucket_name = os.getenv('S3_BUCKET_NAME')
        
        # boto3 클라이언트에 명시적으로 전달
        self.s3 = boto3.client(
            's3',
            aws_access_key_id=access_key,
            aws_secret_access_key=secret_key,
            region_name=region
        )
        # 싱글톤으로 관리되는 기존 TTS 서비스 가져오기
        self.tts_service = get_tts_service()

    async def extract_and_save_latent(self, user_id: str):
        from database.schema import VoiceProfile, StatusEnum
        from database.database import SessionLocal
        db_session = SessionLocal()
        profile = db_session.query(VoiceProfile).filter(VoiceProfile.user_id == user_id).first()
        if not profile or not profile.raw_wav_path: return

        temp_wav = f"temp_{user_id}.wav"
        local_latent_path = f"{user_id}_latent.pth"
        s3_latent_key = f"latents/{user_id}/{user_id}_latent.pth"
        try:
            profile.status = StatusEnum.PENDING
            db_session.commit()

            # 1. S3에서 파일 다운로드
            self.s3.download_file(self.bucket_name, profile.raw_wav_path, temp_wav)

            # 2. 기존 서비스를 통해 특징 추출
            tone_color_embedding = self.tts_service.extract_tone_color(temp_wav)

            # 3. 로컬 저장 후 S3 업로드
            latent_data = {
                "tone_color_embedding": tone_color_embedding,
                "version": "openvoice_v2",
                "extracted_at": time.time()
            }
            torch.save(latent_data, local_latent_path)
            self.s3.upload_file(local_latent_path, self.bucket_name, s3_latent_key)

            # 4. DB 업데이트
            profile.latent_path = s3_latent_key
            profile.status = StatusEnum.READY
            db_session.commit()
            print(f"✅ [VoiceProcessor] {user_id} 복제 데이터 생성 완료")

        except Exception as e:
            profile.status = StatusEnum.FAILED
            db_session.commit()
            print(f"❌ [VoiceProcessor] 에러: {e}")
        finally:
            if os.path.exists(temp_wav): os.remove(temp_wav)
            if os.path.exists(local_latent_path): os.remove(local_latent_path)