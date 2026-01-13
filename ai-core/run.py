import os
import time
import logging
from typing import Dict, Any, Optional
from services.STT.stt_service import STTService
from services.LLM.llm_service_Gemma_stream import LLMService
from services.TTS.tts_service import TTSService   # 🔥 TTS 서비스 추가
import numpy as np
import re  # 정규표현식(특수문자 제거)을 위해 필요합니다!
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
        self.llm = LLMService(model_path=llm_model)
        self.tts = TTSService(model_name=tts_model)   # 🔥 TTS 추가
        
        logger.info("Pipeline ready!")
    
    def process_audio_stream(
        self,
        audio_path: Optional[str] = None,
        audio_data: Optional[Any] = None
    ):
        """
        STT -> LLM(Streaming) -> TTS(Sentence by Sentence) 파이프라인
        """
        # 1. STT (사용자 음성을 텍스트로)
        logger.info("Processing STT...")
        stt_result = self.stt.transcribe(audio_path=audio_path, audio_data=audio_data)
        user_text = stt_result["text"]
        
        # UI에 사용자 텍스트를 먼저 보여주기 위해 yield
        yield {"type": "user_text", "content": user_text}

        # 2. LLM 스트리밍 시작
        logger.info("Starting LLM streaming...")
        full_response = ""
        
        # LLMService의 generate_stream 호출
        for sentence in self.llm.generate_stream(user_text):
            # 문장 내 불필요한 이모티콘/특수문자 제거 (TTS 오류 방지)
            clean_sentence = re.sub(r'[^가-힣a-zA-Z0-9\s.\?\!\,]', '', sentence).strip()
            if not clean_sentence:
                continue

            # 3. TTS 생성 (문장 단위로 즉시 연산)
            wav_list = self.tts.tts.synthesizer.tts(
                clean_sentence,
                None,
                self.tts.language,
                self.tts.speaker_wav
            )
            
            full_response += clean_sentence + " "
            
            # 오디오 데이터와 함께 문장 반환
            yield {
                "type": "audio_chunk",
                "ai_sentence": clean_sentence,
                "audio": np.array(wav_list)
            }

        # 대화 기록 저장 (히스토리 관리)
        self.llm.conversation_history.append({"role": "user", "content": user_text})
        self.llm.conversation_history.append({"role": "assistant", "content": full_response.strip()})
    
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
        llm_model="./models/llm/gemma-2-9b-it-Q5_K_M.gguf"
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
