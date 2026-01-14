# upload_test.py
import boto3
import os
from dotenv import load_dotenv

load_dotenv() # .env 파일을 읽어서 환경 변수로 등록

def upload_sample_voice():
    # 환경 변수에서 값 가져오기
    access_key = os.getenv('S3_ACCESS_KEY_ID')
    secret_key = os.getenv('S3_SECRET_ACCESS_KEY')
    region = os.getenv('S3_REGION', 'ap-northeast-2')
    bucket_name = os.getenv('S3_BUCKET_NAME')

    # 클라이언트에 명시적으로 전달
    s3 = boto3.client(
        's3',
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        region_name=region
    )
    
    local_file = "test.wav" 
    s3_key = "uploads/test_user_1/test.wav"
    
    try:
        if not os.path.exists(local_file):
            print(f"❌ {local_file} 파일이 없습니다! 경로를 확인하세요.")
            return

        print(f"🚀 S3 업로드 시작: {local_file} -> {s3_key}")
        s3.upload_file(local_file, bucket_name, s3_key)
        print(f"✅ 업로드 완료! s3://{bucket_name}/{s3_key}")
        
    except Exception as e:
        print(f"❌ 업로드 실패: {e}")

if __name__ == "__main__":
    upload_sample_voice()