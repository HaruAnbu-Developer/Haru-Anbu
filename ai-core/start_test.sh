#!/bin/bash

echo "🚀 시니어 맞춤 AI 챗봇 시스템 테스트"
echo "======================================"

# 환경 확인
echo "1. 환경 설정 확인..."
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다!"
    exit 1
fi

if [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  OPENAI_API_KEY가 설정되지 않았습니다."
    echo "   .env 파일에 실제 OpenAI API 키를 설정하세요."
fi

# 의존성 설치
echo "2. 의존성 설치..."
pip install -q python-dotenv websockets

# AI Core 서비스 시작
echo "3. AI Core 서비스 시작..."
echo "   포트 8001에서 서비스를 시작합니다."
echo "   Ctrl+C로 중지하세요."
echo ""

python run.py &
AI_CORE_PID=$!

# 서비스 시작 대기
sleep 3

# 헬스 체크
echo "4. 헬스 체크..."
curl -s http://localhost:8001/health || echo "AI Core 서비스 시작 실패"

echo ""
echo "📋 테스트 방법:"
echo "   1. 새 터미널: python test_ai_system.py"
echo "   2. WebSocket 테스트: python websocket_test_client.py" 
echo "   3. 수동 테스트: curl http://localhost:8001/health"
echo "   4. 프론트엔드 앱에서 채팅 테스트"
echo ""
echo "⏹️  종료하려면 Ctrl+C를 누르세요"

# AI Core 프로세스 대기
wait $AI_CORE_PID