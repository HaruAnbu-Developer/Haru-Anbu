# grpc_directory/tts_handler.py

import grpc
import voice_stream_pb2
import voice_stream_pb2_grpc
from services.tts.tts_service import get_tts_service
from services.voice_training_service.latent_manager import get_latent_manager

class VoiceConversationServicer(voice_stream_pb2_grpc.VoiceConversationServicer):
    def __init__(self):
        self.tts_service = get_tts_service()
        self.latent_manager = get_latent_manager() # 학습된 자식 목소리 tts 
        
    async def StreamConversation(self, request_iterator, context):
        user_id = context.invocation_metadata().get('user_id')
        
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
                        
    # 일단 작성한거 파이프라인 정리 필요함
    async def ChatStream(self, request_iterator, context):
        user_id = context.invocation_metadata().get('user_id')
        
        # 1. 연결 시점에 매니저 생성 (메모리 로드)
        manager = ConversationManager(user_id, db_session)
        
        async for audio_chunk in request_iterator:
            # 2. STT로 텍스트 변환
            user_text = await self.stt.recognize(audio_chunk)
            
            # 3. 매니저로부터 이번 턴 지침 획득 (매우 빠름, DB 조회 없음)
            instruction = manager.get_next_instruction()
            
            # 4. LLM 호출 (지침 포함)
            # LLM은 이제 고민하지 않고 지침대로 응답 생성
            async for char in self.llm.generate_stream(user_text, instruction):
                yield voice_pb2.AudioResponse(text=char)