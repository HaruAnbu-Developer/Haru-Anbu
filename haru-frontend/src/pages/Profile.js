import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

function Profile() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const providerName = {
    LOCAL: '이메일',
    GOOGLE: 'Google',
    KAKAO: 'Kakao',
    NAVER: 'Naver',
  };

  return (
    <div className="container-fluid px-4 pt-3 pb-5 bg-white">
      <h2 className="mb-4 text-center fw-bold">프로필</h2>

      <div className="card mb-3">
        <div className="card-body text-center py-4">
          <div className="mb-3">
            {user?.profileImageUrl ? (
              <img
                src={user.profileImageUrl}
                alt="프로필"
                style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover' }}
              />
            ) : (
              <i className="fas fa-user-circle fa-5x text-secondary"></i>
            )}
          </div>
          <h4 className="mb-1">{user?.name || '사용자'}</h4>
          <p className="text-muted mb-0">
            {providerName[user?.provider] || ''} 계정
          </p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-envelope me-2"></i>이메일</span>
              <span className="text-muted">{user?.email || '-'}</span>
            </div>
          </div>
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-user me-2"></i>아이디</span>
              <span className="text-muted">{user?.username || '-'}</span>
            </div>
          </div>
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-shield-alt me-2"></i>이메일 인증</span>
              <span className={user?.emailVerified ? 'text-success' : 'text-danger'}>
                {user?.emailVerified ? '인증됨' : '미인증'}
              </span>
            </div>
          </div>
          <div className="mb-3">
            <Link to="/profile/detail" className="btn btn-outline-primary w-100">
              <i className="fas fa-cog me-2"></i>회원정보 수정
            </Link>
          </div>
          <div>
            <button className="btn btn-outline-danger w-100" onClick={handleLogout}>
              <i className="fas fa-sign-out-alt me-2"></i>로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;
