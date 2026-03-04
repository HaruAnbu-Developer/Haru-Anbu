import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { signupWithEmail } from "../api/authApi";

function Signup() {
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const validate = () => {
    if (username.length < 4) {
      setError("사용자명은 4자 이상이어야 합니다.");
      return false;
    }
    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return false;
    }
    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return false;
    }
    return true;
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    setError(null);

    if (!validate()) return;

    setIsLoading(true);
    try {
      const result = await signupWithEmail(username, email, password, name);
      if (result.success) {
        setSuccess(true);
      } else {
        setError(result.error?.message || result.message || "회원가입에 실패했습니다.");
      }
    } catch (err) {
      if (err.response?.data?.error?.message) {
        setError(err.response.data.error.message);
      } else if (err.request) {
        setError("서버에 연결할 수 없습니다. 네트워크를 확인해주세요.");
      } else {
        setError("회원가입 중 오류가 발생했습니다.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div
        className="d-flex flex-column justify-content-center align-items-center"
        style={{
          background: 'linear-gradient(135deg, #ffffffff)',
          minHeight: '100vh',
          padding: '20px'
        }}
      >
        <div className="text-center mb-4">
          <h2 className="fw-bold mb-2" style={{ color: '#6C63FF' }}>하루안부</h2>
        </div>
        <div
          className="card p-4 text-center"
          style={{
            width: '100%',
            maxWidth: '380px',
            borderRadius: '20px',
            border: 'none',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
          }}
        >
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎉</div>
          <h5 className="fw-bold mb-3">회원가입 완료!</h5>
          <p style={{ fontSize: '14px', color: '#666', marginBottom: '24px' }}>
            회원가입이 완료되었습니다.<br />
            로그인해주세요.
          </p>
          <button
            className="btn btn-primary w-100"
            onClick={() => navigate('/login')}
            style={{
              padding: '12px',
              borderRadius: '10px',
              border: 'none',
              background: 'linear-gradient(135deg, #6C63FF)',
              fontWeight: 'bold',
              fontSize: '16px'
            }}
          >
            로그인으로 이동
          </button>
        </div>
      </div>
    );
  }

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
          새로운 계정 만들기
        </p>
      </div>

      {/* 회원가입 폼 */}
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
        <form onSubmit={handleSignup}>
          {error && (
            <div className="alert alert-danger py-2 mb-3"
                 style={{ fontSize: '14px', borderRadius: '10px' }}>
              {error}
            </div>
          )}

          <div className="mb-3">
            <label className="form-label fw-semibold">이름</label>
            <input
              type="text"
              className="form-control"
              placeholder="홍길동"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              disabled={isLoading}
              maxLength={100}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-semibold">사용자명</label>
            <input
              type="text"
              className="form-control"
              placeholder="4자 이상 입력"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={isLoading}
              minLength={4}
              maxLength={50}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
          </div>

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
              maxLength={100}
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
              placeholder="8자 이상 입력"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={isLoading}
              minLength={8}
              maxLength={100}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-semibold">비밀번호 확인</label>
            <input
              type="password"
              className="form-control"
              placeholder="비밀번호를 다시 입력"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              required
              disabled={isLoading}
              style={{
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #e0e0e0'
              }}
            />
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
                가입 중...
              </>
            ) : '회원가입'}
          </button>

          <div className="text-center">
            <span style={{ fontSize: '14px', color: '#666' }}>
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-decoration-none fw-bold" style={{ color: '#6C63FF' }}>
                로그인
              </Link>
            </span>
          </div>
        </form>
      </div>
    </div>
  );
}

export default Signup;
