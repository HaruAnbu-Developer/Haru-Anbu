import logging
import json
import os
import boto3
from datetime import datetime, timedelta, timezone
from dotenv import load_dotenv

from database.database import SessionLocal
from database.schema import ConversationAnalysis, UserMission, UserMemory
from services.llm.llm_service_Gemma_stream import get_llm_service

load_dotenv()
logger = logging.getLogger(__name__)

class AnalysisService:
    def __init__(self):
        self.llm_service = get_llm_service()
        
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.getenv("S3_ACCESS_KEY_ID"),
            aws_secret_access_key=os.getenv("S3_SECRET_ACCESS_KEY"),
            region_name=os.getenv("S3_REGION", "ap-northeast-2")
        )
        self.bucket_name = os.getenv("S3_BUCKET_NAME", "haru-anbu-voice-storage")
    async def run_daily_analysis_batch(self):
        """
        [스케줄러용] 어제(D-1) 하루 동안 생성된 S3 로그 파일들을 찾아 일괄 분석 수행
        """
        logger.info("🕒 일일 통화 분석 배치 작업 시작...")
        
        try:
            # 1. 날짜 범위 설정 (어제 00:00 ~ 오늘 00:00 UTC 기준)
            # S3는 UTC 시간을 사용하므로 주의 필요
            now = datetime.now(timezone.utc)
            yesterday = now - timedelta(days=1)
            
            # 2. S3 logs/ 폴더 목록 조회
            paginator = self.s3_client.get_paginator('list_objects_v2')
            page_iterator = paginator.paginate(Bucket=self.bucket_name, Prefix='logs/')

            processed_count = 0
            
            for page in page_iterator:
                if 'Contents' not in page:
                    continue
                    
                for obj in page['Contents']:
                    file_key = obj['Key']
                    last_modified = obj['LastModified']
                    
                    # 3. "어제" 수정된 파일인지 확인 (Active User 판단 로직 대체)
                    if last_modified > yesterday:
                        logger.info(f"🔍 분석 대상 발견: {file_key} ({last_modified})")
                        await self.analyze_from_s3_log(file_key)
                        processed_count += 1
            
            logger.info(f"✅ 배치 완료: 총 {processed_count}건의 통화 기록 분석됨.")
            
        except Exception as e:
            logger.error(f"❌ 배치 작업 중 치명적 오류: {e}")
            
            
    async def analyze_from_s3_log(self, log_key: str):
        """S3 로그 파일 -> 치매/인지 기능 정밀 분석 -> DB 저장"""
        logger.info(f"🚀 로그 기반 정밀 분석 시작: {log_key}")
        
        try:
            # 1. S3 다운로드
            response = self.s3_client.get_object(Bucket=self.bucket_name, Key=log_key)
            content = response['Body'].read().decode('utf-8')
            log_data = json.loads(content)
            
            user_id = log_data['user_id']
            conversation_id = f"{user_id}_{log_data['timestamp']}"
            
            full_text = "\n".join(log_data['conversation_log'])
            missions = log_data.get('missions', []) # 미션(질문) 목록

            # 2. LLM 분석 및 저장
            await self._run_llm_analysis_and_save(user_id, conversation_id, full_text, missions, log_key)

        except Exception as e:
            logger.error(f"❌ 분석 실패 ({log_key}): {e}")

    async def _run_llm_analysis_and_save(self, user_id: str, conversation_id: str, full_text: str, missions: list, log_key: str):
        db = SessionLocal()
        try:
            mission_texts = [m['question'] for m in missions]
            
            # ★ 업데이트된 치매 관리 특화 프롬프트
            prompt = f"""
            다음은 치매 고위험군 대상자와 AI의 통화 기록입니다. 
            이 대화를 기반으로 대상자의 인지 기능을 평가하고 보호자에게 전달할 요약 리포트를 JSON으로 생성하세요.
            
            [대화 로그]
            {full_text}
            
            [제시된 질문 목록 (UserMission)]
            {mission_texts}
            
            [지시사항]
            1. **종합 평가**:
               - chi_score: 0~100점 (70점 이상 양호).
               - danger_level: 0(양호), 1(주의), 2(위험).
            
            2. **세부 인지 지표 (각 0~20점)**:
               - recall_score: 기억력/회상 능력.
               - coherence_score: 문맥 일관성/논리성.
               - orientation_score: 시간/장소 지남력.
               - stability_score: 감정 안정성.
               - engagement_score: 참여 적극성.
            
            3. **질문 수행 결과**:
               - question_results: [{{"question": "...", "is_correct": true/false}}]
               - 정답 기준: 문맥상 적절하고 논리적인 답변을 했으면 true.
            
            4. **텍스트 분석**:
               - summary: 보호자를 위한 3줄 요약 (비의료인 관점).
               - health_flags: ["두통", "우울", "불면"] 등 건강 이상 키워드. 없으면 [].
               - daily_answer: 오늘의 라디오 공통 질문이나 주요 질문에 대한 대상자의 핵심 답변 한 문장.
            
            5. **(중요) 다음 통화를 위한 기억**:
               - memory_summary: **내일 AI가 안부를 물을 때 참조해야 할 핵심 내용 한 문장.** (예: "어제 무릎이 아파서 파스를 붙였다고 하셨음")

            반드시 아래 JSON 형식으로만 출력하세요:
            {{
              "chi_score": 0,
              "danger_level": 0,
              "recall_score": 0,
              "coherence_score": 0,
              "orientation_score": 0,
              "stability_score": 0,
              "engagement_score": 0,
              "question_results": [],
              "summary": "",
              "health_flags": [],
              "daily_answer": "",
              "memory_summary": ""
            }}
            """
            
            # LLM 호출
            result_json = await self.llm_service.ask_json(prompt)
            
            # (테스트용 더미 데이터 - 실제 연결 시 위 주석 해제 후 삭제)
            # LLM이 파싱 실패했을 때를 대비한 기본값 처리도 고려해야 함

            # 3-1. ConversationAnalysis 저장 (리포트용)
            analysis = ConversationAnalysis(
                conversation_id=conversation_id,
                user_id=user_id,
                
                # 점수 매핑
                chi_score=result_json.get("chi_score", 0),
                danger_level=result_json.get("danger_level", 0),
                
                # 세부 지표
                recall_score=result_json.get("recall_score", 0),
                coherence_score=result_json.get("coherence_score", 0),
                orientation_score=result_json.get("orientation_score", 0),
                stability_score=result_json.get("stability_score", 0),
                engagement_score=result_json.get("engagement_score", 0),
                
                # 텍스트 데이터
                question_results=json.dumps(result_json.get("question_results", []), ensure_ascii=False),
                summary=result_json.get("summary", ""),
                health_flags=json.dumps(result_json.get("health_flags", []), ensure_ascii=False),
                daily_answer=result_json.get("daily_answer", ""),
                
                # 원본 로그 파일 경로 (추적용)
                # (스키마에 해당 컬럼이 없다면 daily_answer 등에 넣거나 무시, 
                #  혹은 daily_answer 컬럼 용도를 파일 경로 저장용이 아닌 실제 답변 저장용으로 쓰기로 했으므로 주의)
                # 여기선 daily_answer에는 실제 답변을 넣고, 파일 경로는 별도로 저장하지 않음(conversation_id로 유추 가능)
            )
            db.add(analysis)

            # 3-2. UserMemory 저장 (다음 통화 문맥 생성용)
            if result_json.get("memory_summary"):
                new_memory = UserMemory(
                    user_id=user_id,
                    conversation_id=conversation_id,
                    summary_text=result_json["memory_summary"]
                )
                db.add(new_memory)

            # 3-3. 미션 완료 처리 (UserMission) 로직 수정
            
            # LLM 분석 결과(question_results)가 있으면 실행
            if result_json.get("question_results"):
                # 미션 리스트(missions)를 순회하며 LLM 결과와 매칭
                for m in missions:
                    mission_text = m.get('question') # 혹은 m.get('mission_text')
                    db_id = m.get('db_id')
                    
                    # LLM 결과에서 해당 질문에 대한 정답 여부 찾기
                    # (LLM이 텍스트를 약간 바꿀 수도 있으므로, 순서가 같다고 가정하거나 텍스트 포함 여부로 확인)
                    is_success = False
                    for res in result_json["question_results"]:
                        # 질문 내용이 대충 맞으면 (LLM이 요약했을 수 있으므로 in 사용 추천)
                        if res.get("question") in mission_text or mission_text in res.get("question"):
                            if res.get("is_correct") == True:
                                is_success = True
                            break
                    
                    # ★ 정답(is_success)이고, DB ID가 존재할 때만 업데이트 수행
                    if is_success and db_id:
                        db_mission = db.query(UserMission).filter(UserMission.id == db_id).first()
                        if db_mission:
                            db_mission.is_cleared = True
                            logger.info(f"✅ 미션 성공 처리 (ID: {db_id}): {mission_text}")

            db.commit()
            logger.info(f"💾 {user_id} 정밀 분석 및 기억 저장 완료")

        except Exception as e:
            logger.error(f"❌ DB 저장 실패: {e}")
            db.rollback()
        finally:
            db.close()

# 싱글톤
_analysis_service = None
def get_analysis_service():
    global _analysis_service
    if _analysis_service is None:
        _analysis_service = AnalysisService()
    return _analysis_service