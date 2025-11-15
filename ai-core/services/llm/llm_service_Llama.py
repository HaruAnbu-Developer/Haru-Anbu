# services/llm_service.py

import torch
import time
import logging
from typing import Optional, Dict, Any, List
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    BitsAndBytesConfig,
    pipeline
)

logger = logging.getLogger(__name__)


class LLMService:
    """
    Llama-3-Open-Ko-8B 기반 대화 생성 서비스
    목표 응답시간: 0.8초 이내
    """
    
    def __init__(
        self,
        model_name: str = "beomi/Llama-3-Open-Ko-8B",
        device: Optional[str] = None,
        use_4bit: bool = True,
        max_new_tokens: int = 150
    ):
        """
        Args:
            model_name: 사용할 모델 이름
            device: 'cuda' 또는 'cpu'. None이면 자동 감지
            use_4bit: 4bit 양자화 사용 여부 (메모리 절약)
            max_new_tokens: 최대 생성 토큰 수
        """
        self.model_name = model_name
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        self.use_4bit = use_4bit and self.device == "cuda"  # 4bit는 CUDA에서만
        self.max_new_tokens = max_new_tokens
        
        self.model = None
        self.tokenizer = None
        self.pipe = None
        
        # 대화 컨텍스트 저장
        self.conversation_history: List[Dict[str, str]] = []
        
        self._load_model()
        
        logger.info(
            f"LLM Service initialized with model={model_name}, "
            f"device={self.device}, 4bit={self.use_4bit}"
        )
    
    def _load_model(self):
        """모델 및 토크나이저 로드"""
        try:
            start_time = time.time()
            logger.info(f"Loading LLM model: {self.model_name}...")
            
            # 토크나이저 로드
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_name,
                trust_remote_code=True
            )
            
            # 4bit 양자화 설정
            if self.use_4bit:
                logger.info("Using 4-bit quantization for memory efficiency")
                quantization_config = BitsAndBytesConfig(
                    load_in_4bit=True,
                    bnb_4bit_compute_dtype=torch.float16,
                    bnb_4bit_use_double_quant=True,
                    bnb_4bit_quant_type="nf4"
                )
                
                self.model = AutoModelForCausalLM.from_pretrained(
                    self.model_name,
                    quantization_config=quantization_config,
                    device_map="auto",
                    trust_remote_code=True,
                    torch_dtype=torch.float16
                )
            else:
                self.model = AutoModelForCausalLM.from_pretrained(
                    self.model_name,
                    device_map=self.device,
                    trust_remote_code=True
                )
            
            # 파이프라인 생성
            self.pipe = pipeline(
                "text-generation",
                model=self.model,
                tokenizer=self.tokenizer,
                max_new_tokens=self.max_new_tokens,
                do_sample=True,
                temperature=0.7,
                top_p=0.9,
                repetition_penalty=1.1
            )
            
            load_time = time.time() - start_time
            logger.info(f"LLM model loaded in {load_time:.2f}s")
            
            # 워밍업
            self._warmup()
            
        except Exception as e:
            logger.error(f"Failed to load LLM model: {e}")
            raise
    
    def _warmup(self):
        """모델 워밍업 - 첫 추론 속도 개선"""
        try:
            logger.info("Warming up LLM model...")
            self.generate("안녕하세요", use_prompt=False)
            logger.info("LLM warmup completed")
        except Exception as e:
            logger.warning(f"Warmup failed: {e}")
    
    def _build_elder_care_prompt(
        self,
        user_input: str,
        user_name: str = "어르신",
        conversation_context: Optional[List[Dict[str, str]]] = None
    ) -> str:
        """
        어르신 안부 전화용 프롬프트 생성
        
        Args:
            user_input: 어르신의 발화 내용
            user_name: 어르신 이름
            conversation_context: 이전 대화 컨텍스트
        
        Returns:
            완성된 프롬프트
        """
        
        system_prompt = f"""당신은 '{user_name}'께 안부 전화를 드리는 따뜻하고 친근한 AI 상담원입니다.

<역할 및 목표>
- 어르신의 안부를 묻고 일상을 경청합니다
- 따뜻하고 존중하는 톤으로 대화합니다
- 간결하지만 공감 어린 응답을 제공합니다 (1-2문장)
- 어르신의 감정 상태와 건강 상태를 파악합니다

<대화 가이드라인>
1. 존댓말을 사용하고 경청하는 태도를 보입니다
2. 응답은 30-50자 내외로 간결하게 유지합니다
3. 어르신의 말씀을 반복하거나 공감을 표현합니다
4. 위험 신호(아파요, 외로워요, 도움 필요 등)에 민감하게 반응합니다
5. 긍정적이고 밝은 톤을 유지합니다

<주의사항>
- 의료 조언이나 진단을 하지 않습니다
- 복잡하거나 긴 설명을 피합니다
- 어르신이 이해하기 쉬운 단어를 사용합니다
"""

        # 대화 히스토리 추가
        conversation = ""
        if conversation_context:
            for msg in conversation_context[-3:]:  # 최근 3턴만 사용
                role = "어르신" if msg["role"] == "user" else "AI"
                conversation += f"{role}: {msg['content']}\n"
        
        conversation += f"어르신: {user_input}\nAI:"
        
        full_prompt = f"{system_prompt}\n\n<대화>\n{conversation}"
        
        return full_prompt
    
    def generate(
        self,
        user_input: str,
        user_name: str = "어르신",
        use_prompt: bool = True,
        add_to_history: bool = True
    ) -> Dict[str, Any]:
        """
        사용자 입력에 대한 응답 생성
        
        Args:
            user_input: 사용자(어르신) 입력 텍스트
            user_name: 어르신 이름
            use_prompt: 안부 전화 프롬프트 사용 여부
            add_to_history: 대화 히스토리에 추가 여부
        
        Returns:
            {
                "response": str,      # 생성된 응답
                "duration": float,    # 처리 시간
                "tokens": int         # 생성된 토큰 수
            }
        """
        start_time = time.time()
        
        try:
            # 프롬프트 생성
            if use_prompt:
                prompt = self._build_elder_care_prompt(
                    user_input,
                    user_name,
                    self.conversation_history if add_to_history else None
                )
            else:
                prompt = user_input
            
            # 응답 생성
            output = self.pipe(
                prompt,
                max_new_tokens=self.max_new_tokens,
                do_sample=True,
                temperature=0.7,
                top_p=0.9,
                repetition_penalty=1.1,
                pad_token_id=self.tokenizer.eos_token_id
            )
            
            # 응답 추출
            generated_text = output[0]["generated_text"]
            
            # 프롬프트 제거하고 응답만 추출
            if use_prompt and "AI:" in generated_text:
                response = generated_text.split("AI:")[-1].strip()
            else:
                response = generated_text[len(prompt):].strip()
            
            # 첫 번째 문장만 추출 (간결성)
            if "." in response:
                sentences = response.split(".")
                response = sentences[0] + "."
            elif "?" in response:
                sentences = response.split("?")
                response = sentences[0] + "?"
            elif "!" in response:
                sentences = response.split("!")
                response = sentences[0] + "!"
            
            # 너무 긴 응답 자르기 (70자 제한)
            if len(response) > 70:
                response = response[:67] + "..."
            
            duration = time.time() - start_time
            
            # 대화 히스토리에 추가
            if add_to_history:
                self.conversation_history.append({
                    "role": "user",
                    "content": user_input
                })
                self.conversation_history.append({
                    "role": "assistant",
                    "content": response
                })
            
            # 성능 모니터링
            if duration > 0.8:
                logger.warning(f"LLM processing took {duration:.3f}s (target: 0.8s)")
            else:
                logger.info(f"LLM processing completed in {duration:.3f}s")
            
            return {
                "response": response,
                "duration": duration,
                "tokens": len(self.tokenizer.encode(response))
            }
            
        except Exception as e:
            logger.error(f"LLM generation failed: {e}")
            raise
    
    def generate_conversation_summary(self, conversation: List[Dict[str, str]]) -> str:
        """
        대화 내용 요약 생성
        
        Args:
            conversation: 대화 내역 [{"role": "user/assistant", "content": "..."}]
        
        Returns:
            한 줄 요약
        """
        # 대화 내용을 텍스트로 변환
        conv_text = ""
        for msg in conversation:
            role = "어르신" if msg["role"] == "user" else "AI"
            conv_text += f"{role}: {msg['content']}\n"
        
        prompt = f"""다음 어르신과의 안부 전화 내용을 한 줄로 요약해주세요.

<대화 내용>
{conv_text}

<요약 지침>
- 20-30자 내외로 핵심만 요약
- 어르신의 주요 관심사나 상태를 포함
- 객관적이고 간결하게 작성

요약:"""

        try:
            output = self.pipe(
                prompt,
                max_new_tokens=50,
                do_sample=False,
                temperature=0.3
            )
            
            summary = output[0]["generated_text"][len(prompt):].strip()
            
            # 첫 문장만 추출
            if "." in summary:
                summary = summary.split(".")[0] + "."
            
            return summary
            
        except Exception as e:
            logger.error(f"Summary generation failed: {e}")
            return "대화 요약 실패"
    
    def clear_history(self):
        """대화 히스토리 초기화"""
        self.conversation_history.clear()
        logger.debug("Conversation history cleared")
    
    def get_device_info(self) -> Dict[str, Any]:
        """현재 디바이스 정보 반환"""
        info = {
            "device": self.device,
            "model_name": self.model_name,
            "use_4bit": self.use_4bit,
            "cuda_available": torch.cuda.is_available()
        }
        
        if torch.cuda.is_available():
            info["gpu_name"] = torch.cuda.get_device_name(0)
            info["gpu_memory_allocated"] = torch.cuda.memory_allocated(0) / 1024**3  # GB
            info["gpu_memory_reserved"] = torch.cuda.memory_reserved(0) / 1024**3  # GB
        
        return info
    
    def unload_model(self):
        """메모리 절약을 위한 모델 언로드"""
        if self.model is not None:
            del self.model
            del self.tokenizer
            del self.pipe
            self.model = None
            self.tokenizer = None
            self.pipe = None
            
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            logger.info("LLM model unloaded")


# 싱글톤 패턴
_llm_service_instance: Optional[LLMService] = None


def get_llm_service(
    model_name: str = "beomi/Llama-3-Open-Ko-8B",
    use_4bit: bool = True
) -> LLMService:
    """LLM 서비스 싱글톤 인스턴스 반환"""
    global _llm_service_instance
    
    if _llm_service_instance is None:
        _llm_service_instance = LLMService(
            model_name=model_name,
            use_4bit=use_4bit
        )
    
    return _llm_service_instance


if __name__ == "__main__":
    # 테스트
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    print("LLM Service Test")
    print("=" * 80)
    
    # 서비스 초기화
    llm = LLMService(use_4bit=True)
    
    # 디바이스 정보
    device_info = llm.get_device_info()
    print(f"\n📊 Device Information:")
    for key, value in device_info.items():
        print(f"   {key}: {value}")
    
    # 테스트 대화
    print(f"\n💬 Test Conversation:")
    print("=" * 80)
    
    test_inputs = [
        "안녕하세요, 오늘 날씨가 참 좋네요.",
        "요즘 무릎이 좀 아파서 걱정이에요.",
        "손주들이 주말에 온대요. 너무 기대돼요!",
    ]
    
    for i, user_input in enumerate(test_inputs, 1):
        print(f"\n[Turn {i}]")
        print(f"어르신: {user_input}")
        
        result = llm.generate(user_input, user_name="김영희")
        
        print(f"AI: {result['response']}")
        print(f"⏱️  Processing time: {result['duration']:.3f}s")
        print(f"📝 Tokens: {result['tokens']}")
    
    # 대화 요약
    print(f"\n📋 Conversation Summary:")
    print("=" * 80)
    summary = llm.generate_conversation_summary(llm.conversation_history)
    print(f"{summary}")
    
    print("\n" + "=" * 80)
    print("Test completed!")
    print("=" * 80)