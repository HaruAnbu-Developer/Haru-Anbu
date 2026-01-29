# /services/tts/tts_service.py
# /services/tts/tts_service.py
import torch
import os, sys
import io
import logging
import numpy as np
import soundfile as sf
import warnings
import re
from typing import Optional, Dict, Any, Generator
from contextlib import contextmanager

warnings.filterwarnings("ignore", category=UserWarning)

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
        
        ckpt_converter = "./checkpoints/converter"
        ckpt_base = "./checkpoints/base_speakers/ses/kr.pth"

        self.base_model = TTS(language=self.language, device=self.device)
        self.speaker_ids = self.base_model.hps.data.spk2id
        
        self.tone_color_converter = ToneColorConverter(
            f"{ckpt_converter}/config.json", device=self.device
        )
        self.tone_color_converter.load_ckpt(f"{ckpt_converter}/checkpoint.pth")
        
        self.source_se = torch.load(ckpt_base, map_location=self.device)
        logger.info(f"✅ TTS Service initialized on {self.device}")
    
    @contextmanager
    def _suppress_output(self):
        """라이브러리 내부 출력 억제"""
        new_target = open(os.devnull, "w")
        old_stdout, old_stderr = sys.stdout, sys.stderr
        try:
            sys.stdout, sys.stderr = new_target, new_target
            yield
        finally:
            sys.stdout, sys.stderr = old_stdout, old_stderr
            new_target.close()
    
    def convert_in_memory(self, src_bytes, src_se, tgt_se, tau=0.3):
        """★ [수정] 출력 억제 적용"""
        src_buffer = io.BytesIO(src_bytes)
        tgt_buffer = io.BytesIO()
        src_buffer.name = "input.wav"
        tgt_buffer.name = "output.wav"

        with torch.no_grad():
            # ★ OpenVoice 내부 tqdm/print 출력 차단
            with self._suppress_output():
                self.tone_color_converter.convert(
                    audio_src_path=src_buffer, 
                    src_se=src_se, 
                    tgt_se=tgt_se, 
                    output_path=tgt_buffer, 
                    tau=tau
                )

        tgt_buffer.seek(0)
        converted_audio, sr = sf.read(tgt_buffer)
        return converted_audio
    
    def extract_tone_color(self, audio_path: str):
        """특징 추출 (VoiceProcessor용)"""
        try:
            target_se, audio_name = se_extractor.get_se(
                audio_path, 
                self.tone_color_converter, 
                vad=True
            )
            return target_se
        except Exception as e:
            logger.error(f"❌ 특징 추출 실패: {e}")
            raise
        
    def synthesize(self, text: str, latents: Dict[str, Any], speed: float = 0.95, tau: float = 0.3) -> bytes:
        """라디오용 전체 합성 (tau 파라미터 추가)"""
        text = self._preprocess_text(text)
        if not text: 
            return b""

        target_se = latents.get("tone_color_embedding")
        
        try:
            # 1. 베이스 음성 생성
            with self._suppress_output():
                audio_numpy = self.base_model.tts_to_file(
                    text, self.speaker_ids['KR'], None, speed=speed
                )
            
            # 2. WAV 버퍼로 변환
            src_bio = io.BytesIO()
            sf.write(src_bio, audio_numpy, self.base_model.hps.data.sampling_rate, format='WAV')
            
            # 3. 톤 컬러 변환 (★ tau 파라미터 전달)
            converted_audio_np = self.convert_in_memory(
                src_bio.getvalue(), self.source_se, target_se, tau=tau
            )

            # 4. 최종 WAV bytes 반환
            out_bio = io.BytesIO()
            sf.write(out_bio, converted_audio_np, 24000, format='WAV')
            return out_bio.getvalue()

        except Exception as e:
            logger.error(f"❌ [TTS] 합성 실패: {e}")
            return b""

    async def synthesize_stream(
        self, 
        text: str, 
        latents: Dict[str, Any], 
        tau: float = 0.3
    ) -> Generator[bytes, None, None]:
        """★통화용 스트리밍 합성 (tau 파라미터 추가, 에러 핸들링 개선)"""
        text = self._preprocess_text(text)
        if not text: 
            return  # 빈 제너레이터
        
        target_se = latents.get("tone_color_embedding")
        if target_se is None:
            logger.error("❌ latents에 tone_color_embedding이 없습니다!")
            return

        try:
            # 1. 베이스 음성 생성
            with self._suppress_output():
                audio_numpy = self.base_model.tts_to_file(
                    text, self.speaker_ids['KR'], None, speed=1.0
                )
            
            # 2. WAV 버퍼로 변환
            bio = io.BytesIO()
            sf.write(bio, audio_numpy, self.base_model.hps.data.sampling_rate, format='WAV')
            
            # 3. 톤 컬러 변환 (★ tau 파라미터 전달)
            converted_audio_np = self.convert_in_memory(
                bio.getvalue(), self.source_se, target_se, tau=tau
            )
            
            # 4. 스트리밍 전송 (0.1초 단위)
            sr = 24000 
            chunk_size = int(0.1 * sr)
            
            for i in range(0, len(converted_audio_np), chunk_size):
                chunk = converted_audio_np[i:i+chunk_size]
                audio_int16 = (chunk * 32767).astype(np.int16)
                yield audio_int16.tobytes()

        except Exception as e:
            logger.error(f"❌ [TTS Stream] 합성 실패: {e}", exc_info=True)
            # ★ 에러 발생 시에도 빈 오디오라도 yield (클라이언트 대기 방지)
            yield b'\x00\x00' * 4800  # 0.1초 분량의 무음

    def _preprocess_text(self, text: str) -> str:
        """텍스트 전처리"""
        text = text.strip()
        # 특수문자 제거 (한글, 영어, 숫자, 기본 문장부호만 허용)
        text = re.sub(r'[^가-힣a-zA-Z0-9\s.\!\?,]', '', text)
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