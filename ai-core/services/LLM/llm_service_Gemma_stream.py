import time
import logging
from typing import Optional, Dict, Any, List
from llama_cpp import Llama
import re # 상단에 추가

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