from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from dk .middleware.cors import CORSMiddleware
from pydantic import BaseModel
import json
import os
from typing import List, Optional
from datetime import datetime
from dotenv import load_dotenv
from services.openai_service import OpenAIService

# 환경 변수 로드
load_dotenv()

app = FastAPI(title="AI Core - Senior Chatbot Service", version="1.0.0")
openai_service = OpenAIService()

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

# 활성 WebSocket 연결 관리
class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
    
    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
    
    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
    
    async def send_personal_message(self, message: str, websocket: WebSocket):
        await websocket.send_text(message)

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

# WebSocket 엔드포인트 - 실시간 스트리밍 채팅
@app.websocket("/ws/chat/{user_id}")
async def websocket_chat_endpoint(websocket: WebSocket, user_id: str):
    await manager.connect(websocket)
    try:
        while True:
            # 클라이언트로부터 메시지 수신
            data = await websocket.receive_text()
            message_data = json.loads(data)
            
            user_message = message_data.get("message", "")
            chat_history = message_data.get("history", [])
            
            # AI 응답 스트리밍
            response_parts = []
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
            
            # 완료된 응답 전송
            full_response = "".join(response_parts)
            await manager.send_personal_message(
                json.dumps({
                    "type": "complete",
                    "content": full_response,
                    "timestamp": datetime.now().isoformat()
                }), 
                websocket
            )
            
    except WebSocketDisconnect:
        manager.disconnect(websocket)
    except Exception as e:
        await manager.send_personal_message(
            json.dumps({
                "type": "error",
                "content": "연결 중 오류가 발생했습니다. 다시 시도해주세요.",
                "timestamp": datetime.now().isoformat()
            }), 
            websocket
        )
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
    uvicorn.run(app, host="0.0.0.0", port=8001)