# Haru-Anbu AI Core

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

### Core Framework
- **FastAPI** (0.104.1) - REST API
- **gRPC** + **Protobuf** - 실시간 스트리밍
- **SQLAlchemy** (2.0.23) + **Alembic** - ORM 및 마이그레이션
- **PyTorch** (2.1.1) - 딥러닝 프레임워크

### AI/ML Models
- **Faster-Whisper** - 음성 인식 (Speech-to-Text)
- **Gemma-2-9B-IT** - 대화 생성 언어 모델 (via llama-cpp)
- **OpenVoice V2** - 음성 클로닝 및 TTS
- **Silero VAD** - 음성 활동 감지
- **Sentence Transformers** - 텍스트 임베딩
- **FAISS** - 벡터 유사도 검색

### Infrastructure
- **AWS S3** - 음성 파일 및 모델 저장
- **AWS RDS** (MySQL) - 데이터베이스
- **Redis** - 캐싱
- **APScheduler** - 일일 배치 작업 (자정 실행)
- **Celery** - 백그라운드 작업 큐

### Audio Processing
- **librosa** - 오디오 특징 추출
- **soundfile** - WAV 파일 I/O
- **PyAudio** - 마이크/스피커 I/O

---

## 프로젝트 구조

```
Haru-Anbu/
├── ai-core/
│   ├── database/              # 데이터베이스 스키마 및 연결
│   │   ├── database.py        # DB 초기화
│   │   └── models.py          # SQLAlchemy 모델
│   │
│   ├── server/
│   │   ├── grpc/              # gRPC 프로토콜 정의
│   │   │   └── voice_conversation.proto
│   │   └── connect_back/      # FastAPI REST 엔드포인트
│   │       └── controller.py  # 음성 관리 API
│   │
│   ├── services/              # 핵심 AI/ML 서비스
│   │   ├── stt/               # Speech-to-Text
│   │   │   ├── stt_service.py
│   │   │   └── vad_service.py
│   │   ├── tts/               # Text-to-Speech
│   │   │   └── tts_service.py
│   │   ├── llm/               # 언어 모델
│   │   │   ├── llm_service_Gemma_stream.py
│   │   │   └── conversation_manager.py
│   │   ├── call_service/      # 통화 처리 및 스트리밍
│   │   │   └── call_service.py
│   │   ├── emotion_analysis_service/  # 대화 분석
│   │   │   └── analysis_service.py
│   │   ├── voice_training_service/    # 음성 클로닝
│   │   │   ├── voice_processor.py
│   │   │   └── latent_manager.py
│   │   └── radio_service/     # 라디오 방송 생성
│   │       ├── radio_pipeline.py
│   │       ├── question_generator.py
│   │       ├── merge_daily_answer.py
│   │       └── scheduler.py
│   │
│   ├── checkpoints/           # 사전 학습 모델 가중치
│   │   ├── converter/         # OpenVoice 모델
│   │   └── base_speakers/     # 기본 TTS 화자
│   │
│   ├── test/                  # 테스트 파일 및 샘플 오디오
│   │   ├── test_gpu.py
│   │   ├── test_S3.py
│   │   ├── radio_test.py
│   │   └── test_analysis_batch.py
│   │
│   └── requirements.txt       # Python 패키지 의존성
│
├── .env                       # 환경 변수 (AWS, DB 설정)
└── README.md                  # 본 문서
```

---

## 데이터베이스 스키마

### 주요 테이블

#### 1. VoiceProfile
사용자별 음성 프로필 관리
```sql
- id (PRIMARY KEY)
- user_id (UNIQUE) - 자녀 사용자 ID
- raw_wav_path - S3 원본 음성 파일 경로
- latent_path - S3 음성 특징 벡터 경로 (.pth)
- status (ENUM: PENDING, READY, FAILED)
- updated_at
```

#### 2. UserMemory
사용자별 대화 기억 저장
```sql
- id (PRIMARY KEY)
- user_id (INDEX)
- conversation_id (UNIQUE)
- summary_text - "어르신이 오늘 기분이 좋으셨음"
- created_at
```

#### 3. UserMission
사용자별 일일 맞춤 질문
```sql
- id (PRIMARY KEY)
- user_id
- mission_text - 개인화된 일일 질문
- category - "health", "memory", "meal", "general"
- is_cleared - 답변 완료 여부
- created_at
```

#### 4. ConversationAnalysis
대화 분석 결과 및 인지 건강 점수
```sql
- id (PRIMARY KEY)
- conversation_id (UNIQUE)
- user_id
- chi_score (0-100) - 인지 건강 지수
- danger_level (0: 정상, 1: 주의, 2: 위험)
- recall_score (0-20) - 기억력
- coherence_score (0-20) - 대화 일관성
- orientation_score (0-20) - 시간/장소 인지
- stability_score (0-20) - 감정 안정성
- engagement_score (0-20) - 참여도
- question_results (JSON) - 질문별 정답 여부
- summary (TEXT) - 보호자용 요약
- health_flags (JSON) - ["headache", "cough"] 건강 이슈
- daily_answer - 라디오 방송용 답변
- analyzed_at
```

#### 5. CommunityRadioTopic
커뮤니티 라디오 컨텐츠
```sql
- id (PRIMARY KEY)
- user_id
- answer_text - 방송에 사용될 사용자 답변
- created_at
```

#### 6. DailyQuestion
일일 커뮤니티 질문 (단일 레코드)
```sql
- id (PRIMARY KEY, default=1)
- question_content - 오늘의 공통 질문
- category
- updated_at
```

---

## API 엔드포인트

### gRPC Service: VoiceConversation

**프로토콜**: `voice_conversation.proto`
**포트**: `50051`

```protobuf
service VoiceConversation {
  rpc StreamConversation(stream VoiceRequest) returns (stream VoiceResponse);
}
```

#### Request Types
- `SessionConfig`: 세션 초기화 (user_id, session_id, sample_rate, language_code)
- `audio_content`: PCM 오디오 청크 (권장: 16kHz, 16-bit, mono)

#### Response Types
- `audio_output`: AI 음성 오디오 청크 (24kHz, 16-bit)
- `transcript`: 사용자 음성 텍스트 변환
- `ai_response`: AI 응답 텍스트
- `is_final`: 문장 완료 플래그

### REST API Endpoints

**프레임워크**: FastAPI
**기본 포트**: `8000`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/voice/upload/{user_id}` | 원본 음성 파일 S3 업로드 |
| POST | `/voice/register/{user_id}` | 음성 특징 추출 (백그라운드) |
| POST | `/voice/prepare/{user_id}` | 음성 특징을 GPU 메모리에 로드 |
| POST | `/voice/release/{user_id}` | GPU 메모리에서 음성 특징 해제 |
| POST | `/force-midnight-job` | 일일 배치 작업 수동 실행 |

---

## 시스템 플로우

### 1. 실시간 통화 플로우

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

### 2. 일일 분석 플로우 (매일 자정 00:00)

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

## 설치 및 실행

### 사전 요구사항

- **Python**: 3.11+
- **CUDA**: PyTorch GPU 지원 (CUDA 11.8+)
- **AWS 계정**: S3, RDS 접근 권한
- **MySQL**: RDS 또는 로컬 MySQL 서버

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

`.env` 파일 생성:

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

#### 스케줄러 (일일 배치 작업)
```bash
python services/radio_service/scheduler.py
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

---

## 주요 서비스 상세

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

---

## 보안 및 프라이버시

- **데이터 암호화**: S3 버킷 암호화 활성화
- **액세스 제어**: IAM 역할 기반 S3/RDS 접근
- **개인정보 보호**: 음성 데이터는 익명화된 user_id로 관리
- **HTTPS**: 프로덕션 환경 TLS 적용 권장

---

## 성능 최적화

- **GPU 가속**: PyTorch CUDA 지원 (RTX 3090/4090 권장)
- **Mixed Precision**: 추론 시 FP16 사용
- **In-Memory Caching**: 음성 latent 벡터 GPU 메모리 캐싱
- **Streaming**: 청크 단위 오디오 전송으로 레이턴시 최소화
- **VAD 최적화**: 침묵 구간 필터링으로 불필요한 STT 호출 제거

---

## 모니터링 및 로깅

- **로깅**: Loguru를 통한 구조화된 로그
- **메트릭**: Prometheus 통합 (추가 설정 필요)
- **에러 추적**: 서비스별 로그 파일 생성
- **헬스 체크**: REST API `/health` 엔드포인트 (구현 권장)

---

## 라이센스

본 프로젝트는 내부 사용 목적으로 개발되었습니다.

---

## 최근 개발 활동

**현재 브랜치**: `ai-core`
**메인 브랜치**: `main`

**최근 커밋**:
- feat: controller test 코드 수정
- feat: grpc refactor & voice 업로드 엔드포인트 추가
- feat: analysis_service test 완료
- feat: User_mission 로직 보완 및 테스트 완료
- feat: analysis_service, conversation_service 구현 완료

---

## 문의 및 지원

프로젝트 관련 문의사항은 개발팀에 문의해주세요.
