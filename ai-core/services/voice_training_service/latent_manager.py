# services/voice_training_service/latent_manager.py
import torch
import os
import boto3
from typing import Dict
from dotenv import load_dotenv
import tempfile
load_dotenv() # .env 로드 확인

class LatentManager:
    def __init__(self, s3_client, bucket_name, device):
        self.s3 = s3_client
        self.bucket_name = bucket_name
        self.device = device
        # 핵심: 메모리에 로드된 특징값들 저장 {user_id: latent_dict}
        self.active_latents: Dict[str, dict] = {}

    def prepare_user(self, user_id: str, s3_key: str):
        """전화 걸기 전 미리 S3에서 로드"""
        if user_id in self.active_latents:
            return True # 이미 준비됨
        
        try:
                # NamedTemporaryFile을 사용하면 context 매니저 종료 시 자동 삭제됨
            with tempfile.NamedTemporaryFile(suffix=".pth", delete=True) as tmp:
                self.s3.download_file(self.bucket_name, s3_key, tmp.name)
                # 특징값 로드 및 GPU 이동
                latents = torch.load(tmp.name, map_location=self.device)
                self.active_latents[user_id] = latents
            return True
        except Exception as e:
            print(f"❌ Prepare failed for {user_id}: {e}")
            return False

    def get_latent(self, user_id: str):
        """합성 시 메모리에서 즉시 꺼내기"""
        return self.active_latents.get(user_id)

    def release_user(self, user_id: str):
        """통화 종료 시 메모리 점유 해제"""
        if user_id in self.active_latents:
            del self.active_latents[user_id]
            # GPU 캐시 정리 (필요시)
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            print(f"🗑️ {user_id} 메모리 해제 완료")
            
# 싱글톤
_latent_manager_instance = None

def get_latent_manager(s3_client=None, bucket_name=None, device=None):
    global _latent_manager_instance
    if _latent_manager_instance is None:
        # 1. 환경 변수에서 S3 정보 로드
        access_key = os.getenv('S3_ACCESS_KEY_ID')
        secret_key = os.getenv('S3_SECRET_ACCESS_KEY')
        region = os.getenv('S3_REGION', 'ap-northeast-2')
        bucket_name = os.getenv('S3_BUCKET_NAME')
        
        # 2. boto3 클라이언트 생성
        s3_client = boto3.client(
            's3',
            aws_access_key_id=access_key,
            aws_secret_access_key=secret_key,
            region_name=region
        )
            
        # 3. 디바이스 설정
        device = "cuda" if torch.cuda.is_available() else "cpu"
            
        # 4. 인스턴스 생성
        _latent_manager_instance = LatentManager(
            s3_client=s3_client,
            bucket_name=bucket_name,
            device=device
        )
        print(f"✅ LatentManager initialized with bucket: {bucket_name}")
            
    return _latent_manager_instance