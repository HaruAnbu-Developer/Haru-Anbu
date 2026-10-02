# Haru-Anbu AI Core

독거 노인을 위한 AI 음성 안부 전화 시스템의 **AI 파이프라인(ai-core)** 입니다. 가족의 목소리를 복제한 음성으로 어르신과 실시간 대화를 나누고, 통화 기록을 분석해 보호자용 리포트와 다음 통화의 맥락을 만듭니다.

> **담당 범위**: 이 브랜치(`ai-core`)의 AI 파이프라인 전체 — 실시간 음성 대화(VAD·STT·LLM·TTS), 음성 클로닝, 통화 분석, 일일 질문·라디오 생성, AI 전용 DB 스키마 — 는 조남웅([@Namung2](https://github.com/Namung2))이 설계·구현했습니다. 백엔드 API 서버와 앱은 팀원이 담당했습니다 (`main`, `callManager`, `frontend` 브랜치).
> **개발 기간**: 2025.11 – 2026.03 · 팀 프로젝트 (HaruAnbu-Developer)
> **상태**: 기능 구현 및 단일 사용자 테스트 완료. 실사용자 서비스 단계는 아닙니다.

## Pipeline Overview

<img width="1890" height="861" alt="Image" src="https://github.com/user-attachments/assets/f5d6b26a-f248-4b84-a567-5cdccdf50965" />

실시간 통화는 gRPC 양방향 스트림 하나로 처리됩니다. 클라이언트가 16 kHz PCM 오디오를 보내면 VAD가 발화 구간을 잘라 STT에 넘기고, LLM이 문장 단위로 답변을 생성하는 즉시 TTS가 합성해 24 kHz 오디오로 돌려보냅니다. 통화가 끝나면 대화 로그가 S3에 저장되고, 자정 배치가 이를 분석해 DB에 결과를 남깁니다.

## Components

### Real-time call (`services/call_service/`, `services/stt/`, `services/llm/`, `services/tts/`)
- **VAD**: Silero VAD로 1024-sample 윈도우마다 발화 시작/종료를 판정합니다. 0.5초 미만의 짧은 소리는 잡음으로 버립니다.
- **STT**: Faster-Whisper(한국어). 환각 억제를 위해 `condition_on_previous_text=False`, 반복 페널티, 낮은 temperature, 도메인 initial prompt를 사용합니다.
- **LLM**: Gemma-2-9B-IT(Q5_K_M GGUF)를 llama-cpp로 로컬 구동합니다. 토큰 스트림을 문장 단위로 끊어 TTS로 넘겨 체감 지연을 줄입니다. 분석용으로는 JSON 전용 출력 모드를 따로 둡니다.
- **TTS / 음성 클로닝**: OpenVoice V2. 기본 화자로 음성을 만든 뒤 tone color converter로 가족 목소리의 임베딩을 입혀 출력합니다. 사용자별 임베딩(`.pth`)은 통화 시작 시 S3에서 GPU 메모리로 올리고 종료 시 해제합니다 (`voice_training_service/`).
- **ConversationManager**: DB에서 그날의 개인화 질문(미션)을 불러와 LLM 지시문에 끼워 넣고, LLM이 답변 앞에 붙이는 `[1]` 태그로 질문 수행 여부를 판정합니다. 통화 로그를 모아 종료 시 S3에 업로드합니다.

### Daily batch (`services/emotion_analysis_service/`, `services/radio_service/`)
APScheduler가 매일 00:00에 아래 순서로 실행합니다.
1. **통화 분석**: 전날 S3 로그를 LLM에 넣어 JSON 형태의 점수(인지 지표 5종, 종합 점수, 위험도), 건강 키워드, 보호자용 요약, 다음 통화용 기억 한 줄을 뽑아 DB에 저장합니다.
2. **기억·미션 갱신**: 분석 결과로 `UserMemory`를 쌓고 완료된 `UserMission`을 처리합니다.
3. **질문 생성**: 전체 공통 질문 하나와 사용자별 개인화 질문을 LLM으로 생성합니다.
4. **라디오 생성**: 사용자들의 공통 질문 답변을 모아 라디오 대본을 쓰고 TTS로 합성해 S3에 올립니다.

### Management API (`server/connect_back/`)
FastAPI. 음성 파일 업로드, 임베딩 추출(백그라운드), 통화 전 GPU 메모리 적재/해제, 배치 수동 실행 엔드포인트를 제공합니다. 백엔드 서버가 이 API를 호출합니다.

## Data

AI 전용 MySQL 스키마는 `database/schema.py`에 있습니다.

| 테이블 | 용도 |
|---|---|
| `VoiceProfile` | 사용자별 원본 음성·임베딩 경로, 처리 상태 |
| `UserMemory` | 통화별 한 줄 기억 (다음 통화 오프닝에 사용) |
| `UserMission` | 사용자별 일일 질문과 완료 여부 |
| `ConversationAnalysis` | 통화 분석 결과 (점수, 요약, 건강 키워드) |
| `DailyQuestion`, `CommunityRadioTopic` | 공통 질문과 라디오용 답변 |

S3에는 통화 로그(JSON), 사용자 음성 임베딩, 라디오 오디오가 저장됩니다.

## Repository Structure

```
ai-core/
├── server/
│   ├── grpc/                  # voice_stream.proto 및 생성 코드
│   └── connect_back/          # FastAPI 관리 API
├── services/
│   ├── call_service/          # gRPC 통화 서버 (파이프라인 오케스트레이션)
│   ├── stt/                   # Faster-Whisper, Silero VAD
│   ├── llm/                   # Gemma-2 서비스, ConversationManager
│   ├── tts/                   # OpenVoice V2 합성·클로닝
│   ├── voice_training_service/ # 임베딩 추출, GPU 메모리 관리
│   ├── emotion_analysis_service/ # 통화 분석 배치
│   └── radio_service/         # 질문 생성, 라디오 대본·합성, 스케줄러
├── database/                  # SQLAlchemy 엔진, 스키마
├── checkpoints/               # OpenVoice 설정 (가중치는 별도 다운로드)
├── test/                      # GPU, S3, 샘플 오디오 테스트
├── mic_to_grpc.py             # 마이크 입력 gRPC 클라이언트 (수동 테스트용)
└── requirements.txt
```

## Running

**요구 사항**: Python 3.11, CUDA GPU (VRAM 12 GB 이상 권장 — Gemma-2-9B Q5 + Whisper + OpenVoice 동시 적재), MySQL, AWS S3 버킷

```bash
cd ai-core
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

`.env`에 DB 접속 정보(`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`)와 S3 정보(`S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`, `S3_REGION`, `S3_BUCKET_NAME`)를 넣습니다.

모델 파일은 레포에 포함되어 있지 않습니다.
- Gemma-2-9B-IT GGUF(Q5_K_M) → `models/llm/gemma-2-9b-it-Q5_K_M.gguf`
- OpenVoice V2 converter 체크포인트 → `checkpoints/converter/checkpoint.pth`
- Faster-Whisper 모델은 첫 실행 시 자동 다운로드됩니다.

```bash
python -c "from database.database import init_db; init_db()"     # 테이블 생성
python services/call_service/call_service.py                      # gRPC 통화 서버 :50051
uvicorn server.connect_back.controller:app --port 8000            # 관리 API
python services/radio_service/scheduler.py                        # 일일 배치
python mic_to_grpc.py                                             # 마이크로 통화 테스트
```

## Limitations

- **단일 세션 구조**: GPU 한 장에 세 모델을 모두 올리는 환경을 전제로 설계해, 추론이 비동기 서버의 이벤트 루프 안에서 동기적으로 실행되고 VAD·LLM 서비스가 싱글톤입니다. 동시 통화를 지원하려면 모델 서버 분리와 세션별 상태 관리가 필요합니다.
- **분석 결과는 검증되지 않은 LLM 기반 휴리스틱입니다.** 인지 지표와 위험도는 프롬프트로 산출한 값이며 임상적 근거가 없습니다. 보호자 참고용 요약 이상의 의미를 두어서는 안 됩니다.
- **LLM 문맥**: 턴마다 지시문과 현재 발화만 모델에 전달됩니다. 이전 턴의 맥락은 ConversationManager가 지시문에 넣는 직전 발화 한 줄과, 전날 분석에서 나온 기억 요약으로만 이어집니다.
- **배치 중복**: 일일 분석은 "최근 24시간 내 수정된 로그"를 대상으로 하며 처리 완료 표시가 없어, 배치가 두 번 실행되면 같은 통화가 중복 분석될 수 있습니다.
- **전송 보안**: gRPC는 TLS 없이(`insecure_port`) 동작합니다. 실서비스에는 TLS와 인증이 필요합니다.
- Windows 환경에서 개발되어 `call_service.py` 상단에 CUDA 라이브러리 경로를 가상환경에서 찾는 코드가 있습니다. 다른 환경에서는 해당 코드가 아무 동작도 하지 않습니다.

## Author

조남웅 (Namwoong Cho) — 단국대학교 컴퓨터공학과
AI core 설계·구현 (2025.11 – 2026.03)
