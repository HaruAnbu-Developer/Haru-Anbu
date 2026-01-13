# services/voice_processor.py (등록 전용 객체)

class VoiceProcessor:
    def __init__(self):
        # 실시간용 모델과 별도로 추출에 최적화된 설정으로 로드
        self.encoder = None # 필요한 시점에 로드하거나 싱글톤으로 관리

    async def process_user_voice(self, db_session, user_id, raw_path):
        """
        이 함수는 백그라운드 태스크로 실행됩니다.
        """
        try:
            # 1. 모델 로드 (추출 시에만 메모리 점유하도록 설계 가능)
            # 2. 특징 추출 (gpt_cond_latent, speaker_embedding)
            # 3. 파일 저장 (.pth)
            # 4. DB 업데이트 (Status: READY, latent_path: 저장경로)
            print(f"User {user_id}의 목소리 특징 추출 시작...")
            
            # 실제 추출 로직 (예시 주석)
            # latent = self.extract(raw_path) 
            # self.save(latent, f"./latents/{user_id}.pth")
            
            return True
        except Exception as e:
            # 실패 시 DB에 FAILED 기록
            return False