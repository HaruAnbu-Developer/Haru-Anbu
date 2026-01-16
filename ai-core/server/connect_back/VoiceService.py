import asyncio
import logging
import grpc
import sys
import os
import io
import soundfile as sf
import datetime

# Add path to finding generated protos and services
sys.path.append(os.getcwd())
sys.path.append(os.path.join(os.getcwd(), 'server', 'grpc'))

from server.grpc import voice_stream_pb2
from server.grpc import voice_stream_pb2_grpc
from services.LLM.llm_service_Gemma_stream import LLMService
from services.TTS.tts_service import TTSService

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class VoiceService(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        logger.info("Initializing AI Services in VoiceService...")
        self.llm = LLMService()
        self.tts = TTSService()
        self.daily_question_cache = None
        self.last_cache_date = None
        logger.info("AI Services Ready!")

    async def GetDailyQuestion(self, request, context):
        today = datetime.date.today()
        logger.info(f"Received GetDailyQuestion request for {today}")
        
        if self.daily_question_cache is None or self.last_cache_date != today:
            loop = asyncio.get_running_loop()
            # Run LLM in executor to avoid blocking async loop
            q = await loop.run_in_executor(None, self.llm.generate_daily_question)
            self.daily_question_cache = q
            self.last_cache_date = today
            
        return voice_stream_pb2.QuestionResponse(
            question=self.daily_question_cache,
            topic_id=str(today)
        )

    async def GenerateRadioContent(self, request, context):
        logger.info(f"Received GenerateRadioContent request from {request.user_name}")
        loop = asyncio.get_running_loop()
        
        # 1. Generate Script (LLM)
        script = await loop.run_in_executor(
            None, 
            self.llm.make_radio_script, 
            request.user_name, 
            request.answer_text
        )
        
        # 2. TTS Synthesis
        result = await loop.run_in_executor(
            None,
            lambda: self.tts.synthesize(script, save_path=None)
        )
        audio_np = result["audio"]
        sr = result["sample_rate"]
        
        # 3. Convert to WAV bytes
        buffer = io.BytesIO()
        sf.write(buffer, audio_np, sr, format='WAV')
        wav_bytes = buffer.getvalue()
        
        return voice_stream_pb2.RadioResponse(
            audio_data=wav_bytes,
            script=script
        )

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
                
                # --- 기존 코드는 여기까지만 구현되어 있었음 ---
                # 실제 연결 시 self.stt, self.llm, self.tts 사용 필요
                # 현재는 Radio 구현에 집중하기 위해 패스
                
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