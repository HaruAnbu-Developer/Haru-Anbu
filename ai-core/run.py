# run.py

import os
import time
import logging
from typing import Dict, Any, Optional
from services.stt.stt_service import STTService
from services.llm.llm_service_polyglot import LLMService

logger = logging.getLogger(__name__)


class VoiceConversationPipeline:
    """
    음성 대화 파이프라인
    STT → LLM → (나중에 TTS 추가)
    """
    
    def __init__(
        self,
        stt_model: str = "tiny",
        llm_model: str = "EleutherAI/polyglot-ko-1.3b"
    ):
        logger.info("Initializing Voice Conversation Pipeline...")
        
        self.stt = STTService(model_size=stt_model)
        self.llm = LLMService(model_name=llm_model)
        
        logger.info("Pipeline ready!")
    
    def process_audio(
        self,
        audio_path: Optional[str] = None,
        audio_data: Optional[Any] = None,
        user_name: str = "어르신"
    ) -> Dict[str, Any]:
        """
        음성 → 텍스트 → LLM 응답
        """
        total_start = time.time()
        
        # 1. STT
        logger.info("Processing STT...")
        stt_result = self.stt.transcribe(
            audio_path=audio_path,
            audio_data=audio_data
        )
        
        user_text = stt_result["text"]
        stt_duration = stt_result["duration"]
        logger.info(f"STT output: {user_text}")
        
        # 2. LLM
        logger.info("Processing LLM...")
        llm_result = self.llm.generate(
            user_input=user_text,
            user_name=user_name
        )
        
        ai_response = llm_result["response"]
        llm_duration = llm_result["duration"]
        logger.info(f"LLM output: {ai_response}")
        
        # 3. 전체 성능
        total_duration = time.time() - total_start
        
        result = {
            "user_text": user_text,
            "ai_response": ai_response,
            "stt_duration": stt_duration,
            "llm_duration": llm_duration,
            "total_duration": total_duration
        }
        
        if total_duration > 1.3:
            logger.warning(f"Pipeline slow: {total_duration:.3f}s (target: 1.3s)")
        else:
            logger.info(f"Pipeline completed in {total_duration:.3f}s")
        
        return result
    
    def process_text(self, user_text: str, user_name: str = "어르신") -> Dict[str, Any]:
        """텍스트만 처리 (테스트용)"""
        logger.info(f"Processing text: {user_text}")
        
        llm_result = self.llm.generate(
            user_input=user_text,
            user_name=user_name
        )
        
        return {
            "user_text": user_text,
            "ai_response": llm_result["response"],
            "llm_duration": llm_result["duration"]
        }
    
    def clear_conversation(self):
        """대화 히스토리 초기화"""
        self.llm.clear_history()
        logger.info("Conversation history cleared")
    
    def get_conversation_history(self):
        """현재 대화 히스토리 반환"""
        return self.llm.conversation_history


def main():
    """메인 실행 함수"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )
    
    print("\n" + "="*80)
    print("🔄 Voice Conversation Pipeline Test")
    print("="*80)
    
    # 파이프라인 초기화
    pipeline = VoiceConversationPipeline(
        stt_model="base",
        llm_model="EleutherAI/polyglot-ko-1.3b"
    )
    
    # 테스트 1: 오디오 파일 처리
    print("\n" + "="*80)
    print("📁 Test 1: 오디오 파일 처리")
    print("="*80)
    
    audio_file = "/Users/namung2/haru/Haru-Anbu/ai-core/studio_origin/M-GS-1/sample.wav"  # 테스트용 오디오 파일 경로 설정
    
    if os.path.exists(audio_file):
        result = pipeline.process_audio(audio_path=audio_file)
        
        print(f"\n👴 어르신: {result['user_text']}")
        print(f"🤖 AI: {result['ai_response']}")
        print(f"\n⏱️  성능:")
        print(f"   STT: {result['stt_duration']:.3f}s")
        print(f"   LLM: {result['llm_duration']:.3f}s")
        print(f"   Total: {result['total_duration']:.3f}s")
    else:
        print(f"❌ 오디오 파일을 찾을 수 없습니다: {audio_file}")
    

    # 대화 히스토리
    print("\n" + "="*80)
    print("📜 Conversation History")
    print("="*80)
    
    history = pipeline.get_conversation_history()
    for i, msg in enumerate(history, 1):
        role = "👴 어르신" if msg["role"] == "user" else "🤖 AI"
        print(f"{i}. {role}: {msg['content']}")
    
    print("\n" + "="*80)
    print("✅ All tests completed!")
    print("="*80)


if __name__ == "__main__":
    main()