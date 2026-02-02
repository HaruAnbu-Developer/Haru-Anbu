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
        
    # [추가됨] 목소리 파일 업로드 (덮어쓰기 방식)
    async def upload_raw_voice(self, user_id: str, file_obj, filename: str):
        from database.schema import VoiceProfile, StatusEnum
        from database.database import SessionLocal
        
        db_session = SessionLocal()
        try:
            # 1. 확장자 추출 및 고정 경로 생성
            ext = os.path.splitext(filename)[1] # .wav, .mp3 등
            s3_key = f"uploads/{user_id}/{user_id}{ext}"
            
            # 2. S3 업로드 (동일 경로이므로 자동 덮어쓰기됨)
            # file_obj 포인터 초기화
            file_obj.seek(0)
            self.s3.upload_fileobj(file_obj, self.bucket_name, s3_key)
            
            # 3. DB 프로필 확인 및 업데이트
            profile = db_session.query(VoiceProfile).filter(VoiceProfile.user_id == user_id).first()
            
            if not profile:
                # 프로필이 없으면 새로 생성
                profile = VoiceProfile(
                    user_id=user_id,
                    raw_wav_path=s3_key,
                    status=StatusEnum.PENDING # 아직 분석 전이므로 PENDING or NONE
                )
                db_session.add(profile)
            else:
                # 있으면 경로 업데이트 (확장자가 바뀔 수도 있으므로)
                profile.raw_wav_path = s3_key
                # 새 파일이 올라왔으니 상태를 초기화하거나 PENDING으로 유지
                # (분석은 별도 API /voice/register 호출 시 진행)
            
            db_session.commit()
            print(f"✅ [VoiceProcessor] {user_id} 원본 음성 업로드 완료: {s3_key}")
            return s3_key
            
        except Exception as e:
            db_session.rollback()
            print(f"❌ [VoiceProcessor] 업로드 에러: {e}")
            raise e
        finally:
            db_session.close()

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
            import shutil
            if os.path.exists("processed"):
                shutil.rmtree("processed") # 중간 생성 폴더 통째로 삭제
            db_session.close() # 세션 닫기 추가

if __name__ == "__main__":
    import asyncio

    async def main():
        # 1. 프로세서 인스턴스 생성
        processor = VoiceProcessor()
        
        # 2. 테스트할 사용자 ID 지정 (실제 DB에 VoiceProfile이 있는 ID여야 함)
        test_user_id = "radio_voice_1" # <--- 여기를 실제 ID로 바꿔주세요
        #목소리 추출할 유저 아이디 입력
        
        print(f"🔄 [Test] {test_user_id}의 목소리 특징 추출 시작...")
        await processor.extract_and_save_latent(test_user_id)
        print("🏁 [Test] 프로세스 종료")

    # 비동기 함수 실행
    asyncio.run(main())