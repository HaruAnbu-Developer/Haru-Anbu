# services/call_service/call_service.py
import grpc
import asyncio
import logging
import numpy as np
from services.stt.stt_service import get_stt_service
from services.llm.llm_service_Gemma_stream import get_llm_service
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager
import server.grpc.voice_stream_pb2 as voice_stream_pb2
import server.grpc.voice_stream_pb2_grpc as voice_stream_pb2_grpc

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logging.getLogger("faster_whisper").setLevel(logging.WARNING)
import os
import sys

# 가상환경 내 NVIDIA 라이브러리 경로 탐색 및 추가
venv_base = os.path.join(os.getcwd(), ".venvw", "Lib", "site-packages", "nvidia")
paths_to_add = [
    os.path.join(venv_base, "cublas", "bin"),
    os.path.join(venv_base, "cudnn", "bin")
]

for p in paths_to_add:
    if os.path.exists(p):
        os.environ["PATH"] += os.pathsep + p

# Windows에서 Python 3.8 이상일 경우 DLL 경로 추가 예약
if sys.platform == 'win32':
    for p in paths_to_add:
        if os.path.exists(p):
            os.add_dll_directory(p)

class VoiceService(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        self.stt_service = get_stt_service()
        self.llm_service = get_llm_service()
        self.tts_service = get_tts_service()
        self.latent_manager= get_latent_manager()

    async def StreamConversation(self, request_iterator, context):
        user_id = "test_user_1"
        # Raw PCM 바이트를 모을 버퍼
        audio_buffer = bytearray()
        
        # 16kHz, 16-bit PCM에서 1초는 32,000바이트입니다. (16000 * 2바이트)
        # 어르신의 말을 인식하기 위해 약 1.5초~2초 정도의 데이터가 모였을 때 STT를 시도합니다.
        # (이 값은 실시간성을 보며 1.0초 정도로 줄여도 됩니다.)
        MIN_PROCESS_SAMPLES = 16000 * 1.5
        MIN_PROCESS_BYTES = int(MIN_PROCESS_SAMPLES * 2) # 16-bit = 2 bytes
        try:
            async for request in request_iterator:
                payload_type = request.WhichOneof("payload")

                if payload_type == "config":
                    user_id = request.config.user_id
                    success = self.latent_manager.prepare_user(user_id, f"latents/{user_id}/{user_id}_latent.pth")
                    logger.info(f"🎙️ 세션 시작 (User ID: {user_id})")
                    continue
                if not success:
                    logger.error(f"❌ {user_id}의 목소리 데이터를 로드할 수 없습니다.")

                elif payload_type == "audio_content":
                    if not user_id:
                        continue
                    
                    # 20~40ms 조각을 버퍼에 추가
                    audio_buffer.extend(request.audio_content)

                    # 설정한 버퍼 크기(예: 1.5초)를 넘었을 때 분석 시작
                    if len(audio_buffer) >= MIN_PROCESS_BYTES:
                        # [포맷 변환] Signed 16-bit Little Endian -> Float32 Numpy
                        # Whisper 모델은 -1.0 ~ 1.0 사이의 float32 데이터를 입력으로 받습니다.
                        raw_data = np.frombuffer(audio_buffer, dtype=np.int16).copy()
                        rms = np.sqrt(np.mean(raw_data.astype(np.float32)**2))
                        if rms < 300: # 300은 마이크 감도에 따라 조절 (보통 100~500 사이)
                            # 소리가 너무 작으면 앞부분만 살짝 쳐내고 다음을 기다림
                            del audio_buffer[:16000] # 0.5초치 삭제
                            continue
                        audio_np = raw_data.astype(np.float32) / 32768.0
                        
                        # STT 실행 (Faster-Whisper)
                        recognized_text, info = self.stt_service.transcribe_stream(audio_np)
                        
                        if recognized_text:
                            logger.info(f"👴 어르신 인식: {recognized_text}")
                            if info and hasattr(info, 'duration'):
                                # 처리된 바이트 계산 (초 * 샘플레이트 * 2바이트)
                                processed_bytes = int(info.duration * 16000 * 2)
                                safe_del = min(processed_bytes, len(audio_buffer))
                                # 처리된 분만 삭제
                                del audio_buffer[:safe_del]

                                # 방법 1의 철학 적용: ->일단 테스트 해보고 안되면 곂치는 시간을 0.5 초로 늘려보자 (최적화 이슈)
                                # 인식 결과가 나왔더라도 다음 인식의 자연스러운 연결을 위해 
                                # 아주 짧은 0.1~0.2초 정도는 겹치게 남겨둘 수도 있습니다.                            
                            else:
                                # info가 제대로 안 들어올 경우를 대비한 방어 로직 끊겨도 -> 일단 유지를 해야하니
                                logger.warning("⚠️ STT info duration missing, keeping buffer for next chunk.")
                                # audio_buffer.clear() 일단 주석처리


                            # LLM & TTS 파이프라인 가동
                            async for audio_out in self.process_ai_response_chain(user_id, recognized_text):
                                yield voice_stream_pb2.VoiceResponse(audio_output=audio_out)
                        
                        # 버퍼가 너무 무한정 커지지 않도록 오래된 데이터는 조금씩 밀어내기
                        elif len(audio_buffer) > MIN_PROCESS_BYTES * 3:
                            # 3배수(약 4.5초) 이상 쌓여도 인식이 안 되면 앞부분 1초 삭제
                            del audio_buffer[:16000 * 2]
        finally:
            if user_id:
                self.latent_manager.release_user(user_id)
                logger.info(f"🗑️ {user_id} 세션 종료 및 메모리 해제")
            
                    
    async def process_ai_response_chain(self, user_id, text):
        latents = self.latent_manager.get_latent(user_id)
        if not latents:
            logger.error(f"❌ {user_id}의 Latent를 찾을 수 없습니다.")
            return

        # 단일 루프로 통합하여 즉시 TTS로 전달
        async for sentence in self.llm_service.ask_stream_sentences(text, instruction="당신은 다정한 손주입니다.어르신의 말씀에 강한 공감을 해주고 한문장으로 답하세요 이모티콘 금지."):
            logger.info(f"🤖 자녀(LLM): {sentence}")
            
            async for audio_bytes in self.tts_service.synthesize_stream(sentence, latents):
                yield audio_bytes


async def serve():
    server = grpc.aio.server()
    voice_stream_pb2_grpc.add_VoiceConversationServicer_to_server(VoiceService(), server)
    listen_addr = "[::]:50051"
    server.add_insecure_port(listen_addr)
    logger.info(f"Starting gRPC server on {listen_addr}")
    await server.start()
    await server.wait_for_termination()

if __name__ == "__main__":
    asyncio.run(serve())