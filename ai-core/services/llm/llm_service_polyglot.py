# services/llm/llm_service_polyglot.py (완전히 새로 작성)

import torch
import time
import logging
from typing import Optional, Dict, Any, List
from transformers import AutoModelForCausalLM, AutoTokenizer

logger = logging.getLogger(__name__)


class LLMService:
    """Polyglot-ko-1.3B 기반 LLM"""
    
    def __init__(self, model_name: str = "EleutherAI/polyglot-ko-1.3b"):
        self.model_name = model_name
        
        if torch.backends.mps.is_available():
            self.device = "mps"
        elif torch.cuda.is_available():
            self.device = "cuda"
        else:
            self.device = "cpu"
        
        self.model = None
        self.tokenizer = None
        self.conversation_history: List[Dict[str, str]] = []
        
        self._load_model()
        logger.info(f"LLM Service initialized: {model_name} on {self.device}")
    
    def _load_model(self):
        """모델 로드"""
        start_time = time.time()
        logger.info(f"Loading {self.model_name}...")
        
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
            self.tokenizer.pad_token_id = self.tokenizer.eos_token_id
        
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_name,
            torch_dtype=torch.float16 if self.device != "cpu" else torch.float32,
            low_cpu_mem_usage=True
        ).to(self.device)
        self.model.eval()
        
        logger.info(f"Model loaded in {time.time() - start_time:.2f}s")
        self._warmup()
    
    def _warmup(self):
        """워밍업"""
        logger.info("Warming up...")
        self.generate("안녕하세요", add_to_history=False)
        logger.info("Warmup done")
    
    def generate(
        self,
        user_input: str,
        user_name: str = "어르신",
        add_to_history: bool = True
    ) -> Dict[str, Any]:
        """응답 생성"""
        start_time = time.time()
        
        prompt = self._build_prompt(user_input, user_name)
        
        inputs = self.tokenizer(
            prompt,
            return_tensors="pt",
            padding=True,
            truncation=True,
            max_length=512,
            return_token_type_ids=False
        ).to(self.device)
        
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=80,
                min_new_tokens=10,
                do_sample=True,
                temperature=0.9,
                top_k=40,
                top_p=0.85,
                repetition_penalty=1.15,
                pad_token_id=self.tokenizer.pad_token_id,
                eos_token_id=self.tokenizer.eos_token_id
            )
        
        generated = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        response = self._extract_response(generated, prompt)
        duration = time.time() - start_time
        
        if add_to_history:
            self.conversation_history.append({"role": "user", "content": user_input})
            self.conversation_history.append({"role": "assistant", "content": response})
        
        logger.info(f"Generated in {duration:.3f}s: {response}")
        
        return {
            "response": response,
            "duration": duration,
            "tokens": len(self.tokenizer.encode(response))
        }
    
    def _build_prompt(self, user_input: str, user_name: str) -> str:
        """단순화된 프롬프트"""
        
        # 예시 (짧게)
        examples = """어르신: 오늘 날씨가 좋네요.
상담원: 그러게요. 산책하시면 좋겠어요.

어르신: 무릎이 아파요.
상담원: 많이 불편하시겠어요. 병원은 가보셨나요?

어르신: 손주가 온대요.
상담원: 손주 보시면 기쁘시겠어요!"""

        # 컨텍스트 (최근 1턴만)
        context = ""
        if self.conversation_history:
            last = self.conversation_history[-2:]
            for msg in last:
                role = "어르신" if msg["role"] == "user" else "상담원"
                context += f"\n{role}: {msg['content']}"
        
        # 프롬프트
        prompt = f"""{examples}{context}

어르신: {user_input}
상담원:"""
        
        return prompt
    
    def _extract_response(self, generated: str, prompt: str) -> str:
        """응답 추출"""
        if prompt in generated:
            response = generated[len(prompt):].strip()
        else:
            if "상담원:" in generated:
                parts = generated.split("상담원:")
                response = parts[-1].strip()
            else:
                response = generated.strip()
        
        response = self._clean_response(response)
        return response
    
    def _clean_response(self, response: str) -> str:
        """응답 정제"""
        response = response.replace("\n", " ").strip()
        
        # 다음 화자 나오면 자르기
        for marker in ["어르신:", "###", "\n\n"]:
            if marker in response:
                response = response.split(marker)[0].strip()
        
        # 첫 문장만
        for delim in ['.', '!', '?']:
            if delim in response:
                response = response.split(delim)[0] + delim
                break
        
        # 길이 제한
        if len(response) > 50:
            response = response[:47] + "..."
        
        # 빈 응답
        if not response or len(response) < 5 or response == ". .":
            response = "네, 말씀 잘 들었습니다."
        
        return response.strip()
    
    def clear_history(self):
        self.conversation_history.clear()
    
    def get_device_info(self) -> Dict[str, Any]:
        return {
            "device": self.device,
            "model_name": self.model_name,
            "mps_available": torch.backends.mps.is_available(),
            "cuda_available": torch.cuda.is_available()
        }