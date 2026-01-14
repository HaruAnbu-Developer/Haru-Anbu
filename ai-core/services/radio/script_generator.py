import logging
import os
import sys
from typing import Optional
from datetime import datetime

# 프로젝트 루트 경로 설정 (ai-core 폴더를 path에 추가)
# 현재 파일: ai-core/services/radio/script_generator.py
# 목표: ai-core 폴더를 sys.path에 추가하여 'services' 패키지를 인식하게 함
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, "../../")) # ai-core
if project_root not in sys.path:
    sys.path.append(project_root)

from services.radio.radio_service import RadioService
try:
    from services.LLM.llm_service_Gemma import LLMService
except ImportError as e:
    logger.warning(f"LLM Import Warning: {e}")
    # Fallback or re-try logic if needed
    from services.LLM.llm_service_Gemma import LLMService

from services.TTS.tts_service import TTSService

logger = logging.getLogger(__name__)

class RadioScriptGenerator:
    def __init__(self, llm_service: Optional[LLMService] = None, radio_service: Optional[RadioService] = None, model_path: str = "./models/llm/gemma-2-9b-it-Q5_K_M.gguf"):
        self.radio_service = radio_service if radio_service else RadioService()
        # LLM 서비스가 주입되지 않으면 새로 생성
        if llm_service:
            self.llm_service = llm_service
        else:
            # 모델 경로가 존재하는지 확인 후 로드, 아니면 기본값
            if not os.path.exists(model_path):
                # 상위 디렉토리에서 찾기 시도
                alt_path = "../../models/llm/gemma-2-9b-it-Q5_K_M.gguf" # ai-core/services/radio/../../models...
                if os.path.exists(alt_path):
                    model_path = alt_path
            
            self.llm_service = LLMService(model_path=model_path)
            
        # TTS 서비스 초기화
        # 우선순위: clean_sample.wav (AI 생성 표준 음성) > test.wav (사용자 제공)
        # ai-core/services/radio/../../.. -> ai-core
        # ai-core/.. -> Project Root (need one more dirname)
        project_root_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        
        clean_wav_path = os.path.join(project_root_dir, "clean_sample.wav")
        test_wav_path = os.path.join(project_root_dir, "test.wav")
        
        # 기본값 설정 (test.wav) 및 clean_sample.wav 존재 시 덮어쓰기
        ref_wav_path = test_wav_path
        if os.path.exists(clean_wav_path):
            ref_wav_path = clean_wav_path
            logger.info(f"Using CLEAN REFERENCE sample: {ref_wav_path}")
        else:
            logger.info(f"Using DEFAULT sample: {ref_wav_path}")
        
        if not os.path.exists(ref_wav_path):
             logger.warning(f"Audio reference file not found at {ref_wav_path}, audio generation might fail.")
        
        self.tts_service = TTSService(
            speaker_wav=ref_wav_path,
            language="ko"
        )

    def generate_script(self, date_str: str = None) -> str:
        """
        특정 날짜의 라디오 대본 생성
        date_str: 'YYYY-MM-DD' (기본값: 오늘)
        """
        if not date_str:
            date_str = datetime.now().strftime("%Y-%m-%d")

        # 1. 해당 날짜의 질문과 토픽 조회
        question, topic_id = self.radio_service.get_question_by_date(date_str)
        if not topic_id:
            return f"[{date_str}] 예정된 라디오 주제가 없습니다."

        # 2. 답변 수집
        answers = self.radio_service.get_answers_by_topic(topic_id)
        if not answers:
            return f"[{date_str}] 주제 '{question}'에 대한 청취자 사연이 아직 없습니다."

        # 3. 프롬프트 구성
        stories_text = ""
        for i, ans in enumerate(answers):
            # 익명화 혹은 ID 사용
            stories_text += f"- 사연 {i+1} (청취자 {ans.user_id}): {ans.answer_text}\n"

        prompt = (
            f"당신은 '하루안부'라는 시니어 전문 라디오 방송의 따뜻하고 유머러스한 메인 DJ입니다.\n"
            f"오늘의 주제는 '{question}'였구요, 어르신들이 직접 보내주신 귀한 사연들이 있습니다.\n"
            f"이 사연들을 소개해드리면서 청취자들에게 하루를 시작할 활력을 드리는 아침 방송 대본을 작성해주세요.\n\n"
            f"[접수된 사연 목록]\n{stories_text}\n\n"
            f"[작성 지침]\n"
            f"1. 오프닝: 활기찬 아침 인사와 함께 오늘의 주제를 소개하세요.\n"
            f"2. 사연 소개: 위 사연들을 하나씩 소개하되, 내용을 그대로 읽기보다 DJ가 맛깔나게 각색해서 읽어주세요.\n"
            f"3. 리액션과 과장: 각 사연에 대해 '아이고~ 정말 대단하십니다!', '이런 효자가 없네요!' 등 감탄과 칭찬을 아낌없이 덧붙이세요. 내용을 긍정적인 방향으로 풍성하게 부풀려주세요.\n"
            f"4. 클로징: 건강과 행복을 기원하며 따뜻하게 마무리하세요.\n"
            f"5. 전체 대본 길이: 3분 내외 분량으로 작성하세요.\n"
            f"6. (중요) 진행자 대사만 출력하세요.\n"
        )
        
        logger.info(f"Generating radio script for date: {date_str}, topic: {topic_id}")

        # 4. LLM 생성
        # generate 메서드는 {'response': ..., 'duration': ...} 반환
        result = self.llm_service.generate(prompt, add_to_history=False)
        result = self.llm_service.generate(prompt, add_to_history=False)
        return result["response"]

    def generate_audio(self, script_text: str, output_path: str = "radio_broadcast.wav"):
        """생성된 대본을 음성 파일로 변환"""
        logger.info("Generating audio from script...")
        try:
            # TTS로 텍스트 -> 음성 변환 (긴 텍스트일 경우 분할 처리가 필요할 수 있음)
            # 여기서는 전체 텍스트를 한 번에 처리 시도
            self.tts_service.synthesize(
                text=script_text,
                save_path=output_path,
                speed=1.0
            )
            logger.info(f"Audio saved to {output_path}")
            return output_path
        except Exception as e:
            logger.error(f"Failed to generate audio: {e}")
            return None

if __name__ == "__main__":
    # 간단 테스트
    logging.basicConfig(level=logging.INFO)
    generator = RadioScriptGenerator()
    # 어제 날짜로 테스트하거나, 오늘 날짜로 테스트
    today = datetime.now().strftime("%Y-%m-%d")
    today = datetime.now().strftime("%Y-%m-%d")
    script = generator.generate_script(today)
    print("=== Generated Script ===")
    print(script)
    
    if script and not script.startswith("["): # 에러 메시지가 아니면
        # 오디오 생성
        wav_path = generator.generate_audio(script)
        if wav_path:
            print(f"=== Audio Generated: {wav_path} ===")
