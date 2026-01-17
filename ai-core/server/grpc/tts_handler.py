# grpc_directory/tts_handler.py

import grpc
import voice_stream_pb2
import voice_stream_pb2_grpc
from services.tts.tts_service import get_tts_service

class VoiceConversationServicer(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        self.tts_service = get_tts_service()
        self.latent_manager = self.tts_service.latent_manager

    async def StreamConversation(self, request_iterator, context):
        user_id = None
        
        async for request in request_iterator:
            # 1. 초기 설정(Config) 처리
            if request.HasField("config"):
                user_id = request.config.user_id
                print(f"🎙️ 대화 세션 시작: {user_id}")
                continue

            # 2. 음성 데이터(STT용) 처리 구간 (현재는 TTS 집중)
            if request.HasField("audio_content"):
                if not user_id:
                    continue
                
                # [여기에 STT 및 LLM 로직이 들어갈 자리]
                # 예시: 답변이 생성되었다고 가정하고 TTS 스트리밍 실행
                # 실제로는 LLM 결과가 나올 때 아래 로직을 트리거해야 합니다.
                
                latents = self.latent_manager.get_latent(user_id)
                if latents:
                    # XTTS 스트리밍 생성
                    audio_gen = self.tts_service.synthesize_stream("할머니 식사하셨어요?", latents)
                    
                    for chunk in audio_gen:
                        yield voice_stream_pb2.VoiceResponse(
                            audio_output=chunk.tobytes()
                        )