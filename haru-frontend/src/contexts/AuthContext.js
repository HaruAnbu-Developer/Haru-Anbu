import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loginWithEmail, fetchCurrentUser, kakaoLoginWithCode, logout as logoutApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const checkAuth = useCallback(async () => {
    try {
      setLoading(true);
      const result = await fetchCurrentUser();
      if (result.success && result.data) {
        setUser(result.data);
      } else {
        setUser(null);
      }
    } catch (err) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (email, password) => {
    try {
      setError(null);
      const result = await loginWithEmail(email, password);

      if (result.success && result.data) {
        setUser(result.data);
        return { success: true };
      } else {
        const message = result.error?.message || result.message || '로그인에 실패했습니다.';
        setError(message);
        return { success: false, message };
      }
    } catch (err) {
      let message = '로그인 중 오류가 발생했습니다.';

      if (err.response) {
        const data = err.response.data;
        if (data?.error?.message) {
          message = data.error.message;
        } else if (err.response.status === 401) {
          message = '이메일 또는 비밀번호가 올바르지 않습니다.';
        }
      } else if (err.request) {
        message = '서버에 연결할 수 없습니다. 네트워크를 확인해주세요.';
      }

      setError(message);
      return { success: false, message };
    }
  };

  const loginWithKakao = async (code, redirectUri) => {
    try {
      setError(null);
      const result = await kakaoLoginWithCode(code, redirectUri);

      if (result.success && result.data) {
        setUser(result.data);
        return { success: true };
      } else {
        const message = result.error?.message || 'Kakao 로그인에 실패했습니다.';
        setError(message);
        return { success: false, message };
      }
    } catch (err) {
      let message = 'Kakao 로그인 중 오류가 발생했습니다.';

      if (err.response?.data?.error?.message) {
        message = err.response.data.error.message;
      } else if (err.request) {
        message = '서버에 연결할 수 없습니다.';
      }

      setError(message);
      return { success: false, message };
    }
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch (err) {
      // 로그아웃 API 실패해도 클라이언트 상태는 초기화
    }
    setUser(null);
    setError(null);
  };

  const updateUser = (userData) => {
    setUser((prev) => (prev ? { ...prev, ...userData } : prev));
  };

  const clearError = () => setError(null);

  const value = {
    user,
    loading,
    error,
    login,
    loginWithKakao,
    logout,
    checkAuth,
    updateUser,
    clearError,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
