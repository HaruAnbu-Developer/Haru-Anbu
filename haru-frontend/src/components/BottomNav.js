// 📁 src/components/BottomNav.js
import { useNavigate, useLocation } from "react-router-dom";

function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  // 🎨 통일된 색상 코드 및 필터 (PNG 색조 보정)
  const activeColor = '#6C63FF';
  const activeFilter =
    'brightness(0) saturate(100%) invert(36%) sepia(97%) ' +
    'saturate(747%) hue-rotate(222deg) brightness(101%) contrast(94%)';

  return (
    <nav className="navbar navbar-light bg-white border-top fixed-bottom">
      <div className="container d-flex justify-content-around">

        {/* 홈 버튼 */}
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/mike_icon.png')}
            alt="Home Icon"
            style={{
              width: "22px",
              height: "22px",
              marginBottom: "3px",
              filter: location.pathname === '/' ? activeFilter : 'none'
            }}
          />
          <div
            style={{
              fontSize: "12px",
              color: location.pathname === '/' ? activeColor : '#212529'
            }}
          >
            홈
          </div>
        </button>

        {/* 챗봇 버튼 */}
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/chat')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/Chat_icon.png')}
            alt="Chat Icon"
            style={{
              width: "22px",
              height: "22px",
              marginBottom: "3px",
              filter: location.pathname === '/chat' ? activeFilter : 'none'
            }}
          />
          <div
            style={{
              fontSize: "12px",
              color: location.pathname === '/chat' ? activeColor : '#212529'
            }}
          >
            챗봇
          </div>
        </button>

        {/* 알림 버튼 */}
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/alerts')}
          style={{ textDecoration: 'none' }}
        >
          <i
            className="fas fa-bell fa-lg"
            style={{
              color: location.pathname === '/alerts' ? activeColor : '#212529'
            }}
          ></i>
          <div
            style={{
              fontSize: "12px",
              color: location.pathname === '/alerts' ? activeColor : '#212529'
            }}
          >
            알림
          </div>
        </button>

        {/* 마이페이지 버튼 */}
        <button
          className="btn text-center p-0"
          onClick={() => navigate('/profile')}
          style={{ textDecoration: 'none' }}
        >
          <img
            src={require('../assets/profile_icon.png')}
            alt="Profile Icon"
            style={{
              width: "22px",
              height: "22px",
              marginBottom: "3px",
              filter: location.pathname === '/profile' ? activeFilter : 'none'
            }}
          />
          <div
            style={{
              fontSize: "12px",
              color: location.pathname === '/profile' ? activeColor : '#212529'
            }}
          >
            마이페이지
          </div>
        </button>
      </div>
    </nav>
  );
}

export default BottomNav;
