import openai
import re
import json
import os
from typing import Dict, List, Tuple
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

class ConversationAnalyzer:
    def __init__(self):
        self.client = openai.AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        
        # 한국어 키워드 매핑
        self.health_keywords = [
            "건강", "혈압", "당뇨", "콜레스테롤", "운동", "다이어트", "약물", "병원", "의사", 
            "검진", "아픈", "통증", "두통", "허리", "무릎", "관절", "소화", "식이요법", 
            "영양", "비타민", "수면", "잠", "피로", "스트레스", "우울", "불안"
        ]
        
        self.family_keywords = [
            "가족", "자녀", "손자", "손녀", "배우자", "남편", "아내", "딸", "아들", 
            "며느리", "사위", "친구", "이웃", "외로움", "그리움", "만남"
        ]
        
        self.hobby_keywords = [
            "취미", "독서", "산책", "등산", "요리", "원예", "화초", "텃밭", "여행", 
            "드라마", "영화", "음악", "노래", "춤", "바둑", "장기", "카드게임"
        ]
        
        self.tech_keywords = [
            "스마트폰", "컴퓨터", "인터넷", "카카오톡", "문자", "전화", "앱", "유튜브", 
            "온라인", "배송", "온라인쇼핑", "인터넷뱅킹", "키오스크"
        ]
    
    async def analyze_conversation(self, messages: List[Dict], user_profile: Dict = None) -> Dict:
        """대화 전체를 분석하여 인사이트 생성"""
        try:
            # 메시지들을 텍스트로 결합
            conversation_text = self._extract_conversation_text(messages)
            
            # 키워드 기반 빠른 분석
            quick_analysis = self._quick_keyword_analysis(conversation_text)
            
            # OpenAI를 활용한 심층 분석
            deep_analysis = await self._deep_ai_analysis(conversation_text, user_profile)
            
            # 결과 통합
            final_analysis = {
                **quick_analysis,
                **deep_analysis,
                "analyzed_at": datetime.now().isoformat(),
                "total_messages": len(messages),
                "conversation_length": len(conversation_text)
            }
            
            return final_analysis
            
        except Exception as e:
            logger.error(f"대화 분석 실패: {e}")
            return self._get_fallback_analysis(messages)
    
    def _extract_conversation_text(self, messages: List[Dict]) -> str:
        """메시지에서 대화 텍스트 추출"""
        user_messages = []
        for msg in messages:
            if msg.get("role") == "user":
                user_messages.append(msg.get("content", ""))
        return " ".join(user_messages)
    
    def _quick_keyword_analysis(self, text: str) -> Dict:
        """키워드 기반 빠른 분석 (오프라인 처리)"""
        analysis = {
            "main_topics": [],
            "health_mentions": [],
            "concerns_discussed": [],
            "detected_categories": {}
        }
        
        # 카테고리별 키워드 매칭
        categories = {
            "health": self.health_keywords,
            "family": self.family_keywords, 
            "hobby": self.hobby_keywords,
            "technology": self.tech_keywords
        }
        
        for category, keywords in categories.items():
            matched_keywords = [kw for kw in keywords if kw in text]
            if matched_keywords:
                analysis["detected_categories"][category] = matched_keywords
                
                if category == "health":
                    analysis["health_mentions"].extend(matched_keywords)
                    analysis["main_topics"].append("건강")
                elif category == "family":
                    analysis["main_topics"].append("가족")
                elif category == "hobby":
                    analysis["main_topics"].append("취미")
                elif category == "technology":
                    analysis["main_topics"].append("기술")
        
        # 감정 키워드 분석
        concern_keywords = ["걱정", "불안", "외로운", "힘들다", "아프다", "우울", "스트레스"]
        detected_concerns = [kw for kw in concern_keywords if kw in text]
        if detected_concerns:
            analysis["concerns_discussed"] = detected_concerns
        
        return analysis
    
    async def _deep_ai_analysis(self, text: str, user_profile: Dict = None) -> Dict:
        """OpenAI를 활용한 심층 분석"""
        try:
            profile_context = ""
            if user_profile:
                profile_context = f"사용자 정보: 나이대 {user_profile.get('ageGroup', '시니어')}, 관심사: {user_profile.get('interests', [])}"
            
            analysis_prompt = f"""
다음은 시니어와 AI의 대화 내용입니다. 이 대화를 분석해주세요.

{profile_context}

대화 내용:
{text[:2000]}  # 텍스트 길이 제한

다음 JSON 형식으로 분석 결과를 제공해주세요:
{{
    "conversation_summary": "대화의 핵심 내용을 2-3문장으로 요약",
    "mood_analysis": "positive/neutral/concerned/sad 중 하나",
    "stress_level": 1-10 사이의 숫자,
    "key_insights": ["인사이트1", "인사이트2", "인사이트3"],
    "ai_recommendations": ["추천사항1", "추천사항2", "추천사항3"],
    "session_title": "이 대화의 제목을 10자 이내로"
}}

응답은 반드시 유효한 JSON 형식이어야 합니다.
            """
            
            response = await self.client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "당신은 시니어 대화를 분석하는 전문가입니다. JSON 형식으로만 응답하세요."},
                    {"role": "user", "content": analysis_prompt}
                ],
                max_tokens=800,
                temperature=0.3
            )
            
            analysis_text = response.choices[0].message.content.strip()
            
            # JSON 파싱 시도
            try:
                return json.loads(analysis_text)
            except json.JSONDecodeError:
                # JSON 파싱 실패 시 패턴 매칭으로 추출 시도
                return self._extract_analysis_from_text(analysis_text)
                
        except Exception as e:
            logger.error(f"AI 심층 분석 실패: {e}")
            return {
                "conversation_summary": "대화 분석이 완료되었습니다.",
                "mood_analysis": "neutral",
                "stress_level": 5,
                "key_insights": ["AI와 유익한 대화를 나누셨습니다."],
                "ai_recommendations": ["건강한 생활습관을 유지하세요."],
                "session_title": "AI 대화"
            }
    
    def _extract_analysis_from_text(self, text: str) -> Dict:
        """텍스트에서 분석 결과를 패턴 매칭으로 추출"""
        analysis = {}
        
        # 간단한 패턴 매칭
        summary_match = re.search(r'"conversation_summary":\s*"([^"]+)"', text)
        if summary_match:
            analysis["conversation_summary"] = summary_match.group(1)
        
        mood_match = re.search(r'"mood_analysis":\s*"([^"]+)"', text)
        if mood_match:
            analysis["mood_analysis"] = mood_match.group(1)
        
        # 기본값 설정
        analysis.setdefault("conversation_summary", "유익한 대화를 나누셨습니다.")
        analysis.setdefault("mood_analysis", "neutral")
        analysis.setdefault("stress_level", 5)
        analysis.setdefault("key_insights", ["AI와 좋은 대화 시간을 가지셨습니다."])
        analysis.setdefault("ai_recommendations", ["꾸준한 대화를 통해 건강한 일상을 유지하세요."])
        analysis.setdefault("session_title", "AI 대화")
        
        return analysis
    
    def _get_fallback_analysis(self, messages: List[Dict]) -> Dict:
        """분석 실패시 기본 결과 반환"""
        return {
            "main_topics": ["일반대화"],
            "health_mentions": [],
            "concerns_discussed": [],
            "mood_analysis": "neutral",
            "stress_level": 5,
            "conversation_summary": f"{len(messages)}개의 메시지로 AI와 대화를 나누셨습니다.",
            "key_insights": ["AI 도우미와 유익한 시간을 보내셨습니다."],
            "ai_recommendations": ["규칙적인 대화를 통해 활기찬 하루를 만들어보세요."],
            "session_title": "AI 채팅",
            "analyzed_at": datetime.now().isoformat(),
            "total_messages": len(messages)
        }