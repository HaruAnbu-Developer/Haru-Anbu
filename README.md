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
