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
        self.generate("안녕", add_to_history=False)
        logger.info("Warmup done")

    def generate(
        self,
        user_input: str,
        user_name: str = "어르신",
        add_to_history: bool = True
    ) -> Dict[str, Any]:
        start_time = time.time()
        
        # Gemma-2 전용 프롬프트 포맷
        system_prompt = "당신은 다정한 손주입니다. 어르신의 말씀에 공감하며 한두 문장으로 따뜻하게 답하세요."
        
        # 히스토리 반영 (최근 2턴)
        history_text = ""
        for msg in self.conversation_history[-2:]:
            role = "user" if msg["role"] == "user" else "model"
            history_text += f"<start_of_turn>{role}\n{msg['content']}<end_of_turn>\n"

        prompt = (
            f"<start_of_turn>user\n{system_prompt}\n{history_text}"
            f"어르신: {user_input}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )

        # 텍스트 생성 (지연 시간을 위해 non-streaming으로 결과만 반환하거나 
        # 필요시 스트리밍으로 구조 변경 가능)
        output = self.llm(
            prompt,
            max_tokens=128,
            stop=["<end_of_turn>", "어르신:"],
            echo=False,
            temperature=0.7
        )
        
        response = output["choices"][0]["text"].strip()
        duration = time.time() - start_time
        
        if add_to_history:
            self.conversation_history.append({"role": "user", "content": user_input})
            self.conversation_history.append({"role": "assistant", "content": response})

        return {
            "response": response,
            "duration": duration,
            "tokens": output["usage"]["completion_tokens"]
        }

    def clear_history(self):
        self.conversation_history.clear()