import os
import sys
import logging

# Setup Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("TestTTS")

# Add ai-core to path
sys.path.append(os.path.join(os.getcwd(), 'ai-core'))

try:
    import torch
    from TTS.tts.configs.xtts_config import XttsConfig
    from TTS.config.shared_configs import BaseDatasetConfig, BaseAudioConfig, BaseTrainingConfig
    
    # Whitelist TTS classes for torch.load
    torch.serialization.add_safe_globals([
        XttsConfig, 
        BaseDatasetConfig, 
        BaseAudioConfig, 
        BaseTrainingConfig
    ])
except ImportError:
    pass # If specific imports fail, we continue and hope for best

try:
    from services.TTS.tts_service import TTSService
except ImportError as e:
    logger.error(f"Import Failed: {e}")
    sys.exit(1)

def test_tts():
    wav_path = "test.wav"
    output_path = "tts_verification.wav"
    
    if not os.path.exists(wav_path):
        logger.error(f"{wav_path} not found!")
        return

    logger.info("Initializing TTS Service...")
    try:
        tts = TTSService(speaker_wav=wav_path, language="ko")
        
        logger.info("Synthesizing text...")
        # Complex text with markdown, brackets, and newlines to test preprocessing
        text = """
        [MOCK SCRIPT]
        **안녕하세요!** 오늘은 첫 월급에 대한 사연을 소개해드리겠습니다.
        (웃음) 어르신들의 따뜻한 사연, 잘 들었습니다.
        
        건강하세요!
        """
        
        result = tts.synthesize(text, save_path=output_path)
        
        if os.path.exists(output_path):
            logger.info(f"SUCCESS: Audio generated at {output_path}")
            logger.info(f"Duration: {result.get('duration')}s")
        else:
            logger.error("FAILURE: File was not created.")
            
    except Exception as e:
        logger.error(f"TTS Failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_tts()
