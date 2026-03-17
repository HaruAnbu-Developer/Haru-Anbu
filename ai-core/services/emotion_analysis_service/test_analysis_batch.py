import asyncio
import logging
import os
import sys
from dotenv import load_dotenv

# 1. 프로젝트 루트 경로 설정 (모듈 import를 위해 필수)
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

# 2. 환경 변수 로드 (.env)
load_dotenv()

# 3. 로깅 설정 (콘솔에 과정이 보이도록)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 4. 서비스 모듈 임포트
try:
    from services.emotion_analysis_service.analysis_service import get_analysis_service
except ImportError as e:
    logger.error(f"모듈 임포트 실패: {e}")
    logger.error("실행 위치가 프로젝트 루트(ai-core)인지 확인해주세요.")
    sys.exit(1)

async def test_analysis_batch():
    print("\n🧪 [TEST] AnalysisService 배치 테스트 시작\n" + "="*50)
    
    # 서비스 인스턴스 가져오기
    try:
        service = get_analysis_service()
        print("✅ AnalysisService 초기화 완료")
    except Exception as e:
        print(f"❌ 서비스 초기화 실패: {e}")
        return

    # 배치 작업 실행
    print("\n🏃‍♂️ run_daily_analysis_batch() 실행 중...")
    print("   (S3 logs 폴더에서 '어제' 이후 생성된 파일을 찾습니다)\n")
    
    try:
        await service.run_daily_analysis_batch()
        print("\n" + "="*50)
        print("✅ 테스트 성공: 배치 작업이 에러 없이 종료되었습니다.")
        print("   DB의 'conversation_analysis', 'user_memories', 'user_missions' 테이블을 확인하세요.")
    except Exception as e:
        print("\n" + "="*50)
        print(f"❌ 테스트 실패: 실행 중 에러 발생\n{e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 비동기 실행
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(test_analysis_batch())