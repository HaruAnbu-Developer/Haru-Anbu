from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import json
import os
from typing import List, Optional, Dict
from datetime import datetime
from dotenv import load_dotenv
from services.openai_service import OpenAIService
from jose import JWTError, jwt
import logging
import uuid

# 환경 변수 로드
load_dotenv()

app = FastAPI(title="AI Core - Senior Chatbot Service", version="1.0.0")
openai_service = OpenAIService()
logger = logging.getLogger(__name__)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발 환경용, 프로덕션에서는 제한
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 환경 변수
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "your-secret-key-change-in-production")
JWT_ALGORITHM = "HS256"

# JWT 토큰 검증 함수
def verify_jwt_token(token: str) -> Optional[int]:
    """JWT 토큰 검증 및 user_id 추출"""
    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])
        user_id = payload.get("user_id")
        if user_id is None:
            return None
        return int(user_id)
    except JWTError as e:
        logger.error(f"JWT 검증 실패: {e}")
        return None
    except Exception as e:
        logger.error(f"토큰 검증 중 오류: {e}")
        return None

# 활성 WebSocket 연결 관리
class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[WebSocket, Dict] = {}  # {websocket: {user_id, session_id, messages}}

    async def connect(self, websocket: WebSocket, user_id: int):
        await websocket.accept()
        session_id = str(uuid.uuid4())
        self.active_connections[websocket] = {
            "user_id": user_id,
            "session_id": session_id,
            "messages": [],
            "connected_at": datetime.now()
        }
        logger.info(f"사용자 {user_id} 연결됨 (세션: {session_id})")

    def disconnect(self, websocket: WebSocket) -> Dict:
        """연결 해제 및 세션 데이터 반환"""
        if websocket in self.active_connections:
            session_data = self.active_connections.pop(websocket)
            logger.info(f"사용자 {session_data['user_id']} 연결 해제 (세션: {session_data['session_id']})")
            return session_data
        return None

    async def send_personal_message(self, message: str, websocket: WebSocket):
        await websocket.send_text(message)

    def get_session_data(self, websocket: WebSocket) -> Optional[Dict]:
        """WebSocket의 세션 데이터 조회"""
        return self.active_connections.get(websocket)

    def add_message(self, websocket: WebSocket, role: str, content: str):
        """대화 메시지 저장"""
        if websocket in self.active_connections:
            self.active_connections[websocket]["messages"].append({
                "role": role,
                "content": content,
                "timestamp": datetime.now().isoformat()
            })

manager = ConnectionManager()

# 데이터 모델
class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None
    user_id: str

class ChatResponse(BaseModel):
    message: str
    session_id: str
    timestamp: datetime

class ConversationSummaryRequest(BaseModel):
    conversation_text: str
    user_id: int
    session_title: str
    total_messages: int
    duration_minutes: int
    topics: Optional[List[str]] = None

class ConversationSummaryResponse(BaseModel):
    conversation_summary: str
    key_insights: List[str]
    ai_recommendations: List[str]
    mood_analysis: str
    stress_level: int
    main_topics: List[str]
    health_mentions: List[str]

@app.get("/")
async def root():
    return {"message": "AI Core - Senior Chatbot Service"}

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "ai-core"}

@app.get("/view")
async def view_connections():
    """현재 활성 연결 및 세션 데이터 조회 (디버깅용)"""
    connections_data = []

    for idx, (websocket, session_data) in enumerate(manager.active_connections.items()):
        connections_data.append({
            "connection_id": idx,
            "user_id": session_data["user_id"],
            "session_id": session_data["session_id"],
            "messages_count": len(session_data["messages"]),
            "connected_at": session_data["connected_at"].isoformat(),
            "messages": session_data["messages"]  # 전체 메시지 표시
        })

    return {
        "total_connections": len(connections_data),
        "connections": connections_data,
        "jwt_secret_key": JWT_SECRET_KEY,
        "timestamp": datetime.now().isoformat()
    }

# WebSocket 엔드포인트 - 실시간 스트리밍 채팅
@app.websocket("/ws/chat")
async def websocket_chat_endpoint(websocket: WebSocket, token: str):
    """JWT 토큰으로 인증하고 실시간 채팅 제공"""

    # JWT 토큰 검증
    user_id = verify_jwt_token(token)
    if user_id is None:
        await websocket.close(code=4001, reason="Invalid or missing token")
        logger.warning("토큰 검증 실패로 연결 거부")
        return

    # 연결 수락
    await manager.connect(websocket, user_id)

    try:
        while True:
            # 클라이언트로부터 메시지 수신
            try:
                data = await websocket.receive_text()
                logger.info(f"📨 원본 데이터 수신: {data}")

                message_data = json.loads(data)
                logger.info(f"📦 파싱된 메시지: {message_data}")

                user_message = message_data.get("message", "").strip()
                if not user_message:
                    logger.warning("⚠️ 메시지가 비어있음")
                    continue

                logger.info(f"💬 사용자 메시지: {user_message}")

                # 사용자 메시지 저장
                manager.add_message(websocket, "user", user_message)

            except json.JSONDecodeError as e:
                logger.error(f"❌ JSON 파싱 실패: {e}, 원본: {data}")
                try:
                    await manager.send_personal_message(
                        json.dumps({
                            "type": "error",
                            "content": "메시지 형식이 잘못되었습니다. JSON 형식으로 전송해주세요.",
                            "timestamp": datetime.now().isoformat()
                        }),
                        websocket
                    )
                except:
                    pass
                continue
            except WebSocketDisconnect:
                raise  # 밖으로 전파
            except Exception as e:
                logger.error(f"❌ 메시지 처리 중 오류: {e}")
                continue

            # 세션 데이터에서 메시지 히스토리 가져오기
            session_data = manager.get_session_data(websocket)
            chat_history = session_data["messages"][:-1] if session_data else []  # 현재 메시지 제외

            # AI 응답 스트리밍
            response_parts = []
            try:
                async for chunk in openai_service.stream_chat_response(user_message, chat_history):
                    response_parts.append(chunk)
                    # 실시간으로 청크 전송
                    await manager.send_personal_message(
                        json.dumps({
                            "type": "chunk",
                            "content": chunk,
                            "timestamp": datetime.now().isoformat()
                        }),
                        websocket
                    )
            except Exception as e:
                logger.error(f"AI 응답 생성 중 오류: {e}")
                await manager.send_personal_message(
                    json.dumps({
                        "type": "error",
                        "content": "죄송합니다. 응답을 생성할 수 없습니다.",
                        "timestamp": datetime.now().isoformat()
                    }),
                    websocket
                )
                continue

            # 완료된 응답 저장
            full_response = "".join(response_parts)
            manager.add_message(websocket, "assistant", full_response)

            # 완료 신호 전송
            await manager.send_personal_message(
                json.dumps({
                    "type": "complete",
                    "content": full_response,
                    "timestamp": datetime.now().isoformat()
                }),
                websocket
            )

    except WebSocketDisconnect:
        session_data = manager.disconnect(websocket)
        if session_data:
            # 세션 종료시 대화 저장 (나중에 DB 로직으로 변경)
            logger.info(f"세션 종료 - 사용자: {session_data['user_id']}, 메시지 수: {len(session_data['messages'])}")
            # TODO: 대화 분석 및 저장 로직 추가

    except Exception as e:
        logger.error(f"WebSocket 처리 중 오류: {e}")
        try:
            await manager.send_personal_message(
                json.dumps({
                    "type": "error",
                    "content": "연결 중 오류가 발생했습니다. 다시 시도해주세요.",
                    "timestamp": datetime.now().isoformat()
                }),
                websocket
            )
        except:
            pass
        manager.disconnect(websocket)

# REST API 엔드포인트 - 일반 채팅
@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    try:
        # 간단한 채팅 히스토리 (실제로는 DB에서 가져와야 함)
        chat_history = []
        
        # AI 응답 생성
        response = await openai_service.get_chat_response(
            request.message, 
            chat_history
        )
        
        return ChatResponse(
            message=response,
            session_id=request.session_id or "default",
            timestamp=datetime.now()
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail="채팅 처리 중 오류가 발생했습니다.")

# 대화 요약 생성 API
@app.post("/conversation/summary", response_model=ConversationSummaryResponse)
async def generate_conversation_summary(request: ConversationSummaryRequest):
    """대화 요약 및 인사이트 생성"""
    try:
        # GPT를 이용한 대화 요약 생성
        summary_data = await openai_service.generate_conversation_summary(
            request.conversation_text, 
            request.topics
        )
        
        return ConversationSummaryResponse(**summary_data)
        
    except Exception as e:
        raise HTTPException(
            status_code=500, 
            detail=f"대화 요약 생성 중 오류가 발생했습니다: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8001))
    uvicorn.run(app, host="0.0.0.0", port=port)