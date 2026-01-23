import torch
import os
import uuid
import logging
import numpy as np
import soundfile as sf
from typing import Optional, Dict, Any, Generator
from openvoice import se_extractor
from openvoice.api import ToneColorConverter
from melo.api import TTS

logger = logging.getLogger(__name__)

class OpenVoiceTTSService:
    def __init__(self, device: str = "cuda"):
        self.device = device if torch.cuda.is_available() else "cpu"
        self.language = "KR"
        
        # 1. 모델 경로 설정 (경로가 실제 파일 위치와 맞는지 확인 필요)
        ckpt_converter = "./checkpoints/converter"
        ckpt_base = "./checkpoints/base_speakers/ses/kr.pth"

        # 2. 베이스 한국어 모델 로드 (MeloTTS)
        self.base_model = TTS(language=self.language, device=self.device)
        self.speaker_ids = self.base_model.hps.data.spk2id
        
        # 3. 톤 컬러 컨버터 로드
        self.tone_color_converter = ToneColorConverter(f"{ckpt_converter}/config.json", device=self.device)
        self.tone_color_converter.load_ckpt(f"{ckpt_converter}/checkpoint.pth")
        
        # 4. 소스 SE (기준점) 로드
        self.source_se = torch.load(ckpt_base, map_location=self.device)


        
        logger.info(f"🚀 OpenVoice V2 (MeloTTS) initialized on {self.device}")

    def extract_tone_color(self, audio_path: str):
        """손주 목소리 샘플에서 특징(SE) 추출"""
        target_se, _ = se_extractor.get_se(audio_path, self.tone_color_converter, vad=True)
        return target_se

    async def synthesize_stream(self, text: str, latents: Dict[str, Any]) -> Generator[bytes, None, None]:
        text = self._preprocess_text(text)
        if not text: return
        
        target_se = latents.get("tone_color_embedding") # XTTS와 인터페이스 호환

        try:
            # 임시 파일 경로
            temp_id = uuid.uuid4()
            src_path = f"temp_src_{temp_id}.wav"
            out_path = f"temp_out_{temp_id}.wav"

            # [Step 1] 베이스 음성 생성 (MeloTTS)
            self.base_model.tts_to_file(text, self.speaker_ids['KR'], src_path, speed=1.0)
            
            # [Step 2] 목소리 톤 변환
            self.tone_color_converter.convert(
                model=self.base_model,
                src_se=self.source_se,
                tgt_se=target_se,
                src_path=src_path,
                save_path=out_path
            )
            
            # [Step 3] 파일 읽기 및 청크 단위 스트리밍
            audio_data, sr = sf.read(out_path)
            chunk_size = int(0.1 * sr) # 100ms 단위로 끊어서 전송
            
            for i in range(0, len(audio_data), chunk_size):
                chunk = audio_data[i:i+chunk_size]
                audio_int16 = (chunk * 32767).astype(np.int16)
                yield audio_int16.tobytes()

            # 임시 파일 삭제
            if os.path.exists(src_path): os.remove(src_path)
            if os.path.exists(out_path): os.remove(out_path)

        except Exception as e:
            logger.error(f"❌ OpenVoice Synthesis Error: {e}")

    def _preprocess_text(self, text: str) -> str:
        # 기존에 만든 정제 로직 활용
        import re
        text = text.strip()
        text = re.sub(r'[^가-힣a-zA-Z0-9\s.\!\?]', '', text)
        return text

# 싱글톤 패턴
_instance = None
def get_tts_service():
    global _instance
    if _instance is None:
        _instance = OpenVoiceTTSService()
    return _instance