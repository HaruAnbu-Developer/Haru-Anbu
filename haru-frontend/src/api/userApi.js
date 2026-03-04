import api from './axiosInstance';

export const getMyProfile = async () => {
  const response = await api.get('/api/users/me/profile');
  return response.data;
};

export const updateProfile = async (data) => {
  const response = await api.patch('/api/users/me/profile', data);
  return response.data;
};

export const changePassword = async (currentPassword, newPassword, confirmPassword) => {
  const response = await api.put('/api/users/me/password', {
    currentPassword,
    newPassword,
    confirmPassword,
  });
  return response.data;
};

export const changeUsername = async (newUsername) => {
  const response = await api.patch('/api/users/me/username', { newUsername });
  return response.data;
};

export const deleteAccount = async (password, confirmationText) => {
  const response = await api.delete('/api/users/me', {
    data: { password, confirmationText },
  });
  return response.data;
};

export const getProviderInfo = async () => {
  const response = await api.get('/api/users/me/provider');
  return response.data;
};

export const resendVerificationEmail = async (email) => {
  const response = await api.post(`/auth/resend-verification?email=${encodeURIComponent(email)}`);
  return response.data;
};
