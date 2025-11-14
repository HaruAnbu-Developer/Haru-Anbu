아# AI Core - 시니어 맞춤 AI 챗봇 서비스

## 🎯 개요
시니어(만 65세 이상)를 위한 전용 AI 챗봇 마이크로서비스입니다.
OpenAI GPT-4를 활용하여 시니어 친화적인 대화를 제공합니다.

## 🏗️ 아키텍처
```
React Native Frontend
    ↕ WebSocket (실시간 스트리밍)
FastAPI AI-Core :8001
    ↕ HTTP (JWT 검증)
Spring Boot Backend :8080
    ↕ OpenAI API
    GPT-4 (스트리밍 응답)
```

## 🚀 실행 방법

### 1. 환경 설정
```bash
# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정 (.env 파일)
OPENAI_API_KEY=sk-your-actual-openai-api-key-here
AI_CORE_HOST=0.0.0.0
AI_CORE_PORT=8001
SPRING_BACKEND_URL=https://cheongchun-backend-40635111975.asia-northeast3.run.app/api
```

### 2. 서비스 실행
```bash
# AI Core 서비스 시작
python run.py

# 또는 직접 실행
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

### 3. 헬스 체크
```bash
curl http://localhost:8001/health
# 응답: {"status":"healthy","service":"ai-core"}
```

## 📱 프론트엔드 연동

### React Native 패키지 설치
```bash
cd /path/to/frontend/my-app
npm install ws react-native-gifted-chat
```

### 사용법
1. 메인 화면에서 "AI 도우미와 채팅하기" 버튼 클릭
2. WebSocket 자동 연결
3. 실시간 스트리밍 채팅 시작

## 🔧 API 엔드포인트

### WebSocket
- **연결**: `ws://localhost:8001/ws/chat/{user_id}`
- **메시지 형식**:
  ```json
  {
    "message": "안녕하세요!",
    "history": [
      {"role": "user", "content": "이전 메시지"},
      {"role": "assistant", "content": "AI 응답"}
    ]
  }
  ```

### REST API
- **POST /chat**: 일반 채팅 (비스트리밍)
- **GET /health**: 서비스 상태 확인

## 🎨 시니어 맞춤 기능

### 프롬프트 엔지니어링
- 존댓말 사용 및 친근한 톤
- 복잡한 용어의 쉬운 설명
- 건강, 취미, 가족, 생활에 관심
- 단계별 자세한 설명
- 시니어 경험과 지혜 존중

### UI/UX 특징
- 큰 글씨 및 버튼
- 직관적인 인터페이스
- 실시간 타이핑 인디케이터
- 연결 상태 표시

## 🔐 보안

### JWT 토큰 검증
- Spring Boot 백엔드와 토큰 공유
- 인증된 사용자만 접근 가능
- 토큰 만료 시 자동 재연결

## 📊 모니터링

### 로그 레벨
- INFO: 일반 작업
- ERROR: 오류 및 예외
- DEBUG: 개발 디버깅

### 에러 처리
- OpenAI API 실패 시 친화적 오류 메시지
- WebSocket 연결 끊어짐 시 자동 재연결
- 토큰 만료 시 재인증 안내

## 🔧 개발 도구

### 테스트
```bash
# WebSocket 테스트 (wscat 사용)
npm install -g wscat
wscat -c ws://localhost:8001/ws/chat/test-user-id

# REST API 테스트
curl -X POST http://localhost:8001/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요", "user_id": "test"}'
```

## 🚨 문제 해결

### 자주 발생하는 문제
1. **OpenAI API 키 오류**: `.env` 파일의 API 키 확인
2. **WebSocket 연결 실패**: 포트 8001 사용 가능 확인
3. **JWT 토큰 오류**: Spring Boot 백엔드 연동 확인

### 디버깅
```bash
# 로그 확인
tail -f logs/ai-core.log

# 프로세스 확인
ps aux | grep python
netstat -tulpn | grep 8001
```

## 📝 TODO
- [ ] 채팅 히스토리 데이터베이스 저장
- [ ] 사용자별 개인화 설정
- [ ] 음성 입력/출력 기능
- [ ] 멀티미디어 메시지 지원