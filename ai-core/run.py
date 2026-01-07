import os
import time
import logging
from typing import Dict, Any, Optional
from services.stt.stt_service import STTService
from services.llm.llm_service_polyglot import LLMService
from services.tts.tts_service import TTSService   # 🔥 TTS 서비스 추가
logger = logging.getLogger(__name__)


class VoiceConversationPipeline:
    """
    음성 대화 파이프라인
    STT → LLM → TTS
    """
    
    def __init__(
        self,
        stt_model: str = "tiny",
        llm_model: str = "EleutherAI/polyglot-ko-1.3b",
        tts_model: str = "tts_models/multilingual/multi-dataset/xtts_v2"
    ):
        logger.info("Initializing Voice Conversation Pipeline...")
        
        self.stt = STTService(model_size=stt_model)
        self.llm = LLMService(model_name=llm_model)
        self.tts = TTSService(model_name=tts_model)   # 🔥 TTS 추가
        
        logger.info("Pipeline ready!")
    
    def process_audio(
        self,
        audio_path: Optional[str] = None,
        audio_data: Optional[Any] = None,
        user_name: str = "어르신"
    ) -> Dict[str, Any]:
        """
        음성 → 텍스트 → LLM → 음성(TTS)
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

        # 3. TTS (AI 응답 → 음성)
        logger.info("Processing TTS...")
        # tts_output_path = "/Users/namung2/haru/Haru-Anbu/ai-core/outputs/tts_response2.wav"
        tts_output_path = "./outputs/tts_response2.wav"
        tts_result = self.tts.synthesize(
            text=ai_response,
            save_path=tts_output_path
        )

        tts_duration = tts_result["duration"]
        logger.info(f"TTS saved to: {tts_output_path}")
        
        # 4. 전체 성능
        total_duration = time.time() - total_start
        
        result = {
            "user_text": user_text,
            "ai_response": ai_response,
            "stt_duration": stt_duration,
            "llm_duration": llm_duration,
            "tts_duration": tts_duration,
            "total_duration": total_duration,
            "tts_file": tts_output_path      # 🔥 wav 파일 반환
        }
        
        if total_duration > 1.8:
            logger.warning(f"Pipeline slow: {total_duration:.3f}s (target: 1.8s)")
        else:
            logger.info(f"Pipeline completed in {total_duration:.3f}s")
        
        return result
    
    def clear_conversation(self):
        self.llm.clear_history()
        logger.info("Conversation history cleared")
    
    def get_conversation_history(self):
        return self.llm.conversation_history


def main():
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )
    
    print("\n" + "="*80)
    print("🔄 Voice Conversation Pipeline Test")
    print("="*80)
    
    pipeline = VoiceConversationPipeline(
        stt_model="base",
        llm_model="EleutherAI/polyglot-ko-1.3b"
    )
    
    # 테스트용 오디오 파일
    print("\n📁 Test 1: 오디오 파일 입력")
    # audio_file = "/Users/namung2/haru/Haru-Anbu/ai-core/studio_origin/M-GS-1/sample10.wav" 이거 맥용 하드코딩
    audio_file = "/home/namung/ai-core/Haru-Anbu/ai-core/data/studio_origin/W-SO-1/sample1.wav"
    if os.path.exists(audio_file):
        result = pipeline.process_audio(audio_path=audio_file)
        
        print(f"\n👴 어르신: {result['user_text']}")
        print(f"🤖 AI: {result['ai_response']}")
        print(f"🔊 생성된 음성 파일: {result['tts_file']}")
        
        print("\n⏱️ 성능:")
        print(f"   STT: {result['stt_duration']:.3f}s")
        print(f"   LLM: {result['llm_duration']:.3f}s")
        print(f"   TTS: {result['tts_duration']:.3f}s")
        print(f"   Total: {result['total_duration']:.3f}s")
    
    else:
        print(f"❌ 오디오 파일 없음: {audio_file}")

    print("\n📜 대화 히스토리")
    for i, msg in enumerate(pipeline.get_conversation_history(), 1):
        role = "👴 어르신" if msg["role"] == "user" else "🤖 AI"
        print(f"{i}. {role}: {msg['content']}")

    print("\n✅ All tests completed!")


if __name__ == "__main__":
    main()
