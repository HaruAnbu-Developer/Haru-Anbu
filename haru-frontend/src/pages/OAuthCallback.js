import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithKakao } = useAuth();
  const [errorMsg, setErrorMsg] = useState(null);
  const hasCalledRef = useRef(false);

  useEffect(() => {
    if (hasCalledRef.current) return;

    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error) {
      setErrorMsg('Kakao 로그인이 취소되었습니다.');
      setTimeout(() => navigate('/login', { replace: true }), 2000);
      return;
    }

    if (!code) {
      setErrorMsg('인가 코드가 없습니다.');
      setTimeout(() => navigate('/login', { replace: true }), 2000);
      return;
    }

    hasCalledRef.current = true;
    const redirectUri = `${window.location.origin}/oauth/callback`;

    const handleKakaoLogin = async () => {
      const result = await loginWithKakao(code, redirectUri);
      if (result.success) {
        navigate('/', { replace: true });
      } else {
        setErrorMsg(result.message || 'Kakao 로그인에 실패했습니다.');
        setTimeout(() => navigate('/login', { replace: true }), 2000);
      }
    };

    handleKakaoLogin();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  return (
    <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
      <div className="text-center">
        {errorMsg ? (
          <>
            <div className="alert alert-danger" style={{ borderRadius: '10px' }}>
              {errorMsg}
            </div>
            <p style={{ color: '#666', fontSize: '14px' }}>로그인 페이지로 이동합니다...</p>
          </>
        ) : (
          <>
            <div className="spinner-border mb-3" style={{ color: '#6C63FF' }} role="status">
              <span className="visually-hidden">로딩 중...</span>
            </div>
            <p style={{ color: '#666' }}>로그인 처리 중...</p>
          </>
        )}
      </div>
    </div>
  );
}

export default OAuthCallback;
