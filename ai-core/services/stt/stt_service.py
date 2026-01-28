# services/stt/stt_service/stt_service.py
from faster_whisper import WhisperModel
import torch
import time
import logging
from pathlib import Path
from typing import Optional
import numpy as np

logger = logging.getLogger(__name__)

class STTService: 
    
    def __init__(self, model_size: str = "small", device: str = "cuda"):
        self.model_size = model_size
        self.device = device
        # compute_type은 CPU일 경우 int8, GPU(CUDA)일 경우 float16 권장
        self.compute_type = "float16" if device == "cuda" else "int8"
        
        try:
            start_time = time.time()
            # 모델 로드
            self.model = WhisperModel(
                self.model_size, 
                device=self.device, 
                compute_type=self.compute_type,
                download_root="../../models/stt/faster-whisper" 
            )
            self._warmup()

            load_time = time.time() - start_time
            logger.info(f"🚀 Faster-Whisper ({model_size}) initialized on {device} time: {load_time:.2f}s")
        except Exception as e:
            logger.error(f"Failed to load Whisper model: {e}")
            raise
    
    def _warmup(self):
        """Faster-Whisper 모델 웜업"""
        try:
            logger.info("🔥 Warming up Faster-Whisper model...")
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
        if len(audio_data) < 16000 * 0.5:
            return None, None

        start_time = time.time()
        try:
            # 2. Faster-Whisper 추론
            segments, info = self.model.transcribe(
                audio_data,
                beam_size=5, 
                language="ko",
                
                # ★ 핵심 1: VAD 설정을 약간 완화하여 끊김 방지
                vad_filter=True,
                vad_parameters=dict(min_silence_duration_ms=500, threshold=0.5),
                
                # ★ 핵심 2: 환각 방지용 프롬프트 유지
                initial_prompt="할머니와 손주의 다정한 대화입니다. 반말과 존댓말이 섞일 수 있습니다.",
                
                # ★ 핵심 3: 환각 억제 파라미터 추가
                condition_on_previous_text=False, # 이전 텍스트 맥락 끊기 (환각 루프 방지)
                repetition_penalty=1.2,           # 반복되는 단어 억제
                temperature=0.2,                  # 창의성 낮춤 (정확도 우선)
            )
            
            segments = list(segments) 
            text = "".join([s.text for s in segments]).strip()

            if text:
                duration = time.time() - start_time
                logger.info(f"👴 인식 결과: {text} ({duration:.3f}s)")
                return text, info
                
            return None, None

        except Exception as e:
            logger.error(f"STT transcription failed: {e}")
            return None, None
        
    
    def unload_model(self):
        if self.model is not None:
            del self.model
            self.model = None
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            logger.info("STT model unloaded")


# 싱글톤 패턴
_stt_service_instance: Optional[STTService] = None

# ★ 여기가 가장 중요합니다. 기본값을 base -> medium로 변경
def get_stt_service(model_size: str = "medium") -> STTService:
    """STT 서비스 싱글톤 인스턴스 반환"""
    global _stt_service_instance
    
    if _stt_service_instance is None:
        _stt_service_instance = STTService(model_size=model_size)
    
    return _stt_service_instance