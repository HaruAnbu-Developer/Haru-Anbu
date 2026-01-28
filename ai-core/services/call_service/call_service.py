# services/call_service/call_service.py
import grpc, asyncio
import logging
import numpy as np
import os
import sys
import boto3
from datetime import datetime
import json

# 서비스 모듈들
from services.stt.stt_service import get_stt_service
from services.llm.llm_service_Gemma_stream import get_llm_service
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager
from services.llm.conversation_manager import ConversationManager
from services.stt.vad_service import get_vad_service 

import server.grpc.voice_stream_pb2 as voice_stream_pb2
import server.grpc.voice_stream_pb2_grpc as voice_stream_pb2_grpc

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logging.getLogger("faster_whisper").setLevel(logging.WARNING)

# 가상환경 내 NVIDIA 라이브러리 경로 탐색 및 추가
venv_base = os.path.join(os.getcwd(), ".venvw", "Lib", "site-packages", "nvidia")
paths_to_add = [
    os.path.join(venv_base, "cublas", "bin"),
    os.path.join(venv_base, "cudnn", "bin")
]
for p in paths_to_add:
    if os.path.exists(p):
        os.environ["PATH"] += os.pathsep + p
if sys.platform == 'win32':
    for p in paths_to_add:
        if os.path.exists(p):
            os.add_dll_directory(p)

class VoiceService(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        self.stt_service = get_stt_service()
        self.llm_service = get_llm_service()
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager()
        self.vad_service = get_vad_service()
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.getenv("S3_ACCESS_KEY_ID"),
            aws_secret_access_key=os.getenv("S3_SECRET_ACCESS_KEY"),
            region_name=os.getenv("S3_REGION", "ap-northeast-2")
        )
        self.bucket_name = os.getenv("S3_BUCKET_NAME", "haru-anbu-voice-storage")
        

    async def StreamConversation(self, request_iterator, context):
        user_id = None
        manager = None 
        session_timestamp = int(datetime.now().timestamp())
        
        speech_buffer = bytearray()       
        vad_chunk_buffer = bytearray()    
        is_user_speaking = False          
        
        # VAD 입력 크기: 512 샘플 * 2 bytes = 1024 bytes
        VAD_WINDOW_SIZE = 1024 

        try:
            async for request in request_iterator:
                payload_type = request.WhichOneof("payload")

                if payload_type == "config":
                    user_id = request.config.user_id
                    
                    if manager is None:
                        manager = ConversationManager(user_id)
                        logger.info(f"✅ 매니저 생성 완료: {user_id} (DB 미션 로드됨)")
                    
                    self.vad_service.reset_states() 
                    self.latent_manager.prepare_user(user_id, f"latents/{user_id}/{user_id}_latent.pth")
                    logger.info(f"🎙️ VAD 세션 시작 (User ID: {user_id})")
                    continue
                
                elif payload_type == "audio_content":
                    if not user_id or manager is None:
                        continue
                    
                    chunk = request.audio_content
                    vad_chunk_buffer.extend(chunk)

                    while len(vad_chunk_buffer) >= VAD_WINDOW_SIZE:
                        window = vad_chunk_buffer[:VAD_WINDOW_SIZE]
                        del vad_chunk_buffer[:VAD_WINDOW_SIZE]
                        
                        window_np = np.frombuffer(window, dtype=np.int16).astype(np.float32) / 32768.0
                        
                        state = self.vad_service.is_speech(window_np)
                        
                        if state == "SPEECH_START":
                            logger.info("🗣️ 말 시작 감지...")
                            is_user_speaking = True
                            speech_buffer.extend(window) 
                            
                        elif state == "SPEAKING":
                            speech_buffer.extend(window)
                            
                        elif state == "SILENCE":
                            if is_user_speaking:
                                speech_buffer.extend(window)
                        
                        elif state == "SPEECH_END":
                            logger.info("🤐 말 끝 감지 (Turn-Taking)")
                            is_user_speaking = False
                            
                            # 말이 끝났으므로 STT 처리 시작
                            if len(speech_buffer) > 16000 * 0.5 * 2: 
                                # ★ [수정] await 단독 호출 -> async for 로 변경
                                async for response in self.process_speech_chunk(user_id, speech_buffer, manager):
                                    yield response
                            else:
                                logger.info("⚠️ 너무 짧은 소리(잡음) 무시됨")
                            
                            speech_buffer.clear() 

        except Exception as e:
            logger.error(f"🔥 통화 중 에러 발생: {e}")
            
        finally:
            if user_id:
                self.latent_manager.release_user(user_id)
                logger.info(f"🗑️ {user_id} 메모리 해제")
                logger.info(f"대화 로그 S3에 업로드 log/")
                if manager and manager.conversation_log:
                    await self.upload_log_to_s3(user_id, session_timestamp, manager)
            logger.info("👋 세션 종료")

    async def process_speech_chunk(self, user_id, audio_bytes, manager):
        """VAD가 확정한 발화 구간을 STT -> LLM 처리"""
        raw_data = np.frombuffer(audio_bytes, dtype=np.int16)
        audio_np = raw_data.astype(np.float32) / 32768.0
        
        # STT 실행
        recognized_text, _ = self.stt_service.transcribe_stream(audio_np)
        
        if recognized_text:
            # 매니저 기록
            manager.record_user_input(recognized_text)
            
            # 응답 생성 파이프라인 가동
            async for audio_out in self.process_ai_response_chain(user_id, recognized_text, manager):
                yield voice_stream_pb2.VoiceResponse(audio_output=audio_out)

    async def process_ai_response_chain(self, user_id, text, manager):
        latents = self.latent_manager.get_latent(user_id)
        if not latents: 
            logger.error("Latent 없음")
            return

        instruction = manager.get_system_instruction()
        logger.info(f"📝 LLM 지시사항: {instruction}")

        full_sentence = ""
        async for sentence in self.llm_service.ask_stream_sentences(text, instruction=instruction):
            full_sentence += sentence
            async for audio_bytes in self.tts_service.synthesize_stream(sentence, latents):
                yield audio_bytes
        
        manager.record_ai_response(full_sentence)
        
    async def upload_log_to_s3(self, user_id, timestamp, manager):
        """대화 로그와 미션 정보를 JSON으로 S3에 저장"""
        try:
            log_data = {
                "user_id": user_id,
                "timestamp": timestamp,
                "conversation_log": manager.conversation_log, # ["User:...", "AI:..."]
                "missions": manager.checklist # 미션 정보 포함
            }
            
            # JSON 문자열 변환
            json_body = json.dumps(log_data, ensure_ascii=False, indent=2)
            
            # S3 경로: logs/test_user_1_170000.json
            file_key = f"logs/{user_id}_{timestamp}_log.json"
            
            # 동기 함수인 put_object를 비동기로 실행 (I/O 블로킹 방지)
            loop = asyncio.get_event_loop()
            await loop.run_in_executor(None, lambda: self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=file_key,
                Body=json_body,
                ContentType="application/json"
            ))
            logger.info(f"📤 대화 로그 S3 업로드 완료: {file_key}")
            
        except Exception as e:
            logger.error(f"❌ 로그 업로드 실패: {e}")

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