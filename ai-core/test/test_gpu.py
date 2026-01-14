import time
from llama_cpp import Llama

model_path = "./models/llm/gemma-2-9b-it-Q5_K_M.gguf"

print("🔥 CUDA 가속 모드로 로딩 중...")
llm = Llama(
    model_path=model_path,
    n_gpu_layers=-1,  # 모든 레이어를 GPU로 전송
    n_ctx=1024,
    verbose=True      # CUDA 로드 로그 확인을 위해 필수로 True
)

prompt = "<start_of_turn>user\n안녕 하루야, 할머니 기분이 안 좋은데 재밌는 얘기 해줘.<end_of_turn>\n<start_of_turn>model\n"

print("\n--- 생성 속도 측정 시작 ---")
start_time = time.time()
first_token_time = 0

stream = llm(prompt, stream=True, max_tokens=100)

for i, chunk in enumerate(stream):
    if i == 0:
        first_token_time = time.time() - start_time
    print(chunk['choices'][0]['text'], end="", flush=True)

print(f"\n\n⏱️ [결과] 첫 토큰 반응 속도: {first_token_time:.4f}초")