import uvicorn
import os
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

if __name__ == "__main__":
    host = os.getenv("AI_CORE_HOST", "0.0.0.0")
    port = int(os.getenv("AI_CORE_PORT", "8001"))
    
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=True,  # 개발 환경에서만
        log_level="info"
    )