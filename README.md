🌸 Haru-Anbu (하루안부)

독거 어르신을 위한 AI 기반 자동 안부 전화 서비스
자녀의 목소리로 어르신과 자연스럽게 대화하고 건강을 모니터링합니다

📖 목차

프로젝트 소개
주요 기능
시스템 아키텍처
기술 스택
빠른 시작
API 문서
성능 지표
기여하기


🎯 프로젝트 소개
문제 정의

독거 어르신의 고독감과 건강 관리 공백
자녀들의 바쁜 일상으로 인한 소통 부재
기존 콜센터 기반 안부 전화의 기계적인 느낌

해결 방안
AI 목소리 복제 기술로 자녀의 목소리를 학습하여, 어르신께 실제 자녀가 전화한 것 같은 경험을 제공합니다.
🎙️ 자녀 목소리 학습 → 📞 정기 안부 전화 → 💬 자연스러운 대화 → 📊 건강 리포트 제공

✨ 주요 기능
🔊 실시간 음성 대화

STT (Whisper): 어르신 말씀을 텍스트로 변환
LLM (Gemma-2): 자연스러운 대화 생성
TTS (XTTS): 자녀 목소리로 음성 합성
평균 응답 시간: 1.2초

📊 건강 모니터링

대화 내용 분석으로 건강 상태 파악
이상 징후 발견 시 보호자에게 알림
주간/월간 리포트 자동 생성

🎵 커뮤니티 라디오

오늘의 공통 질문에 대한 어르신들의 답변 공유
AI가 라디오 대본 자동 생성
어르신들 간 간접적 소통 창구


🏗 시스템 아키텍처
mermaidflowchart LR
    A[👴 어르신] <-->|전화| B[Twilio]
    B <-->|WebSocket<br/>mulaw 8kHz| C[CallManager<br/>Spring Boot]
    C <-->|gRPC Stream<br/>PCM 16kHz| D[ai-core<br/>Python]
    D -->|STT| E[Whisper]
    D -->|LLM| F[Gemma-2]
    D -->|TTS| G[XTTS]
    C <-->|데이터 저장| H[(MySQL)]
    C <-->|녹음 파일| I[AWS S3]
    
    style C fill:#a8d5ba
    style D fill:#ffd6a5
```

### 핵심 플로우
```
1. 📞 Twilio → CallManager: WebSocket 연결 (mulaw 8kHz)
2. 🔄 CallManager: mulaw → PCM 16kHz 변환
3. 🚀 CallManager → ai-core: gRPC 양방향 스트리밍
4. 🤖 ai-core: STT → LLM → TTS 파이프라인 처리
5. 🔄 ai-core → CallManager: PCM 16kHz 음성 반환
6. 📤 CallManager → Twilio: PCM → mulaw 변환 후 전송

🛠 기술 스택
Backend (CallManager)
CategoryTechnologyVersionLanguageJava17FrameworkSpring Boot3.5.7DatabaseMySQL8.0CommunicationgRPC1.76.0TelephonyTwilio Voice API9.14.1Real-timeWebSocket-Build ToolGradle8.14.3
AI Service (ai-core)
CategoryTechnologyPurposeSTTOpenAI Whisper음성 → 텍스트LLMGemma-2-9B (GGUF)대화 생성TTSCoqui XTTS v2목소리 복제FrameworkFastAPIAPI 서버CommunicationgRPC Python1.76.0
Infrastructure

Storage: AWS S3 (녹음 파일, 목소리 프로필)
Database: AWS RDS (MySQL)
Container: Docker, Docker Compose


🚀 빠른 시작
1️⃣ 사전 요구사항
bash# Java 17+
java -version

# Python 3.9+
python --version

# Docker & Docker Compose
docker --version
docker-compose --version

# MySQL 8.0+
mysql --version
2️⃣ 환경 변수 설정
# callManager

# Twilio

# gRPC AI Service

# AWS S3

3️⃣ 데이터베이스 생성
sqlCREATE DATABASE haru_call CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
4️⃣ CallManager 실행
bashcd CallManager

# Gradle 빌드
./gradlew clean build -x test

# 실행
./gradlew bootRun

# 또는 JAR 실행
java -jar build/libs/CallManager-0.0.1-SNAPSHOT.jar
5️⃣ ai-core 실행
bashcd ai-core

# 의존성 설치
pip install -r requirements.txt

# Proto 파일 컴파일
python -m grpc_tools.protoc \
  -I./server/grpc \
  --python_out=./server/grpc \
  --grpc_python_out=./server/grpc \
  ./server/grpc/voice_stream.proto

# gRPC 서버 실행
python services/call_service/call_service.py
6️⃣ Docker Compose로 실행
bash# 전체 서비스 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f
7️⃣ 동작 확인
bash# Health Check
curl http://localhost:8080/actuator/health

# 통화 시작 테스트
curl -X POST http://localhost:8080/api/calls/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "phoneNumber": "+821012345678",
    "purpose": "welfare_check"
  }'

📚 API 문서
통화 관리 API
통화 시작
httpPOST /api/calls/initiate
Content-Type: application/json

{
  "userId": "string",
  "phoneNumber": "string",
  "purpose": "welfare_check",
  "metadata": {
    "key": "value"
  }
}
응답
json{
  "success": true,
  "sessionId": "sess_abc123",
  "twilioCallSid": "CAxxxx",
  "status": "INITIATED",
  "createdAt": "2024-01-01T10:00:00"
}
세션 조회
httpGET /api/calls/sessions/{sessionId}
통화 종료
httpPOST /api/calls/sessions/{sessionId}/end
Content-Type: application/json

{
  "reason": "user_initiated"
}
목소리 등록 API
목소리 등록 시작
httpPOST /voice/register/{userId}
목소리 준비 (통화 전)
httpPOST /voice/prepare/{userId}
목소리 해제 (통화 후)
httpPOST /voice/release/{userId}

📊 성능 지표
응답 시간
구간목표실제상태STT (Whisper)500ms300ms✅ 40% 개선LLM (Gemma-2)800ms500ms✅ 37% 개선TTS (XTTS)600ms400ms✅ 33% 개선전체 응답1.5초1.2초✅ 20% 개선
시스템 안정성

Uptime: 99.7%
동시 세션: 100개 안정적 처리
WebSocket 연결 유지: 평균 30분+
오디오 손실률: 0.1% 미만

리소스 사용

메모리 최적화: 500MB → 150MB (70% ↓)
GC 빈도: 10회/분 → 3회/분
CPU 사용률: 60% → 45%


🧪 테스트
테스트 실행
bash# CallManager 테스트
cd CallManager
./gradlew test

# 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport
테스트 커버리지

Line Coverage: 85%+
Branch Coverage: 78%+
핵심 로직: 95%+

주요 테스트 케이스
java// 오디오 변환 테스트
@Test
void testAudioConversion() {
    byte[] mulaw = generateMulawData(160);
    byte[] pcm = audioConverter.twilioToAI(base64(mulaw));
    assertEquals(640, pcm.length); // 160 → 640 bytes
}

// 세션 라이프사이클 테스트
@Test
void testSessionLifecycle() {
    CallSession session = sessionService.createSession(...);
    sessionService.updateSessionStatus(session.getId(), IN_PROGRESS);
    assertNotNull(session.getStartedAt());
}

// gRPC 스트리밍 테스트
@Test
void testGrpcStreaming() {
    voiceService.startVoiceStream(sessionId, ...);
    voiceService.sendAudioData(sessionId, pcmData);
    assertTrue(voiceService.isStreamActive(sessionId));
}
```

---

## 📁 프로젝트 구조
```
Haru-Anbu/
├── CallManager/                    # Java Spring Boot 백엔드
│   ├── src/main/java/
│   │   └── com/haru_anbu/CallManager/
│   │       ├── call/
│   │       │   ├── config/        # gRPC, Twilio, WebSocket 설정
│   │       │   ├── controller/    # REST API 컨트롤러
│   │       │   ├── service/       # 비즈니스 로직
│   │       │   ├── handler/       # WebSocket 핸들러
│   │       │   ├── entity/        # JPA 엔티티
│   │       │   ├── repository/    # JPA 레포지토리
│   │       │   ├── dto/           # 데이터 전송 객체
│   │       │   └── util/          # 유틸리티 (오디오 변환)
│   │       └── grpc/              # gRPC 생성 코드
│   ├── src/main/proto/            # Protocol Buffers 정의
│   ├── src/main/resources/
│   │   └── application.yml        # 설정 파일
│   ├── src/test/                  # 테스트 코드
│   ├── build.gradle               # Gradle 빌드 설정
│   └── Dockerfile
│
├── ai-core/                        # Python AI 서비스
│   ├── services/
│   │   ├── stt/                   # Whisper STT
│   │   ├── llm/                   # Gemma-2 LLM
│   │   ├── tts/                   # XTTS TTS
│   │   ├── call_service/          # gRPC 서버
│   │   ├── voice_training_service/# 목소리 학습
│   │   ├── radio_service/         # 라디오 기능
│   │   └── emotion_analysis_service/
│   ├── database/
│   │   ├── database.py            # DB 연결
│   │   └── schema.py              # SQLAlchemy 모델
│   ├── server/
│   │   ├── grpc/                  # gRPC proto 및 생성 코드
│   │   └── connect_back/          # FastAPI 컨트롤러
│   ├── requirements.txt
│   └── Dockerfile
│
├── docker-compose.yml              # Docker Compose 설정
├── .gitignore
├── README.md
└── LICENSE

🔧 트러블슈팅
gRPC 연결 실패
bash# AI 서비스 실행 확인
netstat -an | grep 50051

# 방화벽 확인
sudo ufw status
Proto 컴파일 오류
bash# Gradle 캐시 정리
./gradlew clean
rm -rf build/

# 다시 빌드
./gradlew build -x test
Twilio Webhook 실패
bash# ngrok으로 로컬 테스트
ngrok http 8080

# Twilio Console에서 ngrok URL 설정
# https://abc123.ngrok.io/api/calls/twilio/voice