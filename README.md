# Haru-Anbu

노인 돌봄을 위한 AI 음성 대화 시스템

## 프로젝트 개요

**Haru-Anbu**는 독거 노인을 위한 AI 기반 음성 대화 시스템입니다. 가족 구성원의 목소리를 복제하여 자연스러운 대화를 제공하며, 대화 분석을 통해 인지 건강 상태를 모니터링합니다.

### 주요 기능

- **실시간 음성 대화**: gRPC 기반 양방향 스트리밍으로 자연스러운 음성 대화 제공
- **음성 클로닝**: OpenVoice V2를 활용한 가족 구성원 목소리 복제
- **인지 건강 분석**: 대화 내용 분석을 통한 치매 조기 발견 및 인지 건강 점수(CHI) 산출
- **개인화된 대화**: 사용자별 메모리 시스템 및 맞춤형 일일 미션
- **커뮤니티 라디오**: 사용자들의 일상 이야기를 모아 일일 라디오 방송 생성
- **보호자 리포트**: 대화 품질 및 건강 지표에 대한 일일 리포트 제공

### 타겟 사용자

- **주 사용자**: 70-80대 독거 노인
- **보호자**: 자녀 및 가족 구성원
- **목적**: 정서적 교감 제공 및 인지 건강 모니터링

---

## 기술 스택

### CallManager (Java/Spring Boot) - 실시간 미들웨어

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 | 엔터프라이즈급 안정성 |
| **Framework** | Spring Boot | 3.5.7 | 백엔드 프레임워크 |
| **Communication** | gRPC | 1.76.0 | AI 서버 양방향 스트리밍 |
| **Real-time** | WebSocket | - | Twilio 실시간 통신 |
| **Telephony** | Twilio Voice API | 9.14.1 | 전화 통신 |
| **Database** | MySQL | 8.0 | 세션/대화 데이터 |
| **Storage** | AWS S3 SDK | 2.20.26 | 녹음 파일 저장 |
| **Build Tool** | Gradle | 8.14.3 | 빌드 자동화 |
| **Testing** | JUnit 5 | - | 단위/통합 테스트 |
| **Container** | Docker | - | 컨테이너화 |

### ai-core (Python) - AI 음성 처리 서버

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Framework** | FastAPI | 0.104.1 | REST API |
| **Communication** | gRPC + Protobuf | - | 실시간 스트리밍 |
| **Database** | SQLAlchemy | 2.0.23 | ORM |
| **Deep Learning** | PyTorch | 2.1.1 | 딥러닝 프레임워크 |
| **STT** | Faster-Whisper | - | 음성 인식 |
| **LLM** | Gemma-2-9B-IT | - | 대화 생성 (llama-cpp) |
| **TTS** | OpenVoice V2 | - | 음성 클로닝 및 합성 |
| **VAD** | Silero VAD | - | 음성 활동 감지 |
| **Audio** | librosa, soundfile | - | 오디오 처리 |
| **Scheduler** | APScheduler | - | 일일 배치 작업 |
| **Task Queue** | Celery | - | 백그라운드 작업 |

### Backend (Java/Spring Boot) - 보호자 앱 API 서버

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 | 엔터프라이즈급 안정성 |
| **Framework** | Spring Boot | 3.5.9 | 백엔드 프레임워크 |
| **Security** | Spring Security + JWT | - | 인증/인가 |
| **OAuth2** | Spring OAuth2 Client | - | 소셜 로그인 |
| **Database** | MariaDB (MySQL 호환) | - | 사용자 데이터 |
| **Cache** | Spring Data Redis | - | 세션/캐싱 |
| **API Docs** | SpringDoc OpenAPI | 2.8.4 | Swagger UI |
| **Build Tool** | Gradle | 8.14.3 | 빌드 자동화 |

### Infrastructure

* **Storage**: AWS S3 (녹음 파일, 목소리 프로필, 모델 체크포인트)
* **Database**: AWS RDS (MySQL 8.0)
* **Container**: Docker, Docker Compose
* **CI/CD**: Terraform, Ansible

---

## 프로젝트 구조
```
Haru-Anbu/
├── CallManager/                    # Java Spring Boot 미들웨어
│   ├── src/main/java/
│   │   └── com/haru_anbu/CallManager/
│   │       ├── call/
│   │       │   ├── config/        # gRPC, Twilio, S3, WebSocket 설정
│   │       │   │   ├── GrpcClientConfig.java
│   │       │   │   ├── TwilioConfig.java
│   │       │   │   ├── S3Config.java
│   │       │   │   └── WebSocketConfig.java
│   │       │   ├── controller/    # REST API 컨트롤러
│   │       │   │   ├── CallController.java
│   │       │   │   ├── RecordingController.java
│   │       │   │   └── TwilioWebHookController.java
│   │       │   ├── service/       # 비즈니스 로직
│   │       │   │   ├── CallManagerService.java
│   │       │   │   ├── CallSessionService.java
│   │       │   │   ├── TwilioService.java
│   │       │   │   ├── RecordingService.java
│   │       │   │   ├── S3StorageService.java
│   │       │   │   ├── AIVoiceProfileClient.java
│   │       │   │   └── VoiceConversationGrpcService.java
│   │       │   ├── handler/       # WebSocket 핸들러
│   │       │   │   └── TwilioMediaStreamHandler.java
│   │       │   ├── scheduler/     # 스케줄러
│   │       │   │   └── SessionCleanupScheduler.java
│   │       │   ├── entity/        # JPA 엔티티
│   │       │   │   ├── CallSession.java
│   │       │   │   └── Conversation.java
│   │       │   ├── repository/    # JPA 레포지토리
│   │       │   │   ├── CallSessionRepository.java
│   │       │   │   └── ConversationRepository.java
│   │       │   ├── dto/           # 데이터 전송 객체
│   │       │   │   ├── AudioDto.java
│   │       │   │   ├── CallSessionDto.java
│   │       │   │   ├── CallRequestResponse.java
│   │       │   │   ├── AIServiceDto.java
│   │       │   │   ├── TwilioWebhookDto.java
│   │       │   │   └── WebSocketMessageDto.java
│   │       │   └── util/
│   │       │       └── AudioConverter.java  # mulaw ↔ PCM 오디오 변환
│   │       └── grpc/              # gRPC 생성 코드
│   ├── src/main/proto/
│   │   └── ai_service.proto       # Protocol Buffers 정의
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/                  # 테스트 코드
│   │   └── java/.../call/
│   │       ├── CallManagerApplicationTests.java
│   │       ├── handler/TwilioMediaStreamHandlerTest.java
│   │       ├── service/CallSessionServiceTest.java
│   │       ├── service/RecordingServiceIntegrationTest.java
│   │       ├── service/S3StorageServiceTest.java
│   │       └── util/AudioConverterTest.java
│   ├── build.gradle
│   ├── Dockerfile
│   └── SETUP.md
│
├── ai-core/                        # Python AI 서비스
│   ├── database/
│   │   ├── database.py            # DB 연결 및 초기화
│   │   └── schema.py              # SQLAlchemy 모델
│   ├── server/
│   │   ├── grpc/                  # gRPC 서비스
│   │   │   ├── voice_stream.proto
│   │   │   ├── voice_stream_pb2.py
│   │   │   └── voice_stream_pb2_grpc.py
│   │   └── connect_back/
│   │       └── controller.py      # 음성 관리 REST 엔드포인트
│   ├── services/
│   │   ├── stt/
│   │   │   ├── stt_service.py
│   │   │   └── vad_service.py
│   │   ├── tts/
│   │   │   └── tts_service.py
│   │   ├── llm/
│   │   │   ├── llm_service_Gemma_stream.py
│   │   │   └── conversation_manager.py
│   │   ├── call_service/
│   │   │   └── call_service.py    # gRPC 서버 진입점
│   │   ├── emotion_analysis_service/
│   │   │   └── analysis_service.py
│   │   ├── voice_training_service/
│   │   │   ├── voice_processor.py
│   │   │   └── latent_manager.py
│   │   └── radio_service/
│   │       ├── radio_pipeline.py
│   │       ├── question_generator.py
│   │       ├── merge_daily_answer.py
│   │       └── scheduler.py
│   ├── checkpoints/
│   │   ├── converter/             # OpenVoice 모델
│   │   └── base_speakers/
│   ├── test/
│   │   ├── test_gpu.py
│   │   ├── test_S3.py
│   │   └── radio_test.py
│   ├── mic_to_grpc.py             # 로컬 마이크 gRPC 테스트 클라이언트
│   └── requirements.txt
│
├── backend/                        # Java Spring Boot 보호자 API 서버
│   ├── src/main/java/
│   │   └── com/cheongchun/backend/
│   │       ├── auth/              # 인증 (회원가입, 로그인, 이메일 인증)
│   │       ├── user/              # 사용자 프로필 관리
│   │       ├── token/             # RefreshToken 관리
│   │       ├── oauth/             # OAuth2 소셜 로그인
│   │       └── global/            # 공통 설정, 보안, 예외 처리
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/
│   └── build.gradle
│
├── docker-compose.yml
├── .env
├── .gitignore
└── README.md
```

---

## 주요 데이터베이스 스키마

### CallManager (Spring Boot)
| 테이블명 | 주요 역할 | 핵심 데이터 |
| :--- | :--- | :--- |
| **call_sessions** | 통화 세션 및 라이프사이클 관리 | 세션 상태(IN_PROGRESS 등), 시작/종료 시각, S3 녹음 URL |
| **conversations** | 실시간 대화 내용 저장 | 세션 ID, 역할(user/ai), STT/LLM 텍스트 |

### ai-core (Python)
| 테이블명 | 주요 역할 | 핵심 데이터 |
| :--- | :--- | :--- |
| **voice_profiles** | 가족 음성 프로필 관리 | S3 원본/벡터 경로, 처리 상태(PENDING/READY/FAILED) |
| **user_memories** | 개인화 대화 컨텍스트 저장 | 이전 대화 요약 (다음 통화 시 LLM 프롬프트 주입용) |
| **user_missions** | 맞춤형 일일 질문 관리 | 개인화 질문 텍스트, 카테고리, 답변 완료 여부 |
| **conversation_analysis** | 인지 건강 분석 결과 | CHI 종합 점수, 5대 세부 지표, 위험도, 건강 플래그 |
| **community_radio_topics** | 라디오 방송 스크립트 소스 | 사용자의 일상 답변 (익명화되어 라디오 생성에 활용) |
| **daily_question** | 공통 커뮤니티 질문 | 매일 자정 자동 갱신되는 오늘의 질문 (단일 레코드) |

### Backend (Spring Boot)
| 테이블명 | 주요 역할 | 핵심 데이터 |
| :--- | :--- | :--- |
| **users** | 보호자 계정 관리 | 이메일, 비밀번호, 이름, 소셜 로그인 Provider |
| **social_accounts** | 소셜 계정 연결 정보 | Provider(GOOGLE/NAVER/KAKAO), Provider ID |
| **refresh_tokens** | 다중 기기 세션 관리 | 토큰 문자열, 만료 시각, 기기 정보(UserAgent, IP) |

---

## API 문서

### CallManager REST API
#### 통화 관리
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **POST** | `/api/calls/initiate` | 통화 시작 |
| **GET** | `/api/calls/sessions/{sessionId}` | 세션 조회 |
| **GET** | `/api/calls/users/{userId}/sessions` | 사용자 세션 목록 |
| **POST** | `/api/calls/sessions/{sessionId}/end` | 통화 종료 |
| **POST** | `/api/calls/sessions/{sessionId}/input` | 텍스트 입력 (테스트용) |

#### 녹음 관리
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **GET** | `/api/recordings/sessions/{sessionId}/download-url` | 녹음 파일 Presigned URL 조회 |
| **DELETE** | `/api/recordings/sessions/{sessionId}` | 녹음 파일 삭제 |

#### Twilio Webhook
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **POST** | `/api/webhooks/twilio/voice` | Voice Webhook (TwiML 반환) |
| **POST** | `/api/webhooks/twilio/status` | Status Callback |
| **POST** | `/api/webhooks/twilio/recording` | Recording Callback → S3 저장 |

#### WebSocket
| 엔드포인트 | 설명 |
| :--- | :--- |
| `wss://{host}/ws/twilio/media-stream` | Twilio Media Stream 실시간 오디오 |

---

### ai-core REST API
#### 음성 관리
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **POST** | `/voice/upload/{user_id}` | 원본 음성 파일 S3 업로드 |
| **POST** | `/voice/register/{user_id}` | 음성 특징 추출 (백그라운드) |
| **POST** | `/voice/prepare/{user_id}` | 음성 특징을 GPU 메모리에 로드 |
| **POST** | `/voice/release/{user_id}` | GPU 메모리에서 음성 특징 해제 |

#### 배치 작업
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **POST** | `/force-midnight-job` | 일일 배치 작업 수동 실행 |

---

### Backend REST API
#### 인증
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **POST** | `/auth/signup` | 회원가입 |
| **POST** | `/auth/login` | 로그인 (JWT 쿠키 발급) |
| **POST** | `/auth/logout` | 로그아웃 (쿠키 삭제) |
| **GET** | `/auth/me` | 현재 사용자 정보 조회 |
| **GET** | `/auth/verify-email` | 이메일 인증 |
| **POST** | `/auth/resend-verification` | 인증 이메일 재발송 |
| **POST** | `/auth/oauth/kakao` | Kakao 인가 코드 로그인 |

#### 사용자 프로필
| Method | Endpoint | 설명 |
| :--- | :--- | :--- |
| **GET** | `/api/users/me/profile` | 프로필 조회 |
| **PATCH** | `/api/users/me/profile` | 프로필 수정 (이름, 사진, 전화번호, 생년월일) |
| **PUT** | `/api/users/me/password` | 비밀번호 변경 (LOCAL 사용자만) |
| **PATCH** | `/api/users/me/username` | 아이디 변경 |
| **DELETE** | `/api/users/me` | 계정 삭제 |
| **GET** | `/api/users/me/provider` | 인증 제공자 정보 조회 |

---

### gRPC Service
* **Protocol**: `voice_stream.proto`
* **Port**: `50051`

```protobuf
service VoiceConversation {
  rpc StreamConversation(stream VoiceRequest) returns (stream VoiceResponse);
}

message VoiceRequest {
  oneof payload {
    SessionConfig config = 1;
    bytes audio_content = 2;
  }
}

message VoiceResponse {
  oneof payload {
    bytes audio_output = 1;
    string transcript = 2;
    string ai_response = 3;
    bool is_final = 4;
  }
}
```

---

## 시스템 플로우

### 1. 핵심 데이터 플로우
```
1. Twilio → CallManager
   • WebSocket 연결 (mulaw 8kHz)
   
2. CallManager (오디오 변환)
   • mulaw 8kHz → PCM 16kHz 업샘플링
   
3. CallManager → ai-core
   • gRPC 양방향 스트리밍 (PCM 16kHz)
   
4. ai-core (AI 처리)
   • STT: 음성 → 텍스트 (300ms)
   • LLM: 대화 생성 (500ms)
   • TTS: 텍스트 → 음성 (400ms)
   • 총 1.2초 평균 응답
   
5. ai-core → CallManager
   • gRPC 응답 (PCM 16kHz)
   
6. CallManager (오디오 변환)
   • PCM 16kHz → mulaw 8kHz 다운샘플링
   
7. CallManager → Twilio
   • WebSocket 전송 (mulaw 8kHz)
   
8. Twilio → 어르신
   • 전화 출력
```

### 2. 실시간 통화 플로우

```
사용자 음성 입력
  ↓
gRPC 스트리밍 수신
  ↓
VAD (음성 활동 감지)
  ↓
STT (Faster-Whisper) → 텍스트 변환
  ↓
Conversation Manager → 대화 컨텍스트 관리
  ↓
LLM (Gemma-2) → 응답 생성
  ↓
TTS (OpenVoice) → 클론 음성 합성
  ↓
gRPC 스트리밍 송신
  ↓
사용자 스피커 출력
  ↓
[통화 종료] → S3에 대화 로그 업로드
```

### 3. 음성 프로필 등록 플로우

```
보호자 앱 (음성 파일 업로드)
  ↓
POST /voice/upload/{user_id}
  ↓
S3 uploads/{user_id}/ 저장
  ↓
POST /voice/register/{user_id}
  ↓
OpenVoice V2 Tone Color 임베딩 추출 (백그라운드)
  ↓
S3 latents/{user_id}/ 저장 (.pth)
  ↓
voice_profiles 상태 PENDING → READY
  ↓
통화 시작 전 POST /voice/prepare/{user_id}
  ↓
GPU 메모리에 latent 로드 → 실시간 합성 준비 완료
```

### 4. 일일 분석 플로우 (매일 자정 00:00)

```
APScheduler 트리거
  ↓
[1] 대화 분석 서비스
  - S3에서 전날 대화 로그 로드
  - LLM 분석 → CHI 점수, 건강 플래그 생성
  - ConversationAnalysis, UserMemory 저장
  ↓
[2] 데이터 마이그레이션
  - daily_answer → CommunityRadioTopic 복사
  ↓
[3] 질문 생성
  - DailyQuestion 생성 (공통 질문)
  - UserMission 생성 (개인화 질문, 메모리 기반)
  ↓
[4] 라디오 방송 생성
  - CommunityRadioTopic 기반 스크립트 생성
  - TTS로 오디오 합성
  - S3에 방송 업로드
```

---

## 주요 서비스 상세

### CallManager (Spring Boot)

**역할**: Twilio 통화 관리 및 AI gRPC 브리지

- **통화 흐름**: Twilio 아웃바운드 호출 → Webhook TwiML 응답 → WebSocket Media Stream 연결 → gRPC 스트리밍
- **세션 관리**: 통화 상태(INITIATED → IN_PROGRESS → COMPLETED) DB 추적 및 타임아웃 스케줄러
- **녹음 처리**: Twilio 녹음 콜백 → S3 업로드 → AI 서버 음성 프로필 연동
- **오디오 변환**: mulaw 8kHz ↔ PCM 16kHz 실시간 변환 (AudioConverter)
- **AI 프로필 연동**: 통화 종료 후 S3 녹음 경로를 ai-core에 비동기 전달 (AIVoiceProfileClient)

### STT Service (`services/stt/`)

- **엔진**: Faster-Whisper (small/medium 모델)
- **언어**: 한국어
- **특징**: VAD 필터링, 환청(hallucination) 방지
- **출력**: 텍스트 전사 + 세그먼트 정보

### TTS Service (`services/tts/`)

- **엔진**: OpenVoice V2 (Melo TTS + Tone Color Converter)
- **특징**:
  - 사용자별 음성 클로닝
  - 스트리밍 합성 지원
  - 속도(tau), 톤 파라미터 조정 가능
- **출력**: 24kHz, 16-bit PCM 오디오

### LLM Service (`services/llm/`)

- **모델**: Gemma-2-9B-IT (llama-cpp 백엔드)
- **특징**:
  - 스트리밍 생성 (문장 단위 청킹)
  - JSON 출력 모드 (구조화된 분석)
  - 대화 히스토리 관리
- **시스템 프롬프트**: "다정한 손주" (친절한 손자/손녀 페르소나)

### Conversation Manager (`services/llm/conversation_manager.py`)

- **기능**: 통화 세션 내 대화 흐름 제어
- **특징**:
  - DB에서 오늘의 UserMission 로드 후 순차 질문
  - LLM 판단 태그 `[1]` 기반 미션 완료 감지
  - UserMemory 기반 개인화 오프닝 멘트 생성
- **출력**: S3 업로드용 대화 로그 + 미션 완료 여부

### Analysis Service (`services/emotion_analysis_service/`)

- **기능**: 통화 후 인지 건강 분석
- **출력**:
  - CHI 점수 (0-100)
  - 5가지 세부 지표 (recall, coherence, orientation, stability, engagement)
  - 건강 플래그 (두통, 우울, 불면 등)
  - 보호자용 요약
  - 메모리 요약 (다음 대화용)

### Voice Training Service (`services/voice_training_service/`)

- **기능**: 음성 클로닝 파이프라인
- **프로세스**:
  1. 원본 음성 파일 S3 업로드
  2. Tone Color 임베딩 추출
  3. 특징 벡터 S3 저장
  4. GPU 메모리 로딩 (통화 시)
- **상태 관리**: PENDING → READY/FAILED

### Radio Service (`services/radio_service/`)

- **기능**: 일일 커뮤니티 라디오 방송 생성
- **컴포넌트**:
  - **radio_pipeline**: LLM 스크립트 생성 + TTS 합성
  - **question_generator**: 공통 질문 + 개인화 미션 생성
  - **merge_daily_answer**: 분석 데이터 → 라디오 토픽 마이그레이션
  - **scheduler**: APScheduler 자정 실행

### Backend (Spring Boot)

**역할**: 보호자 앱 대상 REST API 서버

- **인증**: JWT 쿠키 기반 인증, Kakao/Google/Naver OAuth2 소셜 로그인
- **사용자 관리**: 회원가입, 이메일 인증, 프로필 수정, 비밀번호 변경, 계정 삭제
- **토큰 관리**: RefreshToken 발급/무효화, 다중 기기 세션 관리, 만료 토큰 스케줄링 정리

---

## 설치 및 실행

### 사전 요구사항

- **Python**: 3.11+
- **Java**: 17+
- **CUDA**: PyTorch GPU 지원 (CUDA 11.8+)
- **AWS 계정**: S3, RDS 접근 권한
- **MySQL/MariaDB**: RDS 또는 로컬 서버
- **Twilio 계정**: Voice API 사용 권한

### 1. 환경 설정

```bash
# 레포지토리 클론
git clone <repository-url>
cd Haru-Anbu/ai-core

# 가상환경 생성 및 활성화
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt
```

### 2. 환경 변수 설정

`.env` 파일 생성: 아래 예시 참고

```env
# Database (RDS)
DB_HOST=your-rds-endpoint.rds.amazonaws.com
DB_PORT=3306
DB_USER=admin
DB_PASSWORD=your-password
DB_NAME=ai_core_db

# AWS S3
S3_ACCESS_KEY_ID=your-access-key
S3_SECRET_ACCESS_KEY=your-secret-key
S3_REGION=ap-northeast-2
S3_BUCKET_NAME=your-bucket-name

# Twilio (CallManager용)
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890
WEBHOOK_BASE_URL=https://your-domain.com

# gRPC
GRPC_AI_SERVICE_HOST=localhost
GRPC_AI_SERVICE_PORT=50051
```

### 3. 데이터베이스 초기화

```bash
cd ai-core
python -c "from database.database import init_db; init_db()"
```

### 4. 모델 체크포인트 다운로드

OpenVoice V2 모델을 `checkpoints/` 디렉토리에 배치:
```
checkpoints/
├── converter/
│   ├── config.json
│   └── checkpoint.pth
└── base_speakers/ses/kr.pth
```

### 5. 서비스 실행

#### gRPC 서버 (통화 서비스)
```bash
python services/call_service/call_service.py
# 실행 포트: 50051
```

#### REST API 서버 (관리 서비스)
```bash
uvicorn server.connect_back.controller:app --reload --port 8000
```

#### CallManager (Spring Boot)
```bash
cd CallManager
./gradlew bootRun
# 실행 포트: 8080
```

#### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun
# 실행 포트: 8080
```

#### 스케줄러 (일일 배치 작업)
```bash
python services/radio_service/scheduler.py
```

#### Docker Compose (전체 실행)
```bash
docker-compose up --build
```

---

## 테스트

### GPU 테스트
```bash
python test/test_gpu.py
```

### S3 연결 테스트
```bash
python test/test_S3.py
```

### 분석 서비스 배치 테스트
```bash
python services/emotion_analysis_service/test_analysis_batch.py
```

### 라디오 생성 테스트
```bash
python services/radio_service/radio_test.py
```

### gRPC 클라이언트 테스트 (마이크 입력)
```bash
python mic_to_grpc.py
```

### CallManager 테스트
```bash
cd CallManager
./gradlew test
```

---

## 모니터링 및 로깅

- **로깅**: Loguru를 통한 구조화된 로그
- **메트릭**: Prometheus 통합 (추가 설정 필요)
- **에러 추적**: 서비스별 로그 파일 생성
- **헬스 체크**: Spring Boot Actuator (`/actuator/health`)
- **API 문서**: Swagger UI (`/swagger-ui.html`, backend 서버)

---

## 라이센스

본 프로젝트는 내부 사용 목적으로 개발되었습니다.
