import time
import logging
from typing import Optional, Dict, Any, List
from llama_cpp import Llama
import re # 상단에 추가


logger = logging.getLogger(__name__)

class LLMService:
    """Gemma-2-9B-IT (GGUF) 기반 LLM 서비스"""
    
    def __init__(self, model_path: str = "./gemma-2-9b-it-Q5_K_M.gguf"):
        self.model_path = model_path
        self.conversation_history: List[Dict[str, str]] = []
        
        # 최적화된 파라미터로 로드
        self.llm = Llama(
            model_path=self.model_path,
            n_gpu_layers=-1,  # 모든 레이어 GPU 사용
            n_ctx=1024,
            n_batch=512,
            verbose=False
        )
        
        
        logger.info(f"Gemma-2 LLM Service initialized from {model_path}")
        
    
    def generate_stream(
        self, 
        user_input: str,
        user_name: str = "어르신",
        user_id: str = "test_user", 
        add_to_history: bool = True):
        """문장 단위로 텍스트를 정제하여 스트리밍 (Chat 기능용)"""
        system_prompt = "당신은 다정한 손주입니다. 어르신의 말씀에 공감하며 한두 문장으로 따뜻하게 답하세요."
        
        # 오늘의 질문 상태(daily_question)가 있다면 맥락에 추가 (Radio 기능 연동 시 활용 가능)
        # if hasattr(self, 'daily_question') and self.daily_question:
        #    system_prompt += f" (오늘의 주제: {self.daily_question})"

        history_text = ""
        for msg in self.conversation_history[-2:]:
            role = "user" if msg["role"] == "user" else "model"
            history_text += f"<start_of_turn>{role}\n{msg['content']}<end_of_turn>\n"
        
        prompt = (
            f"<start_of_turn>user\n{system_prompt}\n{history_text}"
            f"어르신: {user_input}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )

        stream = self.llm(
            prompt,
            max_tokens=128,
            stop=["<end_of_turn>", "어르신:"],
            temperature=0.7,
            stream=True
        )
        
        sentence = ""
        full_response = ""
        for chunk in stream:
            token = chunk["choices"][0]["text"]
            sentence += token
            full_response += token
            
            if any(punc in token for punc in [".", "?", "!", "\n"]):
                clean_sentence = re.sub(r'[^가-힣a-zA-Z0-9\s.\?\!\,]', '', sentence).strip()
                if clean_sentence:
                    yield clean_sentence
                sentence = ""
                
        if add_to_history:
            self.conversation_history.append({"role": "user", "content": user_input})
            clean_full = re.sub(r'[^가-힣a-zA-Z0-9\s.\?\!\,]', '', full_response).strip()
            self.conversation_history.append({"role": "model", "content": clean_full})

    def generate_daily_question(self) -> str:
        """오늘의 라디오 질문 생성 (DB 없이 즉석 생성)"""
        prompt = (
            "<start_of_turn>user\n"
            "어르신들을 위한 따뜻하고 감성적인 '오늘의 질문'을 하나만 만들어줘.\n"
            "예시: '첫 월급을 타셨을 때 누구에게 선물을 하셨나요?', '가장 기억에 남는 여행지는 어디인가요?'\n"
            "답변은 질문 내용만 딱 한 문장으로 출력해.\n"
            "<end_of_turn>\n"
            "<start_of_turn>model\n"
        )
        output = self.llm(prompt, max_tokens=128, stop=["<end_of_turn>"], echo=False)
        question = output["choices"][0]["text"].strip()
        logger.info(f"Generated Question: {question}")
        return question

    def make_radio_script(self, user_name: str, answer_text: str) -> str:
        """사용자 답변을 라디오 대본으로 변환"""
        prompt = (
            "<start_of_turn>user\n"
            f"당신은 라디오 DJ '하루'입니다. 청취자 '{user_name}' 어르신이 보내주신 사연을 소개해주세요.\n"
            f"사연 내용: \"{answer_text}\"\n"
            "요구사항:\n"
            "1. 따뜻하고 활기찬 목소리로 사연을 읽어주듯이 작성하세요.\n"
            "2. 어르신의 사연에 공감하는 멘트를 덧붙이세요.\n"
            "3. 5분 내외 분량으로 작성하세요.\n"
            "4. 대본 내용만 바로 출력하세요.\n"
            "<end_of_turn>\n"
            "<start_of_turn>model\n"
        )
        output = self.llm(prompt, max_tokens=256, stop=["<end_of_turn>"], echo=False)
        script = output["choices"][0]["text"].strip()
        logger.info(f"Generated Script: {script}")
        return script

    def clear_history(self):
        self.conversation_history.clear()