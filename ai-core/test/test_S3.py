import boto3
import os
from dotenv import load_dotenv

load_dotenv()

def test_s3_connection():
    try:
        s3 = boto3.client(
            's3',
            aws_access_key_id=os.getenv('S3_ACCESS_KEY_ID'),
            aws_secret_access_key=os.getenv('S3_SECRET_ACCESS_KEY'),
            region_name=os.getenv('S3_REGION')
        )
        
        # 버킷 목록 가져오기 테스트
        response = s3.list_buckets()
        print("✅ S3 연결 성공! 버킷 목록:")
        for bucket in response['Buckets']:
            print(f" - {bucket['Name']}")
            
    except Exception as e:
        print(f"❌ S3 연결 실패: {e}")

if __name__ == "__main__":
    test_s3_connection()