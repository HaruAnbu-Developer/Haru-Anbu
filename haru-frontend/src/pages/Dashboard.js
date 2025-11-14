import { useState } from "react";

function VoiceCallScreen() {
  const [isCalling, setIsCalling] = useState(false);

  const handleMicClick = () => {
    setIsCalling(!isCalling);
    if (!isCalling) {
      console.log("AI 음성 통화 시작");
    } else {
      console.log("AI 음성 통화 종료");
    }
  };

  return (
    <div style={{ maxWidth: '430px', margin: '0 auto', boxShadow: '0 0 20px rgba(0,0,0,0.1)' }}>
      {/* 메인 음성 통화 화면 */}
      <div 
        style={{ 
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          minHeight: '100vh',
          position: 'relative',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '40px 20px'
        }}
      >
        {/* AI 아이콘 상단 */}
        <div className="mb-4" style={{ animation: isCalling ? 'bounce 2s ease-in-out infinite' : 'none' }}>
          <div style={{
            width: '100px',
            height: '100px',
            borderRadius: '50%',
            background: 'rgba(255, 255, 255, 0.15)',
            backdropFilter: 'blur(10px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
          }}>
            <i className="fas fa-robot" style={{ fontSize: '50px', color: 'white' }}></i>
          </div>
        </div>

        {/* 상태 표시 */}
        <div className="text-center mb-5">
          <h2 className="text-white fw-bold mb-3" style={{ fontSize: '2rem', letterSpacing: '0.5px' }}>
            {isCalling ? "통화 중" : "하루안부 AI"}
          </h2>
          <p className="text-white mb-2" style={{ fontSize: '18px', opacity: 0.95 }}>
            {isCalling ? "편하게 대화해보세요" : "언제든 대화를 시작하세요"}
          </p>
          {isCalling && (
            <div style={{ animation: 'pulse-text 1.5s ease-in-out infinite' }}>
              <small className="text-white" style={{ fontSize: '14px', opacity: 0.8 }}>
                <i className="fas fa-circle me-2" style={{ fontSize: '8px', color: '#4ade80' }}></i>
                연결됨
              </small>
            </div>
          )}
        </div>

        {/* 마이크 버튼 */}
        <div className="position-relative mb-5">
          {/* 통화 중 멀티 웨이브 애니메이션 */}
          {isCalling && (
            <>
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '160px',
                height: '160px',
                borderRadius: '50%',
                border: '3px solid rgba(255, 255, 255, 0.3)',
                animation: 'wave 2s ease-out infinite',
                zIndex: 1
              }}></div>
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '160px',
                height: '160px',
                borderRadius: '50%',
                border: '3px solid rgba(255, 255, 255, 0.3)',
                animation: 'wave 2s ease-out infinite 0.7s',
                zIndex: 2
              }}></div>
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '160px',
                height: '160px',
                borderRadius: '50%',
                border: '3px solid rgba(255, 255, 255, 0.3)',
                animation: 'wave 2s ease-out infinite 1.4s',
                zIndex: 3
              }}></div>
            </>
          )}

          {/* 메인 마이크 버튼 */}
          <button
            onClick={handleMicClick}
            style={{
              width: '160px',
              height: '160px',
              borderRadius: '50%',
              border: 'none',
              background: isCalling 
                ? 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)'
                : 'linear-gradient(135deg, #ffffff 0%, #f8fafc 100%)',
              boxShadow: isCalling 
                ? '0 20px 60px rgba(239, 68, 68, 0.4)'
                : '0 20px 60px rgba(0, 0, 0, 0.2)',
              cursor: 'pointer',
              transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
              position: 'relative',
              zIndex: 4
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'scale(1.08)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'scale(1)';
            }}
          >
            <i 
              className={`fas ${isCalling ? 'fa-phone-slash' : 'fa-microphone'}`}
              style={{ 
                fontSize: '65px', 
                color: isCalling ? 'white' : '#667eea',
                transition: 'all 0.3s ease'
              }}
            ></i>
          </button>
        </div>

        {/* 안내 문구 */}
        <div className="text-center">
          <p className="text-white mb-2" style={{ fontSize: '15px', opacity: 0.85 }}>
            {isCalling ? "버튼을 눌러 통화를 종료하세요" : "버튼을 눌러 음성 대화를 시작하세요"}
          </p>
        </div>


        {/* CSS 애니메이션 */}
        <style>{`
          @keyframes wave {
            0% {
              transform: translate(-50%, -50%) scale(1);
              opacity: 0.6;
            }
            100% {
              transform: translate(-50%, -50%) scale(2.2);
              opacity: 0;
            }
          }

          @keyframes bounce {
            0%, 100% {
              transform: translateY(0);
            }
            50% {
              transform: translateY(-10px);
            }
          }

          @keyframes pulse-text {
            0%, 100% {
              opacity: 1;
            }
            50% {
              opacity: 0.6;
            }
          }
        `}</style>
      </div>
    </div>
  );
}

export default VoiceCallScreen;