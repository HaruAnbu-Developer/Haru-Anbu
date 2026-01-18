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

            self._warmup()

        except Exception as e:
            logger.error(f"Failed to load TTS model: {e}")
            raise

    def _warmup(self):
        """모델 엔진 예열"""
        try:
            logger.info("🔥 TTS 엔진 예열 중...")
            # 모델의 기본 스피커 정보 활용 (별도 파일 로드 방지)
            spk_name = self.tts.speaker_manager.speaker_names[0]
            latents = {
                "gpt_cond_latent": self.tts.speaker_manager.speakers[spk_name]["gpt_cond_latent"],
                "speaker_embedding": self.tts.speaker_manager.speakers[spk_name]["embedding"]
            }
            
            # 실제 인퍼런스 엔진을 한 번 태움 (결과는 무시)
            gen = self.tts.model.inference_stream("아", "ko", latents["gpt_cond_latent"], latents["speaker_embedding"])
            for _ in gen: break 
            
            logger.info("✅ TTS warmup completed")
        except Exception as e:
            logger.warning(f"⚠️ TTS warmup failed: {e}")


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
        """
        [핵심] 실시간 스트리밍 합성: Latent 데이터를 직접 주입하여 텍스트를 음성 바이트로 변환
        """
        text = self._preprocess_text(text)
        if not text: return

        try:
            # XTTS v2의 inference_stream 사용
            # 파일 경로 대신 미리 준비된 gpt_cond_latent와 speaker_embedding 사용
            chunks = self.tts.model.inference_stream(
                text=text,
                language=self.language,
                gpt_cond_latent=latents["gpt_cond_latent"],
                speaker_embedding=latents["speaker_embedding"],
                enable_text_splitting=True # 긴 문장 대응
            )

            for chunk in chunks:
                # 1. 텐서를 CPU로 옮기고 Numpy 변환
                audio_np = chunk.cpu().numpy()
                
                # 2. Float32 -> Int16 (PCM 16-bit) 변환 (전화망 전송용)
                # XTTS는 -1~1 사이의 float32를 뱉으므로 32767을 곱해줍니다.
                audio_int16 = (audio_np * 32767).astype(np.int16)
                
                # 3. 바이트로 변환하여 전송
                yield audio_int16.tobytes()

        except Exception as e:
            logger.error(f"Streaming synthesis failed: {e}")
            
#----------------------------------------------------------------------------------
    def extract_latents(self, audio_path: str) -> Dict[str, Any]:
        """
        S3 등에서 다운로드한 wav 파일로부터 XTTS 전용 특징(Latent)을 추출합니다.
        """
        try:
            if not Path(audio_path).exists():
                raise FileNotFoundError(f"Audio file not found: {audio_path}")
                
            # 모델의 내부 메서드를 사용하여 특징 추출
            gpt_cond_latent, speaker_embedding = self.tts.model.get_conditioning_latents(audio_path=[audio_path])
            
            return {
                "gpt_cond_latent": gpt_cond_latent,
                "speaker_embedding": speaker_embedding
            }
        except Exception as e:
            logger.error(f"Latent extraction failed: {e}")
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

