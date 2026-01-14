from fastapi import FastAPI, UploadFile, File, Form, BackgroundTasks
import shutil

app = FastAPI()
voice_manager = VoiceManager(model_path="./models/tts/xtts_v2")

@app.post("/api/voice/register")
async def register_child_voice(
    background_tasks: BackgroundTasks,
    user_id: str = Form(...),
    file: UploadFile = File(...)
):
    # 1. 원본 파일 저장
    raw_dir = "./data/raw_voices"
    os.makedirs(raw_dir, exist_ok=True)
    raw_path = os.path.join(raw_dir, f"{user_id}_raw.wav")
    
    with open(raw_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # 2. 특징 추출 작업을 백그라운드에서 실행 (응답 속도 향상)
    # 사용자는 "업로드 성공" 메시지를 즉시 받고, 서버는 뒤에서 연산을 수행합니다.
    background_tasks.add_task(voice_manager.extract_and_cache_latent, user_id, raw_path)

    return {
        "message": "음성 파일이 성공적으로 업로드되었습니다. 목소리 분석을 시작합니다.",
        "user_id": user_id,
        "raw_path": raw_path
    }