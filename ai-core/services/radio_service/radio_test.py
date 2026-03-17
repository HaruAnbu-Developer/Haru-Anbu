import asyncio
import boto3
import os
import sys
from datetime import datetime, timezone
from dotenv import load_dotenv

# ---------------------------------------------------------
# 1. 경로 설정 및 환경 변수 로드
# ---------------------------------------------------------
# 현재 파일 위치 기준으로 루트 디렉토리(../../)를 sys.path에 추가하여
# 루트에 있는 scheduler 모듈을 import 할 수 있게 합니다.
current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.abspath(os.path.join(current_dir, "../../"))
sys.path.append(root_dir)

load_dotenv()  # .env 파일을 읽어서 환경 변수로 등록

# ---------------------------------------------------------
# 2. 모듈 임포트 (경로 설정 후 실행)
# ---------------------------------------------------------
try:
    from scheduler import midnight_job
except ImportError:
    print("❌ 'scheduler.py'를 찾을 수 없습니다. 파일명이나 경로를 확인해주세요.")
    sys.exit(1)

# ---------------------------------------------------------
# 3. 설정 (환경 변수 우선 사용)
# ---------------------------------------------------------
BUCKET_NAME = os.getenv('S3_BUCKET_NAME', 'haru-anbu-voice-storage')
# RadioPipeline 코드 내부에서 저장하는 경로와 정확히 일치해야 합니다.
# 예: "radio/" 또는 "uploads/" (끝에 / 포함 권장, 루트면 "")
S3_FOLDER_PREFIX = "radio/" 

async def test_s3_upload_execution():
    print(f"🚀 [S3 통합 테스트] 시작: {datetime.now()}")
    print(f"   - Bucket: {BUCKET_NAME}")
    print(f"   - Prefix: {S3_FOLDER_PREFIX}")

    # ---------------------------------------------------------
    # 4. S3 클라이언트 연결 (명시적 인증 정보 사용)
    # ---------------------------------------------------------
    access_key = os.getenv('S3_ACCESS_KEY_ID')
    secret_key = os.getenv('S3_SECRET_ACCESS_KEY')
    region = os.getenv('S3_REGION', 'ap-northeast-2')

    try:
        s3 = boto3.client(
            's3',
            aws_access_key_id=access_key,
            aws_secret_access_key=secret_key,
            region_name=region
        )
        print("✅ S3 클라이언트 연결 성공")
    except Exception as e:
        print(f"❌ S3 연결 설정 실패: {e}")
        return

    # 작업 시작 시간 기록 (S3 LastModified는 UTC 기준이므로 UTC로 맞춤)
    start_time = datetime.now(timezone.utc)
    
    print("\nrunning midnight_job()... (생성 및 업로드 대기 중)")

    # ---------------------------------------------------------
    # 5. 자정 작업 강제 실행
    # ---------------------------------------------------------
    try:
        await midnight_job()
        print("✅ midnight_job 실행 완료")
    except Exception as e:
        print(f"❌ 실행 중 로직 에러 발생: {e}")
        import traceback
        traceback.print_exc()
        return

    print("\n🔎 S3 업로드 결과 확인 중...")

    # ---------------------------------------------------------
    # 6. S3 버킷 조회 및 검증
    # ---------------------------------------------------------
    try:
        response = s3.list_objects_v2(
            Bucket=BUCKET_NAME, 
            Prefix=S3_FOLDER_PREFIX
        )
        
        if 'Contents' not in response:
            print(f"❌ 실패: 버킷({BUCKET_NAME}/{S3_FOLDER_PREFIX})에 파일이 하나도 없습니다.")
            print("   -> RadioPipeline 코드 내의 '저장 경로'가 위 Prefix와 일치하는지 확인하세요.")
            return

        # LastModified 기준으로 정렬 (최신순)
        files = sorted(response['Contents'], key=lambda x: x['LastModified'], reverse=True)
        latest_file = files[0]
        
        latest_filename = latest_file['Key']
        latest_time = latest_file['LastModified']
        latest_size = latest_file['Size']

        # 검증: 파일 생성 시간이 테스트 시작 시간보다 뒤인가?
        # (네트워크 딜레이 등을 고려하여 약간의 오차는 허용될 수 있으나, usually > works)
        if latest_time > start_time:
            print(f"🎉 성공! S3에 새로운 파일이 업로드되었습니다.")
            print(f"   📂 파일명: {latest_filename}")
            print(f"   ⏰ 업로드 시간: {latest_time} (UTC)")
            print(f"   💾 크기: {latest_size / 1024:.2f} KB")
            
            if latest_size < 1024:
                print("⚠️ 경고: 파일 크기가 너무 작습니다 (1KB 미만). 내용이 비어있을 수 있습니다.")
        else:
            print("❌ 실패: 작업은 끝났지만, 최신 파일이 테스트 시작 전 파일입니다.")
            print(f"   - 테스트 시작 시간: {start_time}")
            print(f"   - 가장 최근 파일: {latest_filename}")
            print(f"   - 최근 파일 시간: {latest_time}")
            print("   -> 파일이 덮어씌워졌거나, 업로드가 실패했을 수 있습니다.")
            
    except Exception as e:
        print(f"❌ S3 조회 및 검증 중 에러 발생: {e}")

if __name__ == "__main__":
    asyncio.run(test_s3_upload_execution())