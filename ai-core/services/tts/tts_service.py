#services/tts/tts_service/py
import torch
import time
import logging
import numpy as np
from typing import Optional, Dict, Any, Generator
from pathlib import Path
from TTS.api import TTS
import soundfile as sf

logger = logging.getLogger(__name__)


class TTSService:
    """
    XTTS_v2 기반 음성 합성 서비스
    """
    def __init__(
        self,
        model_name: str = "tts_models/multilingual/multi-dataset/xtts_v2",
        device: Optional[str] = None,
        language: str = "ko"
    ):
        self.model_name = model_name
        self.language = language
        self.device = device if device else ("cuda" if torch.cuda.is_available() else "cpu")
        
        self.tts = None
        self.sample_rate = 24000  # XTTS v2는 기본적으로 24kHz 출력입니다.
        self._load_model()
        logger.info(
            f"TTS Service initialized: model={model_name}, device={self.device}"
        )


    def _load_model(self):
        try:
            logger.info(f"Loading TTS model: {self.model_name}...")
            start_time = time.time()

            self.tts = TTS(
                model_name=self.model_name,
                progress_bar=False,
                gpu=(self.device == "cuda")
            )

            load_time = time.time() - start_time
            logger.info(f"TTS model loaded in {load_time:.2f}s")


        except Exception as e:
            logger.error(f"Failed to load TTS model: {e}")
            raise
    
    def run_actual_warmup(self, latents: dict):
        try:
            model_engine = self.tts.synthesizer.tts_model
            # 스트리밍 대신 일반 추론 호출
            _ = model_engine.inference(
                text="hello",
                language="ko",
                gpt_cond_latent=latents["gpt_cond_latent"],
                speaker_embedding=latents["speaker_embedding"]
            )
            logger.info("✅ 일반 추론 방식으로 예열 성공")
        except Exception as e:
            logger.error(f"❌ 일반 추론 예열도 실패: {e}")


    def _preprocess_text(self, text: str) -> str:
        text = text.strip().replace("\n", " ")

        import re
        text = re.sub(r'\s+', ' ', text)

        if len(text) > 200:
            logger.warning(f"Text too long ({len(text)} chars), truncating to 200")
            text = text[:200]

        return text
    

    def unload_model(self):
        if self.tts:
            del self.tts
            self.tts = None
            logger.info("TTS model unloaded")

    async def synthesize_stream(self, text: str, latents: Dict[str, Any]) -> Generator[bytes, None, None]:
        text = self._preprocess_text(text)
        if not text: return

        try:
            # 엔진 참조
            model_engine = self.tts.synthesizer.tts_model
            
            # 스트리밍 추론 실행
            # XTTS v2는 generator를 반환합니다.
            chunks = model_engine.inference_stream(
                text=text,
                language=self.language,
                gpt_cond_latent=latents["gpt_cond_latent"],
                speaker_embedding=latents["speaker_embedding"],
                enable_text_splitting=True
            )

            for chunk in chunks:
                # 텐서를 numpy 바이트로 변환 (PCM 16bit)
                audio_np = chunk.cpu().numpy()
                audio_int16 = (audio_np * 32767).astype(np.int16)
                yield audio_int16.tobytes()

        except Exception as e:
            logger.error(f"❌ Streaming synthesis failed: {e}")
            # 에러 발생 시 추적을 위해 스택트레이스 출력 추가 가능
            import traceback
            logger.error(traceback.format_exc())
            
#----------------------------------------------------------------------------------
    def extract_latents(self, audio_path: str) -> Dict[str, Any]:
        """
        S3 등에서 다운로드한 wav 파일로부터 XTTS 전용 특징(Latent)을 추출합니다.
        """
        try:
            if not Path(audio_path).exists():
                raise FileNotFoundError(f"Audio file not found: {audio_path}")
            
            logger.info(f"🎙️ 특징(Latent) 추출 시작: {audio_path}")

            # 1. 실제 모델 엔진 참조
            model_engine = self.tts.synthesizer.tts_model
            
            # 2. 모델의 내부 메서드를 사용하여 특징 추출
            # audio_path는 리스트 형태로 전달해야 합니다.
            gpt_cond_latent, speaker_embedding = model_engine.get_conditioning_latents(
                audio_path=[audio_path]
            )
            
            logger.info(f"✅ 특징 추출 성공: {audio_path}")
            return {
                "gpt_cond_latent": gpt_cond_latent,
                "speaker_embedding": speaker_embedding
            }
            
        except Exception as e:
            logger.error(f"❌ Latent extraction failed: {e}")
            raise

# Singleton
_tts_service_instance: Optional[TTSService] = None


def get_tts_service(
    model_name: str = "tts_models/multilingual/multi-dataset/xtts_v2"
) -> TTSService:
    global _tts_service_instance

    if _tts_service_instance is None:
        _tts_service_instance = TTSService(
            model_name=model_name
        )

    return _tts_service_instance

