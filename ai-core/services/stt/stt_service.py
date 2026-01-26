#services/stt/stt_service/stt_service.py
from faster_whisper import WhisperModel
import torch
import time
import logging
from pathlib import Path
from typing import Optional
import numpy as np

logger = logging.getLogger(__name__)

class STTService: 
    
    def __init__(self, model_size: str = "medium", device: str = "cuda"):
        self.model_size = model_size
        self.device = device
        # compute_type은 CPU일 경우 int8, GPU(CUDA)일 경우 float16 권장
        self.compute_type = "float16" if device == "cuda" else "int8"
        # 어차피 빠르니까 init 시 바로 모델 load -> warm up
        try:
            start_time = time.time()
            # 모델 로드 (tip: Faster-Whisper는 C++이래서 빠르다네요)
            self.model = WhisperModel(
                self.model_size, 
                device=self.device, 
                compute_type=self.compute_type,
                download_root="../../models//stt/faster-whisper" # 여기에다가 깔기
            )
            self._warmup()

            load_time = time.time() - start_time

            logger.info(f"🚀 Faster-Whisper ({model_size}) initialized on {device} time: {load_time:.2f}s")
        except Exception as e:
            logger.error(f"Failed to load Whisper model: {e}")
            raise
    
    def _warmup(self):
        """Faster-Whisper 모델 웜업 - 첫 인식 지연 방지"""
        try:
            logger.info("🔥 Warming up Faster-Whisper model...")
            # 1.5초 분량의 무음 오디오 생성 (16kHz, float32)
            # 웜업 시에는 실제 추론과 동일한 데이터 형식을 넣어주는 게 좋습니다.
            dummy_audio = np.zeros(16000 * 1, dtype=np.float32)
            
            # transcribe를 한 번 실행하여 내부 엔진을 활성화
            list(self.model.transcribe(dummy_audio, language="ko"))
            
            logger.info("✅ Faster-Whisper warmup completed")
        except Exception as e:
            logger.warning(f"⚠️ Warmup failed: {e}")
    
    
    def transcribe_stream(self, audio_data: np.ndarray) -> Optional[str]:
        """
        최소 길이 검증 + Faster-Whisper VAD를 결합한 최적화 인식
        """
        # 1. 방어 로직: 너무 짧은 데이터는 연산 자원 낭비이므로 컷 
        # 16kHz 기준 0.5초(8000 샘플) 미만은 무시
        if len(audio_data) < 16000 * 0.5:
            return None

        start_time = time.time()
        try:
            # 2. Faster-Whisper 추론
            # beam_size=1: 속도를 위해 가장 확률 높은 단어 하나만 선택 (실시간성 핵심)
            segments, info = self.model.transcribe(
                audio_data,
                beam_size=5, 
                language="ko",
                vad_filter=True,
                vad_parameters=dict(min_silence_duration_ms=700),
                initial_prompt="할머니와 손주의 다정한 대화입니다. 일상적인 안부와 식사 메뉴에 대해 이야기합니다.",
                condition_on_previous_text=False # 환청 방지 및 속도 향상
            )
            # 제너레이터에서 텍스트 추출
            segments = list(segments) # 제너레이터 실행
            text = "".join([s.text for s in segments]).strip()

            
            
            if text:
                duration = time.time() - start_time
                logger.info(f"👴 인식 결과: {text} ({duration:.3f}s)")
                return text ,info
                
            return None, None

        except Exception as e:
            logger.error(f"STT transcription failed: {e}")
            return None , None
        
    
    def unload_model(self):
        """메모리 절약을 위한 모델 언로드"""
        if self.model is not None:
            del self.model
            self.model = None
            
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            logger.info("STT model unloaded")


# 싱글톤 패턴으로 사용할 전역 인스턴스 주입을 위함.
_stt_service_instance: Optional[STTService] = None


def get_stt_service(model_size: str = "base") -> STTService:
    """STT 서비스 싱글톤 인스턴스 반환"""
    global _stt_service_instance
    
    if _stt_service_instance is None:
        _stt_service_instance = STTService(model_size=model_size)
    
    return _stt_service_instance