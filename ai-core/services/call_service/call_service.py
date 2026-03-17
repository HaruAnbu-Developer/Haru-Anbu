import grpc
import asyncio
import logging
import numpy as np
import os
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
        VAD_WINDOW_SIZE = 1024 

        try:
            async for request in request_iterator:
                payload_type = request.WhichOneof("payload")

                if payload_type == "config":
                    user_id = request.config.user_id
                    
                    if manager is None:
                        manager = ConversationManager(user_id)
                        logger.info(f"✅ 매니저 생성 완료: {user_id}")
                        
                        # VAD 및 Latent 초기화
                        self.vad_service.reset_states() 
                        self.latent_manager.prepare_user(user_id, f"latents/{user_id}/{user_id}_latent.pth")
                        logger.info(f"🎙️ VAD 세션 시작")

                        # ★ [추상화] 오프닝 멘트 처리 (공통 로직 사용)
                        async for response in self._process_opening(user_id, manager):
                            yield response
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
                            if is_user_speaking: speech_buffer.extend(window)
                        elif state == "SPEECH_END":
                            logger.info("🤐 말 끝 감지")
                            is_user_speaking = False
                            
                            if len(speech_buffer) > 16000 * 0.5 * 2: 
                                # ★ [추상화] 음성 처리 및 답변 생성
                                async for response in self._handle_speech_input(user_id, speech_buffer, manager):
                                    yield response
                            else:
                                logger.info("⚠️ 잡음 무시")
                            speech_buffer.clear() 

        except Exception as e:
            logger.error(f"🔥 통화 중 에러: {e}")
            
        finally:
            if user_id:
                self.latent_manager.release_user(user_id)
                logger.info(f"🗑️ {user_id} 메모리 해제")
                if manager: # 대화가 없었어도 로그 저장
                    await self.upload_log_to_s3(user_id, session_timestamp, manager)
            logger.info("👋 세션 종료")

    # =========================================================================
    #  PRIVATE HELPER METHODS 
    # =========================================================================

    async def _process_opening(self, user_id, manager):
        """통화 시작 시 첫 인사 처리"""
        text = manager.get_opening_remark()
        logger.info(f"👋 오프닝: {text}")
        manager.record_ai_response(text)
        
        # 단일 텍스트를 비동기 제너레이터로 변환하여 공통 로직에 전달
        async def text_gen(): yield text
        
        async for resp in self._synthesize_response_stream(user_id, text_gen()):
            yield resp

    async def _handle_speech_input(self, user_id, speech_buffer, manager):
        """STT -> LLM -> TTS 파이프라인 처리"""
        raw_data = np.frombuffer(speech_buffer, dtype=np.int16)
        audio_np = raw_data.astype(np.float32) / 32768.0
        
        recognized_text, _ = self.stt_service.transcribe_stream(audio_np)
        
        if recognized_text:
            clean_text = recognized_text.strip()
            manager.record_user_input(clean_text)
            
            # LLM 대화 처리 호출
            async for resp in self._process_llm_conversation(user_id, clean_text, manager):
                yield resp

    async def _process_llm_conversation(self, user_id, user_text, manager):
        instruction = manager.get_system_instruction()
        logger.info(f"📝 지시사항: {instruction}")
        
        full_text_accumulator = []
        is_mission_cleared_in_this_turn = False
        
        # 스트림 처리를 위한 내부 제너레이터
        async def llm_gen_with_tag_check():
            nonlocal is_mission_cleared_in_this_turn
            buffer = ""
            
            # LLM 스트림 시작
            async for token in self.llm_service.ask_stream_sentences(user_text, instruction=instruction):
                buffer += token
                
                # ★ 태그 검사 로직
                # 문장 초반에 [[OK]]가 있는지 확인
                if "[1]" in buffer:
                    if not is_mission_cleared_in_this_turn:
                        logger.info("🎯 LLM 판단: 미션 성공! ([1] 감지)")
                        # 매니저 상태 업데이트 (현재 단계 완료 처리)
                        if manager.current_step < len(manager.checklist):
                            manager.checklist[manager.current_step]['user_answer'] = user_text
                            manager.checklist[manager.current_step]['answered'] = True
                            is_mission_cleared_in_this_turn = True
                    
                    # 태그 제거 후 텍스트만 전송
                    cleaned_token = buffer.replace("[1]", "")
                    buffer = "" # 버퍼 비움
                    if cleaned_token.strip():
                        full_text_accumulator.append(cleaned_token)
                        yield cleaned_token
                else:
                    if len(buffer) > 10:
                        full_text_accumulator.append(buffer)
                        yield buffer
                        buffer = ""
            
            # 스트림이 끝났는데 버퍼에 남은 게 있다면 털기
            if buffer:
                final_text= buffer.replace("[1]", "") # 혹시 끝에 잘렸을 경우 대비
                full_text_accumulator.append(final_text)
                yield final_text

        # 공통 합성 로직에 연결
        async for resp in self._synthesize_response_stream(user_id, llm_gen_with_tag_check()):
            yield resp
            
        # 완료 후 기록
        manager.record_ai_response("".join(full_text_accumulator))

    async def _synthesize_response_stream(self, user_id, text_iterator):
        """★ [핵심] 텍스트 스트림 -> TTS 합성 -> 버퍼링 -> 전송 (공통 로직)"""
        latents = self.latent_manager.get_latent(user_id)
        if not latents: return

        TTS_BUFFER_SIZE = 24000 # 24KB (약 0.5초) - 끊김 방지 최적값
        audio_chunk_buffer = bytearray()

        async for text_chunk in text_iterator:
            async for audio_bytes in self.tts_service.synthesize_stream(text_chunk, latents):
                audio_chunk_buffer.extend(audio_bytes)

                if len(audio_chunk_buffer) >= TTS_BUFFER_SIZE:
                    yield voice_stream_pb2.VoiceResponse(audio_output=bytes(audio_chunk_buffer))
                    audio_chunk_buffer.clear()
        
        # 남은 버퍼 전송
        if len(audio_chunk_buffer) > 0:
            yield voice_stream_pb2.VoiceResponse(audio_output=bytes(audio_chunk_buffer))

    async def upload_log_to_s3(self, user_id, timestamp, manager):
        """S3 로그 업로드"""
        try:
            log_data = {
                "user_id": user_id,
                "timestamp": timestamp,
                "conversation_log": manager.conversation_log,
                "missions": manager.checklist
            }
            json_body = json.dumps(log_data, ensure_ascii=False, indent=2)
            file_key = f"logs/{user_id}_{timestamp}_log.json"
            
            loop = asyncio.get_event_loop()
            await loop.run_in_executor(None, lambda: self.s3_client.put_object(
                Bucket=self.bucket_name, Key=file_key, Body=json_body, ContentType="application/json"
            ))
            logger.info(f"📤 로그 업로드: {file_key}")
        except Exception as e:
            logger.error(f"❌ 로그 업로드 실패: {e}")

async def serve():
    server = grpc.aio.server()
    voice_stream_pb2_grpc.add_VoiceConversationServicer_to_server(VoiceService(), server)
    server.add_insecure_port("[::]:50051")
    logger.info("Starting gRPC server on [::]:50051")
    await server.start()
    await server.wait_for_termination()

if __name__ == "__main__":
    asyncio.run(serve())