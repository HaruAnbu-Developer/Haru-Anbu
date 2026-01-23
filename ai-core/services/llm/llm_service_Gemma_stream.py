#services/llm/llm_service_Gemma_stream.py

import time
import logging
from typing import Optional, Dict, Any, List
from llama_cpp import Llama
import re 
import json
logger = logging.getLogger(__name__)

class LLMService:
    """Gemma-2-9B-IT (GGUF) 기반 LLM 서비스"""
    
    def __init__(self, model_path: str = "./gemma-2-9b-it-Q5_K_M.gguf"):
        self.model_path = model_path
        self.conversation_history: List[Dict[str, str]] = []
        
        # 최적화된 파라미터로 로드
        self.llm = Llama(
            model_path=self.model_path,
            n_gpu_layers=-1,  # 모든 레이어 GPU 사용
            n_ctx=1024,
            n_batch=512,
            verbose=False
        )
        
        logger.info(f"Gemma-2 LLM Service initialized from {model_path}")
        self._warmup()

    def _warmup(self):
        logger.info("Warming up Gemma-2...")
        self.generate_stream("안녕", add_to_history=False)
        logger.info("Warmup done")

    def generate_stream(
        self, 
        user_input: str,
        user_name: str = "어르신",
        add_to_history: bool = True):
        """문장 단위로 텍스트를 정제하여 스트리밍"""
        system_prompt = "당신은 다정한 손주입니다. 어르신의 말씀에 공감하며 한두 문장으로 따뜻하게 답하세요."
        
        history_text = ""
        for msg in self.conversation_history[-2:]:
            role = "user" if msg["role"] == "user" else "model"
            history_text += f"<start_of_turn>{role}\n{msg['content']}<end_of_turn>\n"
        
        # 프롬프트 구성 (기존과 동일)
        prompt = (
            f"<start_of_turn>user\n{system_prompt}\n{history_text}"
            f"어르신: {user_input}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )

        # 스트리밍 생성 시작
        stream = self.llm(
            prompt,
            max_tokens=128,
            stop=["<end_of_turn>", "어르신:"],
            temperature=0.7,
            stream=True  # 🔥 스트리밍 활성화
        )
        
        sentence = ""
        full_response = ""
        for chunk in stream:
            token = chunk["choices"][0]["text"]
            sentence += token
            full_response += token
            
            # 문장 종결 기호(. ? !)가 나오면 문장을 잘라서 반환
            if any(punc in token for punc in [".", "?", "!", "\n"]):
                # 이모티콘 및 특수문자 제거 로직
                clean_sentence = re.sub(r'[^가-힣a-zA-Z0-9\s.\?\!\,]', '', sentence)
                clean_sentence = clean_sentence.strip()
                
                if clean_sentence:
                    yield clean_sentence
                sentence = "" # 문장 초기화
                
        # 모든 문장이 끝난 후(for 루프 종료 후)에 히스토리에 추가 full_responce
        if add_to_history:
            self.conversation_history.append({"role": "user", "content": user_input})
            # 전체 답변에서 이모티콘 제거 후 저장
            clean_full = re.sub(r'[^가-힣a-zA-Z0-9\s.\?\!\,]', '', full_response).strip()
            self.conversation_history.append({"role": "model", "content": clean_full})
            
    def clear_history(self):
        self.conversation_history.clear()
            
    
    # --- 새로 추가할 ask_json 메서드 --- 
    async def ask_json(self, prompt_text: str) -> Dict[str, Any]:
        """질문을 던지고 JSON 결과만 파싱하여 반환"""
        
        # 1. Gemma-2 전용 JSON 유도 프롬프트
        # 모델에게 JSON 외의 말은 하지 말라고 엄격하게 지시합니다.
        formatted_prompt = (
            f"<start_of_turn>user\n"
            f"지시사항: 반드시 다음 형식을 지킨 JSON 데이터만 출력하세요. 다른 설명은 절대 하지 마세요.\n"
            f"형식: {{\"category\": \"...\", \"question\": \"...\"}}\n"
            f"내용: {prompt_text}<end_of_turn>\n"
            f"<start_of_turn>model\n{{" # '{'를 미리 입력해두어 JSON 시작을 강제함
        )

        output = self.llm(
            formatted_prompt,
            max_tokens=256,
            stop=["<end_of_turn>", "}"], # '}'가 나오면 바로 멈추게 설정
            temperature=0.2,            # 형식을 위해 온도를 낮춤
            echo=False,
            stream=False
        )
        
        # 모델 출력값 복구 (미리 넣은 '{'와 멈춘 '}'를 다시 결합)
        raw_text = "{" + output["choices"][0]["text"].strip()
        if not raw_text.endswith("}"):
            raw_text += "}"

        try:
            # 2. 정규식으로 순수 JSON 블록만 추출 (혹시 모를 앞뒤 설명 제거)
            match = re.search(r"(\{.*\})", raw_text, re.DOTALL)
            if match:
                return json.loads(match.group(1))
            else:
                raise ValueError("JSON 형식을 찾을 수 없음")

        except Exception as e:
            logger.error(f"❌ JSON 파싱 에러: {e} | Raw: {raw_text}")
            # 대비책(Fallback): 질문 생성 실패 시 기본 질문 반환
            return {
                "category": "기본",
                "question": "오늘 하루 기분은 어떠신가요?"
            }

    async def ask_plain_text(self, prompt_text: str) -> str:
        """대본 생성을 위한 최적화된 텍스트 생성 메서드"""
        formatted_prompt = (
            f"<start_of_turn>user\n{prompt_text}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )
        
        # Llama-cpp-python의 특성에 맞게 파라미터 조정
        output = self.llm(
            formatted_prompt,
            max_tokens=2048,    # 1024는 대본이 길어질 경우 아슬아슬할 수 있으니 넉넉히 잡습니다.
            stop=["<end_of_turn>", "<|file_separator|>", "user:", "model:"], # 불필요한 생성을 막는 중복 방어
            temperature=0.8,    # 창의적인 표현을 위해 유지
            top_p=0.9,          # 너무 엉뚱한 단어가 나오지 않게 제어 (추가 권장)
            repeat_penalty=1.2, # 같은 말을 반복하는 '무한 루프' 방지 (추가 권장)
            echo=False,
            stream=False
        )
        
        result = output["choices"][0]["text"].strip()
        
        # 혹시 모를 모델의 자문자답(예: "알겠습니다. 대본을 작성해드릴게요") 제거
        # 보통 Gemma는 지시사항을 잘 따르지만, 가끔 서두를 붙이는 경우가 있음
        clean_result = re.sub(r'^(알겠습니다|네|작성해드리겠습니다).*?\n', '', result).strip()
        
        return clean_result if clean_result else result
    
    async def ask_stream_sentences(self, user_text: str, instruction: str = ""):
        """
        LLM 답변을 문장 단위로 끊어서 반환하여 TTS 체감 속도를 극대화합니다.
        """
        # 프롬프트 구성 (지침이 있다면 포함)
        full_prompt = (
            f"<start_of_turn>user\n{instruction}\n\n질문: {user_text}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )

        # Llama-cpp-python 스트리밍 호출
        # 문장 단위로 끊어야 하므로 stream=True
        stream_output = self.llm(
            full_prompt,
            max_tokens=1024,
            stop=["<end_of_turn>", "user:", "model:"],
            temperature=0.7,
            stream=True  # 한 토큰씩 받아오기
        )

        sentence_buffer = ""
        
        # 문장을 끊을 기준 (마침표, 물음표, 느낌표, 줄바꿈)
        # 한글 특성상 "..." 이나 "요." 뒤에서 끊는 것이 자연스럽습니다.
        split_marks = re.compile(r'([.!?\n])')

        for chunk in stream_output:
            token = chunk["choices"][0]["text"]
            sentence_buffer += token

            # 현재 버퍼에 문장 끝맺음 기호가 있는지 확인
            if split_marks.search(token):
                # 문장 기호 기준으로 텍스트 분리
                parts = split_marks.split(sentence_buffer)
                
                # 기호까지 포함해서 문장 완성 (ex: ["안녕하세요", ".", " "])
                # 마지막 요소는 기호 뒤의 잔여물이므로 제외하고 합침
                for i in range(0, len(parts) - 1, 2):
                    complete_sentence = (parts[i] + parts[i+1]).strip()
                    if complete_sentence:
                        yield complete_sentence
                
                # 남은 잔여물은 다시 버퍼에 저장
                sentence_buffer = parts[-1]

        # 스트리밍 종료 후 버퍼에 남은 내용 처리
        final_sentence = sentence_buffer.strip()
        if final_sentence:
            yield final_sentence

#싱글톤
_llm_service_instance = None

def get_llm_service() -> LLMService:
    global _llm_service_instance
    if _llm_service_instance is None:
        # 모델 경로는 본인의 환경에 맞게 수정
        _llm_service_instance = LLMService(model_path="./models/llm/gemma-2-9b-it-Q5_K_M.gguf")
    return _llm_service_instance