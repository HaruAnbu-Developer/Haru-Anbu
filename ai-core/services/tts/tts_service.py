import torch
import time
import logging
import numpy as np
from typing import Optional, Dict, Any, Union
from pathlib import Path
from TTS.api import TTS
import soundfile as sf

logger = logging.getLogger(__name__)


class TTSService:
    """
    Coqui XTTS 기반 음성 합성 서비스 (speaker_wav 기반)
    """

    def __init__(
        self,
        model_name: str = "tts_models/multilingual/multi-dataset/xtts_v2",
        device: Optional[str] = None,
        speaker_wav: str = "/Users/namung2/haru/Haru-Anbu/ai-core/studio_origin/W-SO-1/sample1.wav",
        language: str = "ko"
    ):
        """
        Args:
            model_name: 사용할 TTS 모델 이름
            device: 'cuda', 'mps', 'cpu' 또는 None
            speaker_wav: 화자 임베딩 생성용 샘플 음성 파일
            language: 합성할 언어
        """
        self.model_name = model_name
        self.language = language
        self.speaker_wav = speaker_wav

        # 디바이스 자동 감지
        if device is None:
            if torch.backends.mps.is_available():
                self.device = "mps"
            elif torch.cuda.is_available():
                self.device = "cuda"
            else:
                self.device = "cpu"
        else:
            self.device = device

        self.tts = None
        self.sample_rate = 22050

        # speaker_wav 파일 존재 확인
        if not Path(self.speaker_wav).exists():
            raise FileNotFoundError(f"Speaker wav file not found: {self.speaker_wav}")

        self._load_model()

        logger.info(
            f"TTS Service initialized: model={model_name}, device={self.device}, speaker_wav={self.speaker_wav}"
        )

    # ------------------------------------------------------------------------------------
    # MODEL LOADING
    # ------------------------------------------------------------------------------------
    def _load_model(self):
        """TTS 모델 로드"""
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

    # ------------------------------------------------------------------------------------
    # WARMUP
    # ------------------------------------------------------------------------------------
    def _warmup(self):
        """모델 첫 실행 시간 단축을 위한 Warm-up"""
        try:
            logger.info("Warming up TTS model...")
            _ = self.synthesize("안녕하세요", save_path=None)
            logger.info("TTS warmup completed")
        except Exception as e:
            logger.warning(f"TTS warmup failed: {e}")

    # ------------------------------------------------------------------------------------
    # TEXT SYNTHESIS
    # ------------------------------------------------------------------------------------
    def synthesize(
        self,
        text: str,
        save_path: Optional[str] = None,
        speed: float = 1.0
    ) -> Dict[str, Any]:

        start_time = time.time()
        text = self._preprocess_text(text)

        try:
            if save_path:
                self.tts.tts_to_file(
                    text=text,
                    file_path=save_path,
                    speed=speed,
                    speaker_wav=self.speaker_wav,
                    language=self.language
                )
                audio, sr = sf.read(save_path)

            else:
                audio = self.tts.tts(
                    text=text,
                    speed=speed,
                    speaker_wav=self.speaker_wav,
                    language=self.language
                )
                audio = np.array(audio, dtype=np.float32)
                sr = self.sample_rate

            duration = time.time() - start_time
            audio_length = len(audio) / sr

            return {
                "audio": audio,
                "sample_rate": sr,
                "duration": duration,
                "audio_length": audio_length,
                "file_path": save_path
            }

        except Exception as e:
            logger.error(f"TTS synthesis failed: {e}")
            raise

    # ------------------------------------------------------------------------------------
    # STREAMING
    # ------------------------------------------------------------------------------------
    def synthesize_streaming(self, text: str, chunk_size: int = 512):
        result = self.synthesize(text)
        audio = result["audio"]

        for i in range(0, len(audio), chunk_size):
            yield audio[i:i + chunk_size]

    # ------------------------------------------------------------------------------------
    # TEXT PREPROCESSING
    # ------------------------------------------------------------------------------------
    def _preprocess_text(self, text: str) -> str:
        text = text.strip().replace("\n", " ")

        import re
        text = re.sub(r'\s+', ' ', text)

        if len(text) > 200:
            logger.warning(f"Text too long ({len(text)} chars), truncating to 200")
            text = text[:200]

        return text

    # ------------------------------------------------------------------------------------
    # BATCH
    # ------------------------------------------------------------------------------------
    def batch_synthesize(self, texts: list, output_dir: str = "./outputs/tts"):
        Path(output_dir).mkdir(parents=True, exist_ok=True)
        results = []

        for i, text in enumerate(texts):
            file_path = f"{output_dir}/output_{i:03d}.wav"
            results.append(self.synthesize(text, save_path=file_path))

        return results

    # ------------------------------------------------------------------------------------
    # MODELS LIST
    # ------------------------------------------------------------------------------------
    def get_available_models(self) -> list:
        try:
            models = TTS().list_models()
            return models if isinstance(models, list) else []
        except Exception as e:
            logger.error(f"Failed to get model list: {e}")
            return []

    # ------------------------------------------------------------------------------------
    # DEVICE INFO
    # ------------------------------------------------------------------------------------
    def get_device_info(self) -> Dict[str, Any]:
        info = {
            "device": self.device,
            "model_name": self.model_name,
            "sample_rate": self.sample_rate,
            "cuda_available": torch.cuda.is_available(),
            "mps_available": torch.backends.mps.is_available()
        }
        return info

    # ------------------------------------------------------------------------------------
    # SAVE
    # ------------------------------------------------------------------------------------
    def save_audio(self, audio: np.ndarray, file_path: str, sample_rate: Optional[int] = None):
        sr = sample_rate or self.sample_rate
        Path(file_path).parent.mkdir(parents=True, exist_ok=True)
        sf.write(file_path, audio, sr)

    # ------------------------------------------------------------------------------------
    # UNLOAD
    # ------------------------------------------------------------------------------------
    def unload_model(self):
        if self.tts:
            del self.tts
            self.tts = None
            logger.info("TTS model unloaded")


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

