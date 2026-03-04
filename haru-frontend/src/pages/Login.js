import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { getKakaoAuthUrl } from "../api/authApi";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { login, error, clearError, isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    return () => clearError();
  }, [clearError]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    const result = await login(email, password);
    if (result.success) {
      navigate('/', { replace: true });
    }

    setIsLoading(false);
  };

  const handleKakaoLogin = () => {
    const redirectUri = `${window.location.origin}/oauth/callback`;
    window.location.href = getKakaoAuthUrl(redirectUri);
  };

  return (
    <div
      className="d-flex flex-column justify-content-center align-items-center"
      style={{
        background: 'linear-gradient(135deg, #ffffffff)',
        minHeight: '100vh',
        padding: '20px'
      }}
    >
      {/* 로고 */}
      <div className="text-center mb-4">
        <h2 className="fw-bold mb-2" style={{ color: '#6C63FF' }}>하루안부</h2>
        <p style={{ fontSize: '14px', opacity: 0.9, color: '#6C63FF' }}>
          매일 건강을 지켜드립니다
        </p>
      </div>

      {/* 로그인 폼 */}
      <div
        className="card p-4"
        style={{
          width: '100%',
          maxWidth: '380px',
          borderRadius: '20px',
          border: 'none',
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
        }}
      >
        <form onSubmit={handleLogin}>
          {error && (
            <div className="alert alert-danger py-2 mb-3"
                 style={{ fontSize: '14px', borderRadius: '10px' }}>
              {error}
            </div>
          )}

          <div className="mb-3">
            <label className="form-label fw-semibold">이메일</label>
            <input
              type="email"
              className="form-control"
              placeholder="example@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={isLoading}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-semibold">비밀번호</label>
            <input
              type="password"
              className="form-control"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={isLoading}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
          </div>

          <div className="mb-3 d-flex justify-content-between align-items-center">
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                id="rememberMe"
              />
              <label className="form-check-label" htmlFor="rememberMe" style={{ fontSize: '14px' }}>
                로그인 상태 유지
              </label>
            </div>
            <a href="#!" className="text-decoration-none" style={{ fontSize: '14px', color: '#6C63FF' }}>
              비밀번호 찾기
            </a>
          </div>

          <button
            type="submit"
            className="btn btn-primary w-100 mb-3"
            disabled={isLoading}
            style={{
              padding: '12px',
              borderRadius: '10px',
              border: 'none',
              background: 'linear-gradient(135deg, #6C63FF)',
              fontWeight: 'bold',
              fontSize: '16px'
            }}
          >
            {isLoading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" />
                로그인 중...
              </>
            ) : '로그인'}
          </button>

          <div className="text-center">
            <span style={{ fontSize: '14px', color: '#666' }}>
              계정이 없으신가요?{' '}
              <Link to="/signup" className="text-decoration-none fw-bold" style={{ color: '#6C63FF' }}>
                회원가입
              </Link>
            </span>
          </div>
        </form>
      </div>

      {/* 간편 로그인 */}
      <div className="mt-4" style={{ width: '100%', maxWidth: '380px' }}>
        <div className="text-center mb-3">
          <small style={{ fontSize: '14px', opacity: 0.8, color: '#6C63FF' }}>
            또는 간편 로그인
          </small>
        </div>
        <div className="d-flex gap-2">
          <button
            className="btn btn-light flex-fill"
            disabled
            style={{
              padding: '12px',
              borderRadius: '10px',
              border: 'none',
              boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)',
              opacity: 0.5
            }}
          >
            <i className="fab fa-google me-2" style={{ color: '#DB4437' }}></i>
            Google
          </button>
          <button
            className="btn btn-light flex-fill"
            onClick={handleKakaoLogin}
            disabled={isLoading}
            style={{
              padding: '12px',
              borderRadius: '10px',
              border: 'none',
              boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)'
            }}
          >
            <i className="fas fa-comment me-2" style={{ color: '#FEE500' }}></i>
            Kakao
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login;
