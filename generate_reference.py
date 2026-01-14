from gtts import gTTS
import os

# "Ideal" content for voice cloning reference:
# - Clear pronunciation
# - Standard intonation
# - Sufficient length (~10-15s)
text = """
안녕하세요. 이것은 인공지능 라디오 방송을 위한 깨끗한 표준 목소리 샘플입니다. 
잡음 없이 맑고 또박또박한 발음으로, 어르신들에게 가장 편안하고 잘 들리는 목소리를 들려드리겠습니다. 
오늘 하루도 건강하고 행복하게 보내세요.
"""

output_path = "clean_sample.wav"

try:
    print("Generating clean reference sample...")
    tts = gTTS(text=text, lang='ko', slow=False)
    tts.save(output_path)
    print(f"SUCCESS: Saved ideal sample to {os.path.abspath(output_path)}")
except Exception as e:
    print(f"FAILED: {e}")
