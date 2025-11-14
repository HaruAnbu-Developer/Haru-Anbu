#!/usr/bin/env python3
"""
시니어 맞춤 AI 챗봇 시스템 테스트 스크립트
"""

import asyncio
import json
import os
import httpx
from datetime import datetime

# 환경 변수 로드
from dotenv import load_dotenv
load_dotenv()

BACKEND_URL = os.getenv("SPRING_BACKEND_URL", "https://cheongchun-backend-40635111975.asia-northeast3.run.app")
AI_CORE_URL = "http://localhost:8001"

class AISystemTester:
    def __init__(self):
        self.backend_url = BACKEND_URL
        self.ai_core_url = AI_CORE_URL
        self.test_token = None
        self.test_user_id = None
    
    async def run_all_tests(self):
        """모든 테스트 실행"""
        print("🚀 시니어 맞춤 AI 챗봇 시스템 테스트 시작\n")
        
        # 1. 백엔드 연결 테스트
        await self.test_backend_connection()
        
        # 2. 사용자 로그인 테스트 (실제 토큰 필요)
        await self.test_user_authentication()
        
        # 3. AI Core 서비스 테스트
        await self.test_ai_core_service()
        
        # 4. AI 프로필 관리 테스트
        await self.test_ai_profile_management()
        
        # 5. 대화 분석 테스트
        await self.test_conversation_analysis()
        
        # 6. 통합 시나리오 테스트
        await self.test_integration_scenario()
        
        print("\n✅ 모든 테스트 완료!")
    
    async def test_backend_connection(self):
        """백엔드 연결 테스트"""
        print("1️⃣ 백엔드 연결 테스트")
        try:
            async with httpx.AsyncClient() as client:
                # 헬스 체크
                response = await client.get(f"{self.backend_url}/actuator/health")
                if response.status_code == 200:
                    health_data = response.json()
                    print(f"   ✅ 백엔드 상태: {health_data.get('status', 'Unknown')}")
                else:
                    print(f"   ❌ 백엔드 연결 실패: {response.status_code}")
                    
        except Exception as e:
            print(f"   ❌ 백엔드 연결 오류: {e}")
        print()
    
    async def test_user_authentication(self):
        """사용자 인증 테스트"""
        print("2️⃣ 사용자 인증 테스트")
        
        # 실제 환경에서는 OAuth2 로그인이 필요
        # 테스트용으로 더미 데이터 사용
        print("   ⚠️  실제 테스트를 위해서는 OAuth2 로그인이 필요합니다")
        print("   💡 프론트엔드에서 로그인 후 토큰을 얻어 테스트하세요")
        
        # 테스트용 사용자 ID (실제 DB에 있는 사용자 ID 사용)
        self.test_user_id = 1  # 실제 사용자 ID로 변경 필요
        print(f"   📝 테스트 사용자 ID: {self.test_user_id}")
        print()
    
    async def test_ai_core_service(self):
        """AI Core 서비스 테스트"""
        print("3️⃣ AI Core 서비스 테스트")
        try:
            async with httpx.AsyncClient() as client:
                # 헬스 체크
                response = await client.get(f"{self.ai_core_url}/health")
                if response.status_code == 200:
                    health_data = response.json()
                    print(f"   ✅ AI Core 상태: {health_data.get('status')}")
                else:
                    print(f"   ❌ AI Core 연결 실패: {response.status_code}")
                    return
                
                # REST API 채팅 테스트
                chat_request = {
                    "message": "안녕하세요",
                    "user_id": str(self.test_user_id)
                }
                
                response = await client.post(
                    f"{self.ai_core_url}/chat",
                    json=chat_request,
                    timeout=30.0
                )
                
                if response.status_code == 200:
                    chat_response = response.json()
                    response_msg = chat_response.get('message', '')
                    if "할당량" in response_msg or "quota" in response_msg.lower():
                        print(f"   ⚠️  OpenAI API 할당량 초과됨: {response_msg[:50]}...")
                        print("   💡 OpenAI API 키의 크레딧을 확인하고 충전해주세요")
                    else:
                        print(f"   ✅ AI 채팅 응답: {response_msg[:50]}...")
                else:
                    print(f"   ❌ AI 채팅 실패: {response.status_code}")
                    if response.status_code == 500:
                        print("   💡 서버 오류 - OpenAI API 키 또는 할당량을 확인해주세요")
                    
        except Exception as e:
            print(f"   ❌ AI Core 테스트 오류: {e}")
        print()
    
    async def test_ai_profile_management(self):
        """AI 프로필 관리 테스트"""
        print("4️⃣ AI 프로필 관리 테스트")
        
        if not self.test_token:
            print("   ⚠️  JWT 토큰이 필요합니다. 실제 로그인 후 테스트하세요")
            print()
            return
        
        try:
            async with httpx.AsyncClient() as client:
                # AI 프로필 업데이트
                profile_data = {
                    "ageGroup": "70-75",
                    "healthProfile": json.dumps({
                        "concerns": ["혈압", "당뇨", "관절염"],
                        "medications": ["혈압약", "당뇨약"]
                    }),
                    "interests": json.dumps(["건강", "요리", "산책", "가족"]),
                    "conversationStyle": "formal"
                }
                
                response = await client.put(
                    f"{self.backend_url}/ai/profile",
                    headers={"Authorization": f"Bearer {self.test_token}"},
                    json=profile_data
                )
                
                if response.status_code == 200:
                    print("   ✅ AI 프로필 업데이트 성공")
                else:
                    print(f"   ❌ AI 프로필 업데이트 실패: {response.status_code}")
                
                # AI 프로필 조회
                response = await client.get(
                    f"{self.backend_url}/ai/profile",
                    headers={"Authorization": f"Bearer {self.test_token}"}
                )
                
                if response.status_code == 200:
                    profile = response.json()
                    print(f"   ✅ AI 프로필 조회 성공: {profile.get('ageGroup')}")
                else:
                    print(f"   ❌ AI 프로필 조회 실패: {response.status_code}")
                    
        except Exception as e:
            print(f"   ❌ AI 프로필 테스트 오류: {e}")
        print()
    
    async def test_conversation_analysis(self):
        """대화 분석 테스트"""
        print("5️⃣ 대화 분석 테스트")
        
        try:
            async with httpx.AsyncClient() as client:
                # 테스트 대화 데이터
                conversation_data = {
                    "userId": self.test_user_id,
                    "sessionTitle": "건강 상담 테스트",
                    "totalMessages": 6,
                    "durationMinutes": 10,
                    "messagesJson": json.dumps([
                        {"role": "user", "content": "요즘 혈압이 높아서 걱정이에요"},
                        {"role": "assistant", "content": "혈압 관리에 대해 말씀드릴게요"},
                        {"role": "user", "content": "어떤 운동이 좋을까요?"},
                        {"role": "assistant", "content": "가벼운 산책을 추천드립니다"}
                    ]),
                    "mainTopics": ["건강", "혈압", "운동"],
                    "healthMentions": ["혈압", "운동"],
                    "concernsDiscussed": ["걱정"],
                    "moodAnalysis": "concerned",
                    "stressLevel": 6,
                    "conversationSummary": "혈압 관리와 운동에 대한 상담을 진행했습니다.",
                    "keyInsights": ["혈압 관리에 관심이 많음", "운동 방법을 찾고 있음"],
                    "aiRecommendations": ["정기적인 산책", "혈압 모니터링"]
                }
                
                response = await client.post(
                    f"{self.backend_url}/ai/conversation",
                    json=conversation_data
                )
                
                if response.status_code == 200:
                    conversation_id = response.json()
                    print(f"   ✅ 대화 저장 성공: ID {conversation_id}")
                    
                    # 사용자 인사이트 조회
                    response = await client.get(f"{self.backend_url}/ai/insights/{self.test_user_id}")
                    if response.status_code == 200:
                        insights = response.json()
                        print(f"   ✅ 인사이트 조회: 총 대화 {insights.get('totalConversations', 0)}회")
                        print(f"   📊 주요 관심사: {', '.join(insights.get('topInterests', [])[:3])}")
                    else:
                        print(f"   ❌ 인사이트 조회 실패: {response.status_code}")
                        
                else:
                    print(f"   ❌ 대화 저장 실패: {response.status_code}")
                    
        except Exception as e:
            print(f"   ❌ 대화 분석 테스트 오류: {e}")
        print()
    
    async def test_integration_scenario(self):
        """통합 시나리오 테스트"""
        print("6️⃣ 통합 시나리오 테스트 (시니어 김할머니)")
        
        scenario = """
        시나리오: 70세 김할머니가 AI 도우미와 건강 상담
        1. 혈압약 복용 시간 문의
        2. 무릎 아픈 증상 상담  
        3. 손자와의 관계 고민
        4. 요리 레시피 추천 요청
        """
        print(scenario)
        
        # 실제 WebSocket 연결 테스트는 별도의 클라이언트가 필요
        print("   💡 WebSocket 테스트:")
        print(f"   - 연결 URL: ws://localhost:8001/ws/chat/{self.test_user_id}")
        print("   - 프론트엔드 앱 또는 wscat으로 테스트 가능")
        
        print("   📱 프론트엔드 테스트:")
        print("   1. React Native 앱 실행")
        print("   2. 로그인 후 'AI 도우미와 채팅하기' 클릭")
        print("   3. 위 시나리오대로 대화 진행")
        print("   4. 개인화된 응답 확인")
        print()

async def main():
    """메인 테스트 실행"""
    tester = AISystemTester()
    await tester.run_all_tests()
    
    print("📋 추가 테스트 가이드:")
    print("1. OpenAI API 키를 .env에 설정하세요")
    print("2. 실제 사용자로 로그인하여 JWT 토큰을 얻으세요") 
    print("3. WebSocket 클라이언트로 실시간 채팅을 테스트하세요")
    print("4. 여러 대화를 나눈 후 인사이트 변화를 확인하세요")

if __name__ == "__main__":
    asyncio.run(main())