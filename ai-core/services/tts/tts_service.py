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
        # speaker_wav: str = "/Users/namung2/haru/Haru-Anbu/ai-core/studio_origin/W-SO-1/sample1.wav",
        # speaker_wav: str = "/home/namung/ai-core/Haru-Anbu/ai-core/data/studio_origin/W-SO-1/sample1.wav",
        speaker_wav: str = r"C:\Haru-Anbu\ai-core\data\studio_origin\W-SO-1\sample1.wav",
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
        self.device = device if device else ("cuda" if torch.cuda.is_available() else "cpu")

        
        self.tts = None
        self.sample_rate = 22050


        if not Path(self.speaker_wav).exists():
            raise FileNotFoundError(f"Speaker wav file not found: {self.speaker_wav}")

        self._load_model()

        logger.info(
            f"TTS Service initialized: model={model_name}, device={self.device}, speaker_wav={self.speaker_wav}"
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
        """ Warm-up"""
        try:
            logger.info("Warming up TTS model...")
            _ = self.synthesize("안녕하세요", save_path=None)
            logger.info("TTS warmup completed")
        except Exception as e:
            logger.warning(f"TTS warmup failed: {e}")


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
                    language=self.language,
                    # [Stability Settings v2]
                    # 온도(Temperature)가 너무 낮으면(0.2) 오히려 기계적이고 딱딱한 억양이 생길 수 있음 -> 0.4로 상향
                    # Top-P/K로 엉뚱한 발음 차단
                    temperature=0.4, 
                    top_p=0.85,
                    top_k=50,
                    enable_text_splitting=True,
                    repetition_penalty=2.0, # 반복되는 톤 억제
                    length_penalty=1.2 # 문장을 너무 빨리 끊지 않도록
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

    def synthesize_streaming(self, text: str, chunk_size: int = 512):
        result = self.synthesize(text)
        audio = result["audio"]

        for i in range(0, len(audio), chunk_size):
            yield audio[i:i + chunk_size]

    def _preprocess_text(self, text: str) -> str:
        import re
        
        # 1. 마크다운 문법 제거 (볼드, 이탤릭 등)
        text = text.replace("**", "").replace("*", "")
        text = text.replace("`", "")
        
        # 2. 괄호 안의 내용 제거 (지문, 시스템 메시지 등) - 예: (웃음), [음악]
        text = re.sub(r'\[.*?\]', '', text)
        text = re.sub(r'\(.*?\)', '', text)
        
        # 3. 불필요한 공백 및 특수문자 정리
        text = text.replace("-", "") # 리스트 하이픈 제거
        text = text.strip()
        
        # 4. 줄바꿈을 적절한 휴식으로 변환 (점이나 쉼표가 없으면 추가)
        # 하지만 XTTS는 문장 단위처리가 좋으므로, 줄바꿈을 공백으로 하되 
        # 마침표가 없는 문장 끝에는 마침표를 찍어주는 것이 좋음
        lines = text.split('\n')
        cleaned_lines = []
        for line in lines:
            line = line.strip()
            if not line:
                continue
            # 문장 끝에 구두점이 없으면 마침표 추가 (자연스러운 휴식을 위해)
            if line[-1] not in ['.', '?', '!', ',', '~']:
                line += '.'
            cleaned_lines.append(line)
            
        text = " ".join(cleaned_lines)
        
        # 5. 다중 공백 제거
        text = re.sub(r'\s+', ' ', text)
        
        # 6. 길이 제한 제거 (라디오 대본은 길 수 있음)
        # 만약 메모리 문제가 발생하면 split해서 처리해야 하지만, 우선 제한 해제
        
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


    def get_available_models(self) -> list:
        try:
            models = TTS().list_models()
            return models if isinstance(models, list) else []
        except Exception as e:
            logger.error(f"Failed to get model list: {e}")
            return []

    def get_device_info(self) -> Dict[str, Any]:
        info = {
            "device": self.device,
            "model_name": self.model_name,
            "sample_rate": self.sample_rate,
            "cuda_available": torch.cuda.is_available(),
            "mps_available": torch.backends.mps.is_available()
        }
        return info

    
    def save_audio(self, audio: np.ndarray, file_path: str, sample_rate: Optional[int] = None):
        """파일(.wav)로 변환하는 함수"""
        sr = sample_rate or self.sample_rate
        Path(file_path).parent.mkdir(parents=True, exist_ok=True)
        sf.write(file_path, audio, sr)



    def unload_model(self):
        if self.tts:
            del self.tts
            self.tts = None
            logger.info("TTS model unloaded")
    def synthesize_streaming_real(self, text: str):
        """
        문장 전체를 기다리지 않고 생성되는 즉시 오디오 조각(chunk)을 반환합니다.{26/01/12}
        """
        text = self._preprocess_text(text)
        
        # XTTS v2의 stream_inference 제너레이터 사용
        chunks = self.tts.model.inference_stream(
            text=text,
            language=self.language,
            speaker_cond_set=self.tts.speaker_manager.speakers[self.tts.speaker_manager.speaker_names[0]]["embedding"], # 내부 임베딩 참조
            gpt_cond_latent=self.tts.speaker_manager.speakers[self.tts.speaker_manager.speaker_names[0]]["gpt_cond_latent"]
        )

        for chunk in chunks:
            # chunk는 torch.Tensor 형태이므로 numpy로 변환
            yield chunk.cpu().numpy()


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

