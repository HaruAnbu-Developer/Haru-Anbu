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

class VoiceService(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        self.stt_service = get_stt_service()
        self.llm_service = get_llm_service()
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager()

    async def StreamConversation(self, request_iterator, context):
        user_id = None
        # Raw PCM 바이트를 모을 버퍼
        audio_buffer = bytearray()
        
        # 16kHz, 16-bit PCM에서 1초는 32,000바이트입니다. (16000 * 2바이트)
        # 어르신의 말을 인식하기 위해 약 1.5초~2초 정도의 데이터가 모였을 때 STT를 시도합니다.
        # (이 값은 실시간성을 보며 1.0초 정도로 줄여도 됩니다.)
        MIN_PROCESS_SAMPLES = 16000 * 1.5
        MIN_PROCESS_BYTES = int(MIN_PROCESS_SAMPLES * 2) # 16-bit = 2 bytes

        async for request in request_iterator:
            payload_type = request.WhichOneof("payload")

            if payload_type == "config":
                user_id = request.config.user_id
                logger.info(f"🎙️ 세션 시작 (User ID: {user_id})")
                continue

            elif payload_type == "audio_content":
                if not user_id:
                    continue
                
                # 20~40ms 조각을 버퍼에 추가
                audio_buffer.extend(request.audio_content)

                # 설정한 버퍼 크기(예: 1.5초)를 넘었을 때 분석 시작
                if len(audio_buffer) >= MIN_PROCESS_BYTES:
                    # [포맷 변환] Signed 16-bit Little Endian -> Float32 Numpy
                    # Whisper 모델은 -1.0 ~ 1.0 사이의 float32 데이터를 입력으로 받습니다.
                    raw_data = np.frombuffer(audio_buffer, dtype=np.int16)
                    audio_np = raw_data.astype(np.float32) / 32768.0
                    
                    # STT 실행 (Faster-Whisper)
                    recognized_text = self.stt_service.transcribe_stream(audio_np)
                    
                    if recognized_text:
                        logger.info(f"👴 어르신 인식: {recognized_text}")
                        
                        # 인식 성공 시 버퍼를 비워 다음 문장을 준비합니다.
                        # (단, VAD가 '말이 끝났다'고 판단했을 때만 비우는 것이 더 정확하지만 
                        # 분석에 성공한 만큼만 버퍼에서 날리기 (슬라이딩 윈도우)
                        # 지금은 단순하게 전체를 비우지만, 실시간성을 위해 
                        # "인식된 부분까지만" 잘라내는 것이 가장 고수준의 구현입니다.
                        #  지금은 인식된 텍스트가 있을 때 비우는 것으로 우선 진행합니다.)
                        audio_buffer.clear()

                        # LLM & TTS 파이프라인 가동
                        async for audio_out in self.process_ai_response_chain(user_id, recognized_text):
                            yield voice_stream_pb2.VoiceResponse(audio_output=audio_out)
                    
                    # 만약 텍스트가 인식되지 않았다면(침묵 등), 
                    # 버퍼가 너무 무한정 커지지 않도록 오래된 데이터는 조금씩 밀어내야 합니다.
                    elif len(audio_buffer) > MIN_PROCESS_BYTES * 3:
                        # 3배수(약 4.5초) 이상 쌓여도 인식이 안 되면 앞부분 1초 삭제
                        del audio_buffer[:16000 * 2]
                    
    async def process_ai_response_chain(self, user_id, text):
        """STT 결과로부터 LLM 문장 생성 및 TTS 생성을 한 번에 처리"""
        latents = self.latent_manager.get_latent(user_id)
        if not latents:
            logger.error(f"❌ {user_id}의 Latent를 찾을 수 없습니다.")
            return

        # LLM 문장 단위 제너레이터 호출
        async for sentence in self.llm_service.ask_stream_sentences(text, instruction="당신은 다정한 손주입니다."):
            logger.info(f"🤖 자녀(LLM): {sentence}")
            
            # 4. TTS: 문장이 나오자마자 XTTS 스트리밍 실행
            # synthesize_stream은 오디오 청크(bytes)를 yield 한다고 가정
            async for chunk in self.tts_service.synthesize_stream(sentence, latents):
                yield chunk

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