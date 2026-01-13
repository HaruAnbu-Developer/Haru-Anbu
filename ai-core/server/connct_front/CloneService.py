import torch
import os
import json
from TTS.tts.models.xtts import Xtts

class VoiceManager:
    def __init__(self, model_path, device="cuda"):
        self.device = device
        # XTTS 모델의 특징 추출기 부분만 활용 (메모리 절약을 위해 필요 시 로드)
        print("목소리 특징 추출을 위한 모델 설정 중...")
        # 실제 모델 로드 로직 (기존 서비스와 공유 가능)
        
    def extract_and_cache_latent(self, user_id: str, wav_path: str):
        """
        자녀의 음성 파일(.wav)에서 특징값을 뽑아 .json 또는 .pth로 저장합니다.
        """
        latent_dir = "./data/latents"
        os.makedirs(latent_dir, exist_ok=True)
        save_path = os.path.join(latent_dir, f"{user_id}_latent.json")

        print(f"[{user_id}] 목소리 특징 추출 시작...")
        
        # XTTS 모델의 get_conditioning_latents 함수를 사용하여 특징 추출
        # gpt_cond_latent, speaker_embedding = model.get_conditioning_latents(audio_path=[wav_path])
        
        # 추출된 값을 딕셔너리 형태로 변환하여 저장 (예시)
        latent_data = {
            "user_id": user_id,
            "latent_file": save_path,
            "status": "ready"
        }
        
        with open(save_path, 'w') as f:
            json.dump(latent_data, f)
            
        print(f"[{user_id}] 특징 추출 완료 및 저장: {save_path}")
        return save_path