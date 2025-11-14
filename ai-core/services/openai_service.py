import openai
import logging
import os
from typing import List, AsyncGenerator, Dict, Optional
import asyncio
import json
from .conversation_analyzer import ConversationAnalyzer
from dotenv import load_dotenv


load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
class OpenAIService:
    def __init__(self):
        self.client = openai.AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.model = "gpt-4o-mini"
        self.conversation_analyzer = ConversationAnalyzer()
        self.logger = logging.getLogger(__name__)
        
        # 간결한 시니어 맞춤 시스템 프롬프트 (토큰 절약)
        self.base_system_prompt = """
    시니어용 AI 도우미입니다. 존댓말로 간결하고 따뜻하게 답변하며, 의료/법률 조언시 전문가 상담을 권유합니다.
        """.strip()
    
    async def stream_chat_response(
        self,
        message: str,
        chat_history: List[dict] = None
    ) -> AsyncGenerator[str, None]:
        """스트리밍 채팅 응답 생성"""
        try:
            system_prompt = self.base_system_prompt
            
            messages = [{"role": "system", "content": system_prompt}]
            
            # 채팅 히스토리 추가 (최근 10개 메시지만)
            if chat_history:
                messages.extend(chat_history[-10:])
            
            # 새로운 사용자 메시지 추가
            messages.append({"role": "user", "content": message})
            
            # OpenAI API 스트리밍 호출
            stream = await self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                stream=True,
                max_tokens=300,
                temperature=0.3
            )
            
            async for chunk in stream:
                if chunk.choices[0].delta.content is not None:
                    yield chunk.choices[0].delta.content
                    
        except Exception as e:
            self.logger.exception("[OpenAIService][stream_chat_response] 에러 발생")
            error_msg = "죄송합니다. 현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
            yield error_msg
    
    async def get_chat_response(self, message: str, chat_history: List[dict] = None) -> str:
        """일반 채팅 응답 생성 (비스트리밍)"""
        try:
            messages = [{"role": "system", "content": self.base_system_prompt}]
            
            if chat_history:
                messages.extend(chat_history[-10:])
            
            messages.append({"role": "user", "content": message})
            
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                max_tokens=300,
                temperature=0.3
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            return "죄송합니다. 현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
    
    
    async def generate_conversation_summary(self, conversation_text: str, topics: List[str] = None) -> Dict:
        """대화 요약 및 인사이트 생성 (원본 메시지 저장 없이)"""
        try:
            # GPT를 이용한 대화 요약 프롬프트
            summary_prompt = f"""
다음은 시니어 사용자와 AI 도우미의 대화입니다. 이 대화를 분석하여 다음을 생성해주세요:

1. 대화 요약 (2-3문장)
2. 핵심 인사이트 (최대 3개)
3. AI 추천사항 (최대 3개)
4. 감정 상태 (positive/neutral/concerned)
5. 스트레스 레벨 (1-10)
6. 주요 주제들
7. 건강 관련 언급사항

대화 내용:
{conversation_text}

응답은 반드시 다음 JSON 형식으로만 답변하세요:
{{
    "summary": "대화 요약",
    "insights": ["인사이트1", "인사이트2"],
    "recommendations": ["추천사항1", "추천사항2"],
    "mood": "positive|neutral|concerned",
    "stress_level": 5,
    "topics": ["주제1", "주제2"],
    "health_mentions": ["건강언급1", "건강언급2"]
}}
            """.strip()
            
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": summary_prompt}],
                max_tokens=500,
                temperature=0.1
            )
            
            # JSON 파싱
            response_text = response.choices[0].message.content.strip()
            
            # JSON 부분만 추출 (```json 마크다운 제거)
            if "```json" in response_text:
                json_start = response_text.find("```json") + 7
                json_end = response_text.find("```", json_start)
                response_text = response_text[json_start:json_end].strip()
            elif "{" in response_text and "}" in response_text:
                json_start = response_text.find("{")
                json_end = response_text.rfind("}") + 1
                response_text = response_text[json_start:json_end]
            
            analysis = json.loads(response_text)
            
            return {
                "conversation_summary": analysis.get("summary", "대화 요약을 생성했습니다."),
                "key_insights": analysis.get("insights", []),
                "ai_recommendations": analysis.get("recommendations", []),
                "mood_analysis": analysis.get("mood", "neutral"),
                "stress_level": analysis.get("stress_level", 5),
                "main_topics": analysis.get("topics", topics if topics else ["일상"]),
                "health_mentions": analysis.get("health_mentions", [])
            }
            
        except Exception as e:
            # 기본값 반환
            return {
                "conversation_summary": "대화가 진행되었습니다.",
                "key_insights": ["사용자와 유의미한 대화를 나누었습니다"],
                "ai_recommendations": ["앞으로도 궁금한 것이 있으면 언제든 물어보세요"],
                "mood_analysis": "neutral",
                "stress_level": 5,
                "main_topics": topics if topics else ["일상"],
                "health_mentions": []
            }