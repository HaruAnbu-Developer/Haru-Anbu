import httpx
import os
import json
import logging
from typing import Dict, List, Optional

logger = logging.getLogger(__name__)

class BackendService:
    def __init__(self):
        self.base_url = os.getenv("SPRING_BACKEND_URL", "https://cheongchun-backend-40635111975.asia-northeast3.run.app")
        self.timeout = httpx.Timeout(30.0)
    
    async def get_user_ai_profile(self, token: str) -> Optional[Dict]:
        """사용자 AI 프로필 조회"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/ai/profile",
                    headers={"Authorization": f"Bearer {token}"}
                )
                if response.status_code == 200:
                    return response.json()
                else:
                    logger.warning(f"AI 프로필 조회 실패: {response.status_code}")
                    return None
        except Exception as e:
            logger.error(f"AI 프로필 조회 오류: {e}")
            return None
    
    async def update_user_ai_profile(self, token: str, profile_data: Dict) -> bool:
        """사용자 AI 프로필 업데이트"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.put(
                    f"{self.base_url}/ai/profile",
                    headers={
                        "Authorization": f"Bearer {token}",
                        "Content-Type": "application/json"
                    },
                    json=profile_data
                )
                return response.status_code == 200
        except Exception as e:
            logger.error(f"AI 프로필 업데이트 오류: {e}")
            return False
    
    async def save_conversation(self, conversation_data: Dict) -> Optional[int]:
        """대화 저장"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/ai/conversation",
                    headers={"Content-Type": "application/json"},
                    json=conversation_data
                )
                if response.status_code == 200:
                    return response.json()  # conversation ID
                else:
                    logger.warning(f"대화 저장 실패: {response.status_code}")
                    return None
        except Exception as e:
            logger.error(f"대화 저장 오류: {e}")
            return None
    
    async def get_chat_history(self, user_id: int, limit: int = 10) -> List[Dict]:
        """채팅 히스토리 조회"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/ai/history/{user_id}",
                    params={"limit": limit}
                )
                if response.status_code == 200:
                    return response.json()
                else:
                    logger.warning(f"채팅 히스토리 조회 실패: {response.status_code}")
                    return []
        except Exception as e:
            logger.error(f"채팅 히스토리 조회 오류: {e}")
            return []
    
    async def get_user_insights(self, user_id: int) -> Optional[Dict]:
        """사용자 인사이트 조회"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(f"{self.base_url}/ai/insights/{user_id}")
                if response.status_code == 200:
                    return response.json()
                else:
                    logger.warning(f"사용자 인사이트 조회 실패: {response.status_code}")
                    return None
        except Exception as e:
            logger.error(f"사용자 인사이트 조회 오류: {e}")
            return None
    
    async def verify_token(self, token: str) -> Optional[Dict]:
        """JWT 토큰 검증"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/auth/verify",
                    headers={"Authorization": f"Bearer {token}"}
                )
                if response.status_code == 200:
                    return response.json()
                else:
                    logger.warning(f"토큰 검증 실패: {response.status_code}")
                    return None
        except Exception as e:
            logger.error(f"토큰 검증 오류: {e}")
            return None