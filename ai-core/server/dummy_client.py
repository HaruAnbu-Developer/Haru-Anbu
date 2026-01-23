import asyncio
import grpc
import wave
import time
# proto 파일 경로에 맞춰 import (경로가 다르면 수정 필요)
import server.grpc.voice_stream_pb2 as voice_stream_pb2
import server.grpc.voice_stream_pb2_grpc as voice_stream_pb2_grpc

async def send_audio_file(stub, file_path):
    """WAV 파일을 읽어 gRPC 스트림으로 전송"""
    
    def request_generator():
        # 1. 첫 번째 메시지: 세션 설정(Config)
        yield voice_stream_pb2.VoiceRequest(
            config=voice_stream_pb2.SessionConfig(user_id="test_user_001")
        )

        # 2. 오디오 데이터 전송
        with wave.open(file_path, 'rb') as wf:
            # 16kHz, Mono, 16bit 인지 확인용 로그
            print(f"📊 파일 정보: 채널={wf.getnchannels()}, 샘플레이트={wf.getframerate()}, 샘플폭={wf.getsampwidth()}")
            
            # 40ms 분량의 프레임 계산 (16000 * 0.04 = 640 samples)
            chunk_size = 640 
            data = wf.readframes(chunk_size)
            
            while data:
                yield voice_stream_pb2.VoiceRequest(audio_content=data)
                # 실제 전송 속도와 유사하게 약간의 딜레이 (선택 사항)
                time.sleep(0.03) 
                data = wf.readframes(chunk_size)
    
    print(f"🚀 {file_path} 전송 시작...")
    
    # 서버 응답 대기 및 출력
    responses = stub.StreamConversation(request_generator())
    
    try:
        async for response in responses:
            # 서버에서 audio_output 필드에 데이터를 담아 보낼 때
            if response.audio_output:
                print(f"🔊 AI 음성 수신 중... (크기: {len(response.audio_output)} bytes)")
    except Exception as e:
        print(f"❌ 에러 발생: {e}")

async def main():
    # 서버 주소 (로컬)
    async with grpc.aio.insecure_channel('localhost:50051') as channel:
        stub = voice_stream_pb2_grpc.VoiceConversationStub(channel)
        
        # 테스트용 파일 경로 (실제 파일이 있어야 함)
        await send_audio_file(stub, "test_audio.wav")

if __name__ == "__main__":
    asyncio.run(main())