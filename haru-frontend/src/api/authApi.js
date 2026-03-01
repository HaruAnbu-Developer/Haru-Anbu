import api from './axiosInstance';

const KAKAO_CLIENT_ID = process.env.REACT_APP_KAKAO_CLIENT_ID;

export const loginWithEmail = async (email, password) => {
  const response = await api.post('/auth/login', {
    username: email,
    password,
  });
  return response.data;
};

export const fetchCurrentUser = async () => {
  const response = await api.get('/auth/me');
  return response.data;
};

export const kakaoLoginWithCode = async (code, redirectUri) => {
  const response = await api.post('/auth/oauth/kakao', {
    code,
    redirectUri,
  });
  return response.data;
};

export const logout = async () => {
  const response = await api.post('/auth/logout');
  return response.data;
};

export const signupWithEmail = async (username, email, password, name) => {
  const response = await api.post('/auth/signup', {
    username,
    email,
    password,
    name,
  });
  return response.data;
};

export const getKakaoAuthUrl = (redirectUri) => {
  const params = new URLSearchParams({
    client_id: KAKAO_CLIENT_ID,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'profile_nickname',
  });
  return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
};
