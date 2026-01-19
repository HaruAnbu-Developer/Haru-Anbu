import pyaudio
import numpy as np
import logging
import time
import torch
from run import VoiceConversationPipeline
import re

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class MicrophoneInterface:
    def __init__(self, pipeline: VoiceConversationPipeline):
        self.pipeline = pipeline
        self.format = pyaudio.paInt16
        self.channels = 1
        self.input_rate = 16000   # 마이크(Whisper)용 샘플링 레이트
        self.output_rate = 24000  # 스피커(XTTS v2)용 샘플링 레이트
        self.chunk = 1024
        
        # 임계값 설정
        self.SILENCE_THRESHOLD = 100
        self.SILENCE_DURATION = 1.8  # 반응 속도를 위해 1.5에서 1.6로 증가 조금더 천천히
        
        self.audio = pyaudio.PyAudio()
        
        # 🔥 추가: 스피커 출력용 스트림을 미리 열어둡니다.
        self.output_stream = self.audio.open(
            format=pyaudio.paInt16,
            channels=1,
            rate=self.output_rate,
            output=True
        )

    def play_audio(self, audio_np):
        if audio_np is not None:
            # XTTS의 float32 데이터를 16비트 정수형 바이트로 변환
            audio_int16 = (audio_np * 32767).astype(np.int16).tobytes()
            self.output_stream.write(audio_int16)

    def start_listening(self):
        # 마이크 입력 스트림
        stream = self.audio.open(
            format=self.format,
            channels=self.channels,
            rate=self.input_rate,
            input=True,
            frames_per_buffer=self.chunk
        )

        logger.info("=== 🎤 실시간 음성 대화 시작 (종료하려면 Ctrl+C) ===")
        
        try:
            while True:
                frames = []
                is_speaking = False
                silent_chunks = 0
                
                # 1. 음성 감지 루프
                while True:
                    data = stream.read(self.chunk, exception_on_overflow=False)
                    audio_data = np.frombuffer(data, dtype=np.int16)
                    amplitude = np.abs(audio_data).mean()

                    if amplitude > self.SILENCE_THRESHOLD:
                        if not is_speaking:
                            logger.info("어르신이 말씀을 시작하셨습니다...")
                            is_speaking = True
                        frames.append(audio_data)
                        silent_chunks = 0
                    elif is_speaking:
                        frames.append(audio_data)
                        silent_chunks += 1
                        
                        if silent_chunks > int(self.SILENCE_DURATION * self.input_rate / self.chunk):
                            break
                    else:
                        continue
                    
                # 2. 파이프라인 처리 (Streaming 방식 호출)
                if is_speaking:
                    logger.info("음성을 처리 중입니다...")
                    audio_np = np.concatenate(frames).astype(np.float32) / 32768.0
                    
                    processing_start = time.time()
                    first_response = True

                    # 🔥 핵심: pipeline.process_audio_stream() 제너레이터를 순회합니다.
                    for result in self.pipeline.process_audio_stream(audio_data=audio_np):
                        
                        # 사용자 인식 결과 먼저 출력
                        if result["type"] == "user_text":
                            print(f"\n👴 어르신: {result['content']}")
                        
                        # AI 목소리 조각(문장 단위)이 도착하면 즉시 재생
                        elif result["type"] == "audio_chunk":
                            if first_response:
                                delay = time.time() - processing_start
                                logger.info(f"⏱️ 첫 문장 응답 지연 시간: {delay:.3f}s")
                                first_response = False
                            
                            print(f"🤖 AI: {result['ai_sentence']}")
                            self.play_audio(result['audio'])
                            
        except KeyboardInterrupt:
            logger.info("대화를 종료합니다.")
        finally:
            stream.stop_stream()
            stream.close()
            # 🔥 추가: 출력 스트림도 안전하게 닫기
            self.output_stream.stop_stream()
            self.output_stream.close()
            self.audio.terminate()

if __name__ == "__main__":
    # CUDA 체크
    print(f"CUDA Available: {torch.cuda.is_available()}")

    pipeline = VoiceConversationPipeline(
        stt_model="base",
        llm_model="./models/llm/gemma-2-9b-it-Q5_K_M.gguf"
    )
    
    mic = MicrophoneInterface(pipeline)
    mic.start_listening()