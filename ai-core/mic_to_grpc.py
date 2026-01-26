import asyncio
import grpc
import pyaudio
import server.grpc.voice_stream_pb2 as voice_stream_pb2
import server.grpc.voice_stream_pb2_grpc as voice_stream_pb2_grpc

# 오디오 설정 (16kHz, Mono, 16-bit PCM)
RATE = 16000
CHUNK = int(RATE * 0.04) # 40ms 단위

async def audio_generator():
    # 1. 초기 설정 전송
    yield voice_stream_pb2.VoiceRequest(
        config=voice_stream_pb2.SessionConfig(user_id="test_user_1")
    )
    
    # 2. 마이크 열기
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paInt16, channels=1, rate=RATE, input=True, frames_per_buffer=CHUNK)
    print("🎤 [마이크 오픈] 말씀해 보세요... (Ctrl+C 종료)")

    try:
        while True:
            data = stream.read(CHUNK, exception_on_overflow=False)
            yield voice_stream_pb2.VoiceRequest(audio_content=data)
            await asyncio.sleep(0.01)
    except Exception as e:
        print(f"마이크 에러: {e}")
    finally:
        stream.stop_stream()
        stream.close()
        p.terminate()

async def main():
    async with grpc.aio.insecure_channel('localhost:50051') as channel:
        stub = voice_stream_pb2_grpc.VoiceConversationStub(channel)
        
        # --- 오디오 출력을 위한 PyAudio 설정 추가 ---
        p = pyaudio.PyAudio()
        # TTS 서비스가 24kHz 모노로 주니까 설정을 맞춰야 합니다.
        out_stream = p.open(format=pyaudio.paInt16, channels=1, rate=24000, output=True)
        # ------------------------------------------

        print("👂 AI의 답변을 기다리는 중...")
        responses = stub.StreamConversation(audio_generator())
        
        async for res in responses:
            if res.audio_output:
                # 수신된 바이트 데이터를 스피커 스트림으로 직접 쓰기 (재생)
                out_stream.write(res.audio_output)
                # print(f"🔊 AI 음성 수신 중: {len(res.audio_output)} bytes")
            if res.transcript:
                print(f"👴 어르신 자막: {res.transcript}")

        # 종료 처리
        out_stream.stop_stream()
        out_stream.close()
        p.terminate()

if __name__ == "__main__":
    asyncio.run(main())