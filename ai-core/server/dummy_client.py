import asyncio
import grpc
import pyaudio
import voice_stream_pb2
import voice_stream_pb2_grpc

# 오디오 설정 (Whisper 권장 사양)
CHUNK = 1600  # 100ms 단위 (16000Hz 기준)
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 16000

async def audio_generator(user_id):
    # 1. 먼저 SessionConfig 전송
    yield voice_stream_pb2.VoiceRequest(
        config=voice_stream_pb2.SessionConfig(
            user_id=user_id,
            sample_rate=RATE,
            language_code="ko-KR"
        )
    )

    # 2. 마이크 스트림 열기
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK)
    
    print(f"🎤 [User:{user_id}] 대화를 시작하세요... (Ctrl+C 종료)")
    
    try:
        while True:
            # 마이크 데이터 읽기
            data = stream.read(CHUNK, exception_on_overflow=False)
            # 서버로 전송
            yield voice_stream_pb2.VoiceRequest(audio_content=data)
            await asyncio.sleep(0.01)
    except asyncio.CancelledError:
        stream.stop_stream()
        stream.close()
        p.terminate()

async def run_client():
    # ngrok 주소를 쓸 경우: "0.tcp.jp.ngrok.io:12345"
    # 로컬 테스트일 경우: "localhost:50051"
    target = "localhost:50051" 
    
    async with grpc.aio.insecure_channel(target) as channel:
        stub = voice_stream_pb2_grpc.VoiceConversationStub(channel)
        
        # 양방향 스트리밍 시작
        responses = stub.StreamConversation(audio_generator("test_user_001"))
        
        async for response in responses:
            payload_type = response.WhichOneof("payload")
            if payload_type == "transcript":
                print(f"👂 인식 중: {response.transcript}")
            elif payload_type == "ai_response":
                print(f"🤖 AI 답변: {response.ai_response}")
            elif payload_type == "audio_output":
                print(f"🔊 AI 음성 수신 중... ({len(response.audio_output)} bytes)")

if __name__ == "__main__":
    try:
        asyncio.run(run_client())
    except KeyboardInterrupt:
        pass