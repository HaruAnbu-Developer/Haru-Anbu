# Call Manager 설정 가이드

## 1. 사전 준비

### 필수 요구사항
- Java 17+
- MySQL 8.0+
- Gradle 8.5+
- Twilio 계정

### 선택 요구사항
- Docker & Docker Compose
- Python 3.9+ (AI 서비스용)

## 2. 환경 변수 설정

`.env` 파일을 프로젝트 루트(`CallManager/`)에 생성:

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/haru_call
DB_USERNAME=root
DB_PASSWORD=your_password
DB_HOST=localhost
DB_PORT=3306
DB_NAME=haru_call

# Twilio Configuration
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890
WEBHOOK_BASE_URL=https://your-domain.com

# gRPC AI Service Configuration
GRPC_AI_SERVICE_HOST=localhost
GRPC_AI_SERVICE_PORT=50051

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Logging
LOGGING_LEVEL_COM_HARU_ANBU=DEBUG
LOGGING_LEVEL_COM_TWILIO=DEBUG
LOGGING_LEVEL_IO_GRPC=INFO
```

## 3. 데이터베이스 설정

### MySQL 데이터베이스 생성
```sql
CREATE DATABASE haru_call CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 자동 스키마 생성
Spring Boot의 `spring.jpa.hibernate.ddl-auto=update` 설정으로 자동 생성됩니다.

## 4. 프로젝트 빌드

### Gradle을 이용한 빌드
```bash
cd CallManager
./gradlew clean build -x test
```

### Proto 파일 컴파일 확인
```bash
# Proto 파일이 컴파일되어 Java 코드 생성
ls -la build/generated/source/proto/main/
```

## 5. 애플리케이션 실행

### 로컬 실행
```bash
./gradlew bootRun
```

### JAR 파일로 실행
```bash
java -jar build/libs/CallManager-0.0.1-SNAPSHOT.jar
```

### Docker로 실행
```bash
docker-compose up --build
```

## 6. API 테스트

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### 통화 시작
```bash
curl -X POST http://localhost:8080/api/calls/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "phoneNumber": "+821012345678",
    "metadata": {
      "purpose": "welfare_check"
    }
  }'
```

### 세션 조회
```bash
curl http://localhost:8080/api/calls/sessions/{sessionId}
```

## 7. Twilio Webhook 설정

Twilio Console에서 다음 URL을 설정:

1. **Voice URL**: `https://your-domain.com/api/calls/twilio/voice`
2. **Status Callback URL**: `https://your-domain.com/api/calls/twilio/status`
3. **Recording Callback URL**: `https://your-domain.com/api/calls/twilio/recording`

### ngrok을 이용한 로컬 테스트
```bash
# ngrok 설치
brew install ngrok  # macOS
# or
choco install ngrok  # Windows

# ngrok 실행
ngrok http 8080

# ngrok URL을 WEBHOOK_BASE_URL로 사용
# 예: https://abc123.ngrok.io
```

## 8. AI gRPC 서버 설정

Python AI 서버를 별도로 실행해야 합니다.

### Python 프로젝트 구조 (예시)
```
ai-service/
├── requirements.txt
├── server.py
└── protos/
    └── ai_service.proto
```

### requirements.txt
```
grpcio==1.71.0
grpcio-tools==1.71.0
protobuf==3.25.1
```

### Python gRPC 서버 실행
```bash
cd ai-service
python -m grpc_tools.protoc \
  -I./protos \
  --python_out=. \
  --grpc_python_out=. \
  ./protos/ai_service.proto

python server.py
```

## 9. 트러블슈팅

### gRPC 연결 실패
```bash
# AI 서비스가 실행 중인지 확인
netstat -an | grep 50051

# 방화벽 설정 확인
# 로컬 환경에서는 localhost:50051 접근 가능해야 함
```

### Proto 컴파일 오류
```bash
# Gradle 캐시 정리
./gradlew clean
rm -rf build/

# 다시 빌드
./gradlew build -x test
```

### 데이터베이스 연결 실패
```bash
# MySQL 실행 확인
mysql -u root -p -e "SELECT 1"

# 데이터베이스 존재 확인
mysql -u root -p -e "SHOW DATABASES"
```

### Twilio Webhook 실패
```bash
# ngrok 로그 확인
# Twilio Console의 Debug 로그 확인
# 애플리케이션 로그 확인
tail -f logs/call-manager.log
```

## 10. 프로덕션 배포

### 환경 변수 설정
프로덕션 환경에서는 `.env` 대신 시스템 환경 변수 사용 권장

### application.yml 프로파일
```bash
# 프로덕션 프로파일로 실행
java -jar -Dspring.profiles.active=prod app.jar
```

### HTTPS 설정
프로덕션에서는 반드시 HTTPS 사용:
- Twilio Webhook은 HTTPS만 지원
- gRPC는 TLS 설정 권장

### 모니터링
- Actuator 엔드포인트 활용
- Prometheus + Grafana 연동
- 로그 집계 (ELK Stack 등)

## 11. 다음 단계

1. ✅ Java 백엔드 구현 완료
2. ⏳ Python AI gRPC 서버 구현
3. ⏳ WebSocket 핸들러 구현 (Twilio Media Stream)
4. ⏳ 통합 테스트
5. ⏳ 프론트엔드 대시보드