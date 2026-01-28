# services/stt/vad_service.py
import torch
import numpy as np
import logging

logger = logging.getLogger(__name__)

class VADService:
    def __init__(self):
        # Silero VAD 모델 로드 (자동 다운로드됨)
        # 매우 가벼워서 CPU로 돌려도 충분합니다.
        self.model, utils = torch.hub.load(
            repo_or_dir='snakers4/silero-vad',
            model='silero_vad',
            force_reload=False,
            onnx=False
        )
        (self.get_speech_timestamps, _, self.read_audio, _, _) = utils
        
        self.reset_states()
        logger.info("✅ Silero VAD initialized")

    def reset_states(self):
        """상태 초기화"""
        self.speaking = False      # 현재 말하고 있는 중인지
        self.silence_frames = 0    # 침묵이 얼마나 지속됐는지 카운트
        self.speech_frames = 0     # 말이 얼마나 지속됐는지 카운트
        
        # 튜닝 파라미터 (16kHz 기준, 512 샘플 = 32ms)
        # 1. 말 시작 감지: 3개 청크(약 100ms) 연속으로 확률 0.5 이상이면 말 시작
        self.START_THRESHOLD = 3 
        # 2. 말 끝 감지: 25개 청크(약 800ms) 연속으로 침묵이면 말 끝
        self.END_THRESHOLD = 25 
        # 3. 최소 발화 길이: 너무 짧은 소리(예: '틱') 무시용
        self.MIN_SPEECH_DURATION = 5

    def is_speech(self, audio_chunk_float32: np.ndarray, sample_rate=16000) -> str:
        """
        오디오 청크를 받아 현재 상태를 반환합니다.
        Return: 'SILENCE', 'SPEAKING', 'SPEECH_START', 'SPEECH_END'
        """
        # Silero VAD는 Tensor 입력을 받음
        audio_tensor = torch.from_numpy(audio_chunk_float32)
        
        # VAD 추론 (0.0 ~ 1.0 사이 확률 반환)
        speech_prob = self.model(audio_tensor, 16000).item()
        
        threshold = 0.5
        
        if speech_prob > threshold:
            # 소리가 감지됨
            self.silence_frames = 0
            self.speech_frames += 1
            
            if not self.speaking and self.speech_frames >= self.START_THRESHOLD:
                self.speaking = True
                return "SPEECH_START"
            
            return "SPEAKING"
        
        else:
            # 소리가 안 들림 (침묵)
            if self.speaking:
                self.silence_frames += 1
                # 침묵이 일정 시간(예: 0.8초) 이상 지속되면 "말 끝남" 판정
                if self.silence_frames >= self.END_THRESHOLD:
                    self.speaking = False
                    self.speech_frames = 0
                    self.silence_frames = 0
                    return "SPEECH_END"
            else:
                self.speech_frames = 0 # 노이즈 였던 것으로 간주하고 초기화
                
            return "SILENCE"

# 싱글톤
_vad_instance = None
def get_vad_service():
    global _vad_instance
    if _vad_instance is None:
        _vad_instance = VADService()
    return _vad_instance