// 📁 src/pages/Chat.js
import { useState, useRef, useEffect } from "react";

function Chat() {
  const [messages, setMessages] = useState([
    {
      id: 1,
      text: "안녕하세요! 하루안부 AI입니다. 오늘 기분이 어떠세요?",
      sender: "bot",
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
      date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
    }
  ]);
  const [inputText, setInputText] = useState("");
  const [isRecording, setIsRecording] = useState(false);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);

  // 자동 스크롤
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 메시지 전송
  const handleSend = () => {
    if (inputText.trim() === "") return;

    // 사용자 메시지 추가
    const userMessage = {
      id: messages.length + 1,
      text: inputText,
      sender: "user",
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
      date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
    };

    setMessages([...messages, userMessage]);
    setInputText("");

    // 봇 응답 (1초 후)
    setTimeout(() => {
      const botMessage = {
        id: messages.length + 2,
        text: "말씀 잘 들었어요. 더 자세히 이야기해주시겠어요?",
        sender: "bot",
        time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
        date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
      };
      setMessages(prev => [...prev, botMessage]);
    }, 1000);
  };

  // 엔터키로 전송
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 음성 녹음 시작/중지
  const handleVoiceRecord = () => {
    if (!isRecording) {
      // 녹음 시작
      setIsRecording(true);
      console.log("음성 녹음 시작");
      
      // 실제 음성 인식 API 연동 시 여기에 추가
      // 예시: Web Speech API 사용
      
      // 테스트용: 3초 후 자동 중지
      setTimeout(() => {
        setIsRecording(false);
        setInputText("음성으로 입력된 텍스트");
      }, 3000);
    } else {
      // 녹음 중지
      setIsRecording(false);
      console.log("음성 녹음 중지");
    }
  };

  // 파일 선택 버튼 클릭
  const handleFileClick = () => {
    fileInputRef.current?.click();
  };

  // 파일 선택 시
  const handleFileChange = (e) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      const file = files[0];
      
      // 파일 메시지 추가
      const fileMessage = {
        id: messages.length + 1,
        text: `📎 ${file.name} (${(file.size / 1024).toFixed(1)}KB)`,
        sender: "user",
        time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
        date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }),
        isFile: true
      };

      setMessages([...messages, fileMessage]);

      // 봇 응답
      setTimeout(() => {
        const botMessage = {
          id: messages.length + 2,
          text: "파일을 잘 받았어요! 확인해볼게요.",
          sender: "bot",
          time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
          date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
        };
        setMessages(prev => [...prev, botMessage]);
      }, 1000);

      // 파일 input 초기화
      e.target.value = '';
    }
  };

  return (
    <div className="d-flex flex-column bg-white" style={{ height: 'calc(100vh - 64px)', marginBottom: '64px' }}>
      {/* 헤더 */}
      <div className="p-3 border-bottom bg-light" style={{ minHeight: 'auto' }}>
        <div className="d-flex align-items-center">
          <div className="me-3">
            <div style={{
              width: '45px',
              height: '45px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #6C63FF 0%, #00B894 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <i className="fas fa-robot text-white"></i>
            </div>
          </div>
          <div>
            <h6 className="mb-0 fw-bold">하루안부 AI</h6>
            <small className="text-success">
              <i className="fas fa-circle" style={{ fontSize: '8px' }}></i> 온라인
            </small>
          </div>
        </div>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-grow-1 overflow-auto p-3 pt-2" style={{ backgroundColor: '#f8f9fa' }}>
        {messages.map((message, index) => {
          // 날짜 구분선 표시 (이전 메시지와 날짜가 다르면)
          const showDateDivider = index === 0 || messages[index - 1].date !== message.date;
          
          return (
            <div key={message.id}>
              {showDateDivider && (
                <div className="text-center my-2">
                  <span 
                    className="badge bg-light text-dark" 
                    style={{ 
                      fontSize: '11px', 
                      fontWeight: 'normal',
                      padding: '5px 12px'
                    }}
                  >
                    {message.date}
                  </span>
                </div>
              )}
              
              <div
                className={`d-flex mb-3 ${message.sender === 'user' ? 'justify-content-end' : 'justify-content-start'}`}
              >
            {message.sender === 'bot' && (
              <div className="me-2">
                <div style={{
                  width: '35px',
                  height: '35px',
                  borderRadius: '50%',
                  background: '#6C63FF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '14px'
                }}>
                  <i className="fas fa-robot text-white"></i>
                </div>
              </div>
            )}
            
            <div style={{ maxWidth: '65%' }}>
              <div
                className={`p-2 px-3 rounded-3 ${
                  message.sender === 'user'
                    ? 'text-white'
                    : 'bg-white border'
                }`}
                style={{
                  borderRadius: message.sender === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
                  background: message.sender === 'user' ? '#6C63FF' : 'white'
                }}
              >
                <p className="mb-0" style={{ fontSize: '15px' }}>{message.text}</p>
              </div>
              <small className={`d-block mt-1 ${message.sender === 'user' ? 'text-end' : ''}`} style={{ fontSize: '11px', color: '#888' }}>
                {message.time}
              </small>
            </div>

            {message.sender === 'user' && (
              <div className="ms-2">
                <div style={{
                  width: '35px',
                  height: '35px',
                  borderRadius: '50%',
                  background: '#6c757d',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '14px'
                }}>
                  <i className="fas fa-user text-white"></i>
                </div>
              </div>
            )}
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* 입력 영역 */}
      <div className="p-3 border-top bg-white position-relative">
        {/* 🎤 녹음 중 오버레이 */}
        {isRecording && (
          <div 
            className="position-absolute start-0 end-0 d-flex align-items-center justify-content-center"
            style={{
              background: 'rgba(13, 216, 91, 0.95)',
              zIndex: 100,
              animation: 'fadeIn 0.3s ease-in',
              bottom: 0,
              top: '-500px',
              paddingBottom: '80px'
            }}
          >
            <div className="text-center">
              {/* 웨이브 애니메이션 */}
              <div className="mb-3" style={{ position: 'relative' }}>
                <div style={{
                  width: '80px',
                  height: '80px',
                  borderRadius: '50%',
                  background: 'white',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto',
                  position: 'relative',
                  zIndex: 3
                }}>
                  <i className="fas fa-microphone fa-2x" style={{ color: '#6C63FF' }}></i>
                </div>
                {/* 펄스 웨이브 */}
                <div style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  width: '80px',
                  height: '80px',
                  borderRadius: '50%',
                  background: 'rgba(255, 255, 255, 0.3)',
                  animation: 'pulse 1.5s ease-out infinite',
                  zIndex: 1
                }}></div>
                <div style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  width: '80px',
                  height: '80px',
                  borderRadius: '50%',
                  background: 'rgba(255, 255, 255, 0.3)',
                  animation: 'pulse 1.5s ease-out infinite 0.5s',
                  zIndex: 2
                }}></div>
              </div>
              
              <h5 className="text-white mb-2 fw-bold">듣고 있어요...</h5>
              <p className="text-white mb-3" style={{ fontSize: '14px', opacity: 0.9 }}>
                편하게 말씀해주세요
              </p>
              
              <button 
                className="btn btn-light rounded-pill px-4"
                onClick={handleVoiceRecord}
              >
                <i className="fas fa-stop me-2"></i>
                녹음 중지
              </button>
            </div>
          </div>
        )}

        <div className="d-flex align-items-center">
          {/* 숨겨진 파일 입력 */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*,.pdf,.doc,.docx,.txt"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
          
          <button 
            className="btn btn-light me-2"
            onClick={handleFileClick}
            title="파일 첨부"
          >
            <i className="fas fa-plus"></i>
          </button>
          <div className="flex-grow-1 position-relative">
            <input
              type="text"
              className="form-control rounded-pill ps-3 pe-5"
              placeholder={isRecording ? "듣고 있어요..." : "메시지를 입력하세요..."}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={isRecording}
              style={{ 
                border: '1px solid #dee2e6',
                fontSize: '16px'
              }}
            />
            <button 
              className={`btn btn-link position-absolute end-0 top-50 translate-middle-y ${isRecording ? 'text-danger' : 'text-muted'}`}
              style={{ textDecoration: 'none', zIndex: 10 }}
              onClick={handleVoiceRecord}
              type="button"
            >
              <i className={`fas ${isRecording ? 'fa-stop-circle' : 'fa-microphone'} fa-lg`}></i>
            </button>
          </div>
          <button 
  className="btn rounded-circle ms-2"
  onClick={handleSend}
  disabled={inputText.trim() === ""}
  style={{ 
    width: '45px', 
    height: '45px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#6C63FF',
    border: 'none',
    color: 'white'
  }}
>
  <i className="fas fa-paper-plane"></i>
</button>
        </div>
        
        {/* CSS 애니메이션 */}
        <style>{`
          @keyframes pulse {
            0% {
              transform: translate(-50%, -50%) scale(1);
              opacity: 0.8;
            }
            100% {
              transform: translate(-50%, -50%) scale(2);
              opacity: 0;
            }
          }
          
          @keyframes fadeIn {
            from {
              opacity: 0;
              transform: translateY(20px);
            }
            to {
              opacity: 1;
              transform: translateY(0);
            }
          }
        `}</style>
      </div>
    </div>
  );
}

export default Chat;