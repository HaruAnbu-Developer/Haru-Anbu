import whisper
import torch
import time
import logging
from pathlib import Path
from typing import Optional, Dict, Any
import numpy as np

logger = logging.getLogger(__name__)

# 음성인식 서비스 클래스 Whisper 기반
class STTService: 
    
    def __init__(self, model_size: str = "tiny", device: Optional[str] = None):
        """
        Args: option 선택하기
            model_size: Whisper 모델 크기 ('tiny', 'base', 'small', 'medium', 'large')
            device: 'cuda' 또는 'cpu'. None이면 자동 감지
        """
        self.model_size = model_size
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu") #지금은 cuda 없으니까 그냥 cpu
        self.model = None
        self._load_model()
        
        logger.info(f"!!!STT Service initialized with model={model_size}, device={self.device}")
    
    def _load_model(self):
        """Whisper 모델 로드"""
        try:
            start_time = time.time()
            logger.info(f"Loading Whisper {self.model_size} model...")
            
            self.model = whisper.load_model(
                self.model_size,
                device=self.device,
                download_root="./models/whisper"  # 모델 저장 경로
            )
            
            load_time = time.time() - start_time
            logger.info(f"Whisper model loaded in {load_time:.2f}s")
            
            # 모델 워밍업
            self._warmup()
            
        except Exception as e:
            logger.error(f"Failed to load Whisper model: {e}")
            raise
    
    def _warmup(self):
        """모델 워밍업 - 첫 추론 속도 개선"""
        try:
            logger.info("Warming up STT model...")
            # 1초 더미 오디오 생성 (16kHz)
            dummy_audio = np.zeros(16000, dtype=np.float32)
            self.model.transcribe(dummy_audio, language="ko", fp16=False)
            logger.info("STT warmup completed")
        except Exception as e:
            logger.warning(f"Warmup failed: {e}")
    
    # 음성 파일 또는 오디오 데이터로부터 텍스트 변환 함수
    def transcribe(
        self,
        audio_path: Optional[str] = None,
        audio_data: Optional[np.ndarray] = None,
        language: str = "ko" # 기본값: 한국어
    ) -> Dict[str, Any]:
        """
        음성을 텍스트로 변환
        
        Args:
            audio_path: 오디오 파일 경로
            audio_data: numpy array 형태의 오디오 데이터 (16kHz) -> 실시간 streaming용(전화 등)
            language: 언어 코드 (기본값: "ko")
        
        Returns:
            {
                "text": str,           # 변환된 텍스트
                "duration": float,     # 처리 시간 (초)
                "language": str,       # 감지된 언어
                "segments": list       # 세그먼트 정보 (옵션)
            }
        """
        if audio_path is None and audio_data is None:
            raise ValueError("Either audio_path or audio_data must be provided")
        
        start_time = time.time()
        
        try:
            # 오디오 소스 결정
            audio_source = audio_path if audio_path else audio_data
            
            # Whisper 옵션 설정
            options = {
                "language": language,
                "task": "transcribe",
                "fp16": self.device == "cuda",  # GPU에서는 FP16 사용
                "verbose": False
            }
            
            # 변환 실행
            result = self.model.transcribe(audio_source, **options)
            
            duration = time.time() - start_time
            
            # 성능 모니터링 대충 어느정도 나오는지.. rough 하게 잡은 시간
            if duration > 0.5:
                logger.warning(f"STT processing took {duration:.3f}s (target: 0.5s)")
            else:
                logger.info(f"STT processing completed in {duration:.3f}s")
            
            return {
                "text": result["text"].strip(),
                "duration": duration,
                "language": result.get("language", language),
                "segments": result.get("segments", [])
            }
            
        except Exception as e:
            logger.error(f"STT transcription failed: {e}")
            raise
    
    def transcribe_stream(self, audio_chunk: np.ndarray) -> Optional[str]:
        """
        실시간 스트리밍 음성 변환
        
        Args:
            audio_chunk: 오디오 청크 (numpy array)
        
        Returns:
            변환된 텍스트 또는 None (음성이 짧으면)
        """
        # 최소 0.5초 이상의 오디오만 처리
        min_length = 16000 * 0.5  # 16kHz * 0.5초
        
        if len(audio_chunk) < min_length:
            logger.debug(f"Audio chunk too short: {len(audio_chunk)} samples")
            return None
        
        try:
            result = self.transcribe(audio_data=audio_chunk)
            return result["text"]
        except Exception as e:
            logger.error(f"Stream transcription failed: {e}")
            return None
    
    #  CPU 혹은 GPU 정보 반환
    def get_device_info(self) -> Dict[str, Any]:
        """현재 디바이스 정보 반환"""
        info = {
            "device": self.device,
            "model_size": self.model_size,
            "cuda_available": torch.cuda.is_available()
        }
        
        if torch.cuda.is_available():
            info["gpu_name"] = torch.cuda.get_device_name(0)
            info["gpu_memory_allocated"] = torch.cuda.memory_allocated(0) / 1024**3  # GB
            info["gpu_memory_reserved"] = torch.cuda.memory_reserved(0) / 1024**3  # GB
        
        return info
    
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


# if __name__ == "__main__":
#     # 간단한 테스트
#     logging.basicConfig(level=logging.INFO)
    
#     print("STT Service Test")
#     print("=" * 50)
    
#     # 서비스 초기화
#     stt = STTService(model_size="base")
    
#     # 디바이스 정보 출력
#     device_info = stt.get_device_info()
#     print(f"\nDevice Info:")
#     for key, value in device_info.items():
#         print(f"  {key}: {value}")

#     audio_test_path = "/Users/namung2/haru/Haru-Anbu/ai-core/studio_origin/M-GS-1/sample.wav" # 테스트용 오디오 파일 경로 설정
#     result = stt.transcribe(audio_path=audio_test_path)
#     print(f"\nTranscription: {result['text']}")
#     print(f"Duration: {result['duration']:.3f}s")
    