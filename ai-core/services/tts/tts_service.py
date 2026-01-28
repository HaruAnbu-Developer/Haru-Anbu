# /services/tts/tts_service.py
import torch
import os
import io
import logging
import numpy as np
import soundfile as sf
import warnings
import re
from typing import Optional, Dict, Any, Generator

# 1. 환경 설정 및 경고 억제
warnings.filterwarnings("ignore", category=UserWarning)

# FFmpeg 경로 설정
FFMPEG_BIN = r"C:\ffmpeg\bin"
os.environ["PATH"] = FFMPEG_BIN + os.pathsep + os.environ["PATH"]

from openvoice import se_extractor
from openvoice.api import ToneColorConverter
from melo.api import TTS

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class OpenVoiceTTSService:
    def __init__(self, device: str = "cuda"):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.language = "KR"
        
        # 모델 경로 설정
        ckpt_converter = "./checkpoints/converter"
        ckpt_base = "./checkpoints/base_speakers/ses/kr.pth"

        # 1. 베이스 모델 (MeloTTS) 로드
        self.base_model = TTS(language=self.language, device=self.device)
        self.speaker_ids = self.base_model.hps.data.spk2id
        
        # 2. 톤 컬러 컨버터 로드
        self.tone_color_converter = ToneColorConverter(f"{ckpt_converter}/config.json", device=self.device)
        self.tone_color_converter.load_ckpt(f"{ckpt_converter}/checkpoint.pth")
        
        # 3. 소스 SE (기준점) 로드
        self.source_se = torch.load(ckpt_base, map_location=self.device)
        
        print(f"🚀 [Memory-Only Mode] TTS 서비스가 {self.device}에서 준비되었습니다.")

    def convert_in_memory(self, src_bytes, src_se, tgt_se, tau=0.3):
        """
        파일 생성 없이 메모리(BytesIO) 상에서 
        공식 convert 함수를 실행하여 에러를 원천 차단합니다.
        """
        import io
        import soundfile as sf
        
        # 1. 원본 데이터를 메모리 파일 객체로 변환
        src_buffer = io.BytesIO(src_bytes)
        
        # 2. 결과물을 받을 메모리 버퍼 생성
        tgt_buffer = io.BytesIO()
        # sf.write는 파일 객체도 지원하므로, 실제 경로 대신 버퍼를 넘깁니다.
        # 하지만 OpenVoice API 내부에서 '파일 확장자'를 체크 이름 속성을 가짜로 부여
        src_buffer.name = "input.wav"
        tgt_buffer.name = "output.wav"

        with torch.no_grad():
            self.tone_color_converter.convert(
                audio_src_path=src_buffer, 
                src_se=src_se, 
                tgt_se=tgt_se, 
                output_path=tgt_buffer, 
                tau=tau
            )

        # 3. 변환된 오디오 데이터를 다시 numpy로 읽기
        tgt_buffer.seek(0)
        converted_audio, sr = sf.read(tgt_buffer)
        
        return converted_audio
    
    def extract_tone_color(self, audio_path: str):
        """
        VoiceProcessor가 전달한 로컬 WAV 파일에서 톤 컬러(SE)를 추출합니다.
        """
        try:
            # OpenVoice V2의 특징 추출기 사용
            # 결과값 target_se는 torch.Tensor 형태입니다.
            target_se, audio_name = se_extractor.get_se(
                audio_path, 
                self.tone_color_converter, 
                vad=True # 목소리 구간만 자동으로 탐지
            )
            return target_se
        except Exception as e:
            logger.error(f"❌ 특징 추출 중 오류 발생: {e}")
            raise e
        
    def synthesize(self, text: str, latents: Dict[str, Any], speed: float = 1.0) -> bytes: 
        """
        [RadioPipeline용] 
        텍스트 전체를 한 번에 변환하여 WAV 바이트(bytes)로 반환합니다.
        스트리밍이 아닌 파일 업로드용입니다.
        """
        text = self._preprocess_text(text)
        if not text: return b""

        target_se = latents.get("tone_color_embedding")
        
        try:
            # 1. MeloTTS로 베이스 오디오 생성 (numpy array)
            # 라디오는 조금 차분하게 0.9 배속 추천, 필요 시 speed 인자 조절
            audio_numpy = self.base_model.tts_to_file(text, self.speaker_ids['KR'], None, speed=speed)
            
            # 2. 메모리 버퍼에 WAV 포맷으로 쓰기 (OpenVoice 입력용)
            src_bio = io.BytesIO()
            sf.write(src_bio, audio_numpy, self.base_model.hps.data.sampling_rate, format='WAV')
            
            # 3. 톤 컬러 변환 (메모리 상에서 처리)
            # convert_in_memory는 numpy array를 반환함
            converted_audio_np = self.convert_in_memory(src_bio.getvalue(), self.source_se, target_se)

            # 4. 최종 결과물을 WAV bytes로 변환
            out_bio = io.BytesIO()
            # OpenVoice 출력은 보통 24000Hz (config 확인 필요하지만 보통 24k)
            sf.write(out_bio, converted_audio_np, 24000, format='WAV')
            
            return out_bio.getvalue()

        except Exception as e:
            logger.error(f"❌ [TTS] 합성 중 오류 발생: {e}")
            return b""

    async def synthesize_stream(self, text: str, latents: Dict[str, Any]) -> Generator[bytes, None, None]:
        text = self._preprocess_text(text)
        if not text: return
        
        target_se = latents.get("tone_color_embedding")

        try:
            # 1. MeloTTS로 베이스 오디오 생성
            audio_numpy = self.base_model.tts_to_file(text, self.speaker_ids['KR'], None, speed=1.0)
            
            # 2. 메모리 버퍼에 WAV 포맷으로 쓰기
            bio = io.BytesIO()
            sf.write(bio, audio_numpy, self.base_model.hps.data.sampling_rate, format='WAV')
            
            # 3. 메모리 변환 함수 호출
            converted_audio_np = self.convert_in_memory(bio.getvalue(), self.source_se, target_se)
            
            # 4. 스트리밍 전송
            sr = 24000 
            chunk_size = int(0.1 * sr)
            for i in range(0, len(converted_audio_np), chunk_size):
                chunk = converted_audio_np[i:i+chunk_size]
                audio_int16 = (chunk * 32767).astype(np.int16)
                yield audio_int16.tobytes()

        except Exception as e:
            logger.error(f"❌ 메모리 합성 중 오류 발생: {e}")

    def _preprocess_text(self, text: str) -> str:
        text = text.strip()
        text = re.sub(r'[^가-힣a-zA-Z0-9\s.\!\?]', '', text)
        return text

# 싱글톤 인스턴스
_instance = None
def get_tts_service():
    global _instance
    if _instance is None:
        _instance = OpenVoiceTTSService()
    return _instance

# if __name__ == "__main__":
#     import asyncio
#     import pyaudio 
    
#     async def test():
#         service = get_tts_service()
#         # 테스트용: 소스 SE를 타겟으로 사용하여 자기 자신 복제 테스트
#         test_latents = {"tone_color_embedding": service.source_se}
        
#         p = pyaudio.PyAudio()
#         stream = p.open(format=pyaudio.paInt16, channels=1, rate=24000, output=True)

#         print("🎙️ [In-Memory] 음성 합성 시작...")
#         async for chunk in service.synthesize_stream("목소리 복제전", test_latents):
#             if chunk:
#                 stream.write(chunk)
        
#         print("✅ 재생 완료!")
#         stream.stop_stream()
#         stream.close()
#         p.terminate()

#     asyncio.run(test())

# tts_service.py 맨 아래에 추가
if __name__ == "__main__":
    import asyncio
    import pyaudio
    import torch

    async def final_test():
        service = get_tts_service()
        
        # 현재 로컬에 있는 파일 이름과 일치해야 합니다.
        latent_path = "test_user_1_latent.pth" 
        
        if not os.path.exists(latent_path):
            print(f"❌ {latent_path} 파일을 찾을 수 없습니다.")
            return

        print(f"🔄 {latent_path} 로드 중...")
        # 저장된 딕셔너리에서 임베딩 텐서만 추출
        latent_dict = torch.load(latent_path, map_location=service.device)
        target_se = latent_dict["tone_color_embedding"]
        
        # OpenVoice V2 모델 형식에 맞게 latents 구성
        test_latents = {"tone_color_embedding": target_se}

        p = pyaudio.PyAudio()
        stream = p.open(format=pyaudio.paInt16, channels=1, rate=24000, output=True)

        print("🎙️ [복제 보이스] 재생 시작...")
        async for chunk in service.synthesize_stream("목소리 복제 후", test_latents):
            if chunk:
                stream.write(chunk)
        
        print("✅ 재생 완료!")
        stream.stop_stream()
        stream.close()
        p.terminate()

    asyncio.run(final_test())