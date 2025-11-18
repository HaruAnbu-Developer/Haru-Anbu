### 11/15 Loacl test  

.venv) namung2@jonam-ung-ui-MacBookAir ai-core % python3 [run.py](http://run.py/)

---  
🔄 Voice Conversation Pipeline Test

2025-11-15 20:13:14,927 - INFO - Initializing Voice Conversation Pipeline...  
2025-11-15 20:13:14,927 - INFO - Loading Whisper base model...  
2025-11-15 20:13:15,295 - INFO - Whisper model loaded in 0.37s  
2025-11-15 20:13:15,295 - INFO - Warming up STT model...  
2025-11-15 20:13:16,972 - INFO - STT warmup completed  
2025-11-15 20:13:16,972 - INFO - !!!STT Service initialized with model=base, device=cpu  
2025-11-15 20:13:16,985 - INFO - Loading EleutherAI/polyglot-ko-1.3b...  
/Users/namung2/haru/Haru-Anbu/.venv/lib/python3.11/site-packages/huggingface_hub/file_download.py:942: FutureWarning: `resume_download` is deprecated and will be removed in version 1.0.0. Downloads always resume when possible. If you want to force a new download, use `force_download=True`.  
warnings.warn(  
Loading checkpoint shards: 100%|████████████████████████████████████████████████████████████████████████████████████████████| 3/3 [00:00<00:00, 26.99it/s]  
/Users/namung2/haru/Haru-Anbu/.venv/lib/python3.11/site-packages/huggingface_hub/file_download.py:942: FutureWarning: `resume_download` is deprecated and will be removed in version 1.0.0. Downloads always resume when possible. If you want to force a new download, use `force_download=True`.  
warnings.warn(  
2025-11-15 20:13:20,171 - INFO - Model loaded in 3.19s  
2025-11-15 20:13:20,171 - INFO - Warming up...  
2025-11-15 20:13:27,996 - INFO - Generated in 7.825s: 어르신, 요즘 어떻게 지내고 계시나요? 어르신의 근황을 이야기해주실 수 있으세요? 어...  
2025-11-15 20:13:27,996 - INFO - Warmup done  
2025-11-15 20:13:27,996 - INFO - LLM Service initialized: EleutherAI/polyglot-ko-1.3b on mps  
2025-11-15 20:13:27,996 - INFO - Pipeline ready!  

---
📁 Test 1: 오디오 파일 처리  

2025-11-15 20:13:27,997 - INFO - Processing STT...  
huggingface/tokenizers: The current process just got forked, after parallelism has already been used. Disabling parallelism to avoid deadlocks...  
To disable this warning, you can either:  
- Avoid using `tokenizers` before the fork if possible  
- Explicitly set the environment variable TOKENIZERS_PARALLELISM=(true | false)  
100%|█████████████████████████████████████████████████████████████████████████████████████████████████████████████| 550/550 [00:00<00:00, 1493.97frames/s]  
2025-11-15 20:13:28,523 - WARNING - STT processing took 0.525s (target: 0.5s)  
2025-11-15 20:13:28,523 - INFO - STT output: 기도를 통해서 마음의 정리를 할 수 있지.  
2025-11-15 20:13:28,523 - INFO - Processing LLM...  
2025-11-15 20:13:33,828 - INFO - Generated in 5.305s: 좋으시겠어요~.  
2025-11-15 20:13:33,828 - INFO - LLM output: 좋으시겠어요~.  
2025-11-15 20:13:33,828 - WARNING - Pipeline slow: 5.831s (target: 1.3s)  
👴 어르신: 기도를 통해서 마음의 정리를 할 수 있지.  
🤖 AI: 좋으시겠어요~.  
성능:  
STT: 0.525s  
LLM: 5.305s  
Total: 5.831s  
---
📜 Conversation History  

1. 👴 어르신: 기도를 통해서 마음의 정리를 할 수 있지.  
2. 🤖 AI: 좋으시겠어요~.  

✅ All tests completed!  
---
---


### 11/18Loacl test  
.venv) namung2@jonam-ung-ui-MacBookAir ai-core % python3 run.py  


🔄  Voice Conversation Pipeline Test  

2025-11-18 18:18:55,167 - INFO - Initializing Voice Conversation Pipeline...  
2025-11-18 18:18:55,167 - INFO - Loading Whisper base model...  
2025-11-18 18:18:55,542 - INFO - Whisper model loaded in 0.37s  
2025-11-18 18:18:55,542 - INFO - Warming up STT model...  
2025-11-18 18:18:57,390 - INFO - STT warmup completed  
2025-11-18 18:18:57,390 - INFO - !!!STT Service initialized with model=base, device=cpu  
2025-11-18 18:18:57,422 - INFO - Loading EleutherAI/polyglot-ko-1.3b...  
/Users/namung2/haru/Haru-Anbu/.venv/lib/python3.11/site-packages/huggingface_hub/file_download.py:942: FutureWarning: `resume_download` is deprecated and will be removed in version 1.0.0. Downloads always resume when possible. If you want to force a new download, use `force_download=True`.  
  warnings.warn(  
Loading checkpoint shards: 100%|████████████████████████████████████████████████████████████████████████████████████████████| 3/3 [00:00<00:00, 12.70it/s]  
/Users/namung2/haru/Haru-Anbu/.venv/lib/python3.11/site-packages/huggingface_hub/file_download.py:942: FutureWarning: `resume_download` is deprecated and will be removed in version 1.0.0. Downloads always resume when possible. If you want to force a new download, use `force_download=True`.  
  warnings.warn(  
2025-11-18 18:19:00,588 - INFO - Model loaded in 3.17s  
2025-11-18 18:19:00,588 - INFO - Warming up...  
2025-11-18 18:19:09,403 - INFO - Generated in 8.815s: 녜, 안녕하십니까~!!(박수)(인사): 자, 이제 제 차례입니다.  
2025-11-18 18:19:09,403 - INFO - Warmup done  
2025-11-18 18:19:09,404 - INFO - LLM Service initialized: EleutherAI/polyglot-ko-1.3b on mps  
2025-11-18 18:19:09,404 - INFO - Loading TTS model: tts_models/multilingual/multi-dataset/xtts_v2...  
 \> tts_models/multilingual/multi-dataset/xtts_v2 is already downloaded.  
 \> Using model: xtts  
2025-11-18 18:19:25,773 - INFO - TTS model loaded in 16.37s  
2025-11-18 18:19:25,774 - INFO - Warming up TTS model...  
 \> Text splitted to sentences.  
['안녕하세요']  
 \> Processing time: 3.2020649909973145  
 \> Real-time factor: 0.7218789163615531  
2025-11-18 18:19:28,978 - INFO - TTS warmup completed  
2025-11-18 18:19:28,979 - INFO - TTS Service initialized: model=tts_models/multilingual/multi-dataset/xtts_v2, device=mps, speaker_wav=/Users/namung2/haru/Haru-Anbu/ai-  core/studio_origin/W-SO-1/sample1.wav  
2025-11-18 18:19:28,979 - INFO - Pipeline ready!  
  
📁 Test 1: 오디오 파일 입력  
2025-11-18 18:19:28,979 - INFO - Processing STT...  
huggingface/tokenizers: The current process just got forked, after parallelism has already been used. Disabling parallelism to avoid deadlocks...  
To disable this warning, you can either:  
        - Avoid using `tokenizers` before the fork if possible  
        - Explicitly set the environment variable TOKENIZERS_PARALLELISM=(true | false)  
100%|█████████████████████████████████████████████████████████████████████████████████████████████████████████████| 680/680 [00:00<00:00, 1828.94frames/s]  
2025-11-18 18:19:29,580 - WARNING - STT processing took 0.601s (target: 0.5s)  
2025-11-18 18:19:29,580 - INFO - STT output: 아이들을 신경 쓰는 마음은 똑같은데 몸이 안 떨어지니까요.   
2025-11-18 18:19:29,580 - INFO - Processing LLM...  
2025-11-18 18:19:33,455 - INFO - Generated in 3.875s: 네, 말씀 잘 들었습니다.  
2025-11-18 18:19:33,455 - INFO - LLM output: 네, 말씀 잘 들었습니다.  
2025-11-18 18:19:33,455 - INFO - Processing TTS...  
 \> Text splitted to sentences.  
['네, 말씀 잘 들었습니다.']  
 \> Processing time: 4.000985860824585  
 \> Real-time factor: 0.7474391540529864  
2025-11-18 18:19:37,462 - INFO - TTS saved to: /Users/namung2/haru/Haru-Anbu/ai-core/outputs/tts_response.wav  
2025-11-18 18:19:37,462 - WARNING - Pipeline slow: 8.484s (target: 1.8s)  

👴 어르신: 아이들을 신경 쓰는 마음은 똑같은데 몸이 안 떨어지니까요.  
🤖 AI: 네, 말씀 잘 들었습니다.  
🔊 생성된 음성 파일: /Users/namung2/haru/Haru-Anbu/ai-core/outputs/tts_response.wav  

⏱️ 성능:  
   STT: 0.601s  
   LLM: 3.875s  
   TTS: 4.007s  
   Total: 8.484s  

📜 대화 히스토리  
1. 👴 어르신: 아이들을 신경 쓰는 마음은 똑같은데 몸이 안 떨어지니까요.  
2. 🤖 AI: 네, 말씀 잘 들었습니다.  

✅ All tests completed!  
