import asyncio
import logging
import grpc
import server.grpc.voice_stream_pb2 as voice_stream_pb2
import server.grpc.voice_stream_pb2_grpc as voice_stream_pb2_grpc

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class VoiceService(voice_stream_pb2_grpc.VoiceConversationServicer):
    async def StreamConversation(self, request_iterator, context):
        # 세션별 데이터 큐 생성
        audio_queue = asyncio.Queue()
        user_id = None

        # 1. Producer: Spring Boot에서 오는 데이터를 큐에 쌓음
        async def receive_requests():
            nonlocal user_id
            async for request in request_iterator:
                payload_type = request.WhichOneof("payload")
                
                if payload_type == "config":
                    user_id = request.config.user_id
                    logger.info(f"Session started for User: {user_id}")
                    # 여기서 DB에서 어르신 정보를 비동기로 로드할 수 있음
                    
                elif payload_type == "audio_content":
                    await audio_queue.put(request.audio_content)

        # 2. Consumer: 큐에서 데이터를 꺼내 STT -> LLM -> TTS 처리 (핵심 로직)
        async def process_and_respond():
            while True:
                # 오디오 조각 가져오기
                audio_chunk = await audio_queue.get()
                
                # --- 여기서부터 기존에 만든 AI 서비스 연결 ---
                # 예: stt_text = stt_service.transcribe_streaming(audio_chunk)
                
                # 테스트용 가짜 응답 스트리밍 (실제로는 TTS 조각을 보냄)
                # yield voice_stream_pb2.VoiceResponse(transcript="어르신 말씀 인식 중...")
                
                # LLM/TTS가 완료되면 조각 단위로 반환
                # yield voice_stream_pb2.VoiceResponse(audio_output=tts_chunk)
                
                await asyncio.sleep(0.01) # CPU 점유 방지

        # 두 작업을 병렬로 실행
        receive_task = asyncio.create_task(receive_requests())
        
        try:
            async for response in process_and_respond():
                yield response
        except Exception as e:
            logger.error(f"Error in session {user_id}: {e}")
        finally:
            receive_task.cancel()
            logger.info(f"Session ended for User: {user_id}")

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