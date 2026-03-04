import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  getMyProfile,
  updateProfile,
  changePassword,
  changeUsername,
  deleteAccount,
  getProviderInfo,
  resendVerificationEmail,
} from '../api/userApi';

function ProfileDetail() {
  const { user, updateUser, logout } = useAuth();
  const navigate = useNavigate();

  // 프로필 데이터
  const [profile, setProfile] = useState(null);
  const [providerInfo, setProviderInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  // 프로필 수정
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', phoneNumber: '', dateOfBirth: '' });

  // 아이디 변경
  const [showUsernameChange, setShowUsernameChange] = useState(false);
  const [newUsername, setNewUsername] = useState('');

  // 비밀번호 변경
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // 계정 삭제
  const [showDeleteAccount, setShowDeleteAccount] = useState(false);
  const [deleteForm, setDeleteForm] = useState({ password: '', confirmationText: '' });

  // 메시지
  const [message, setMessage] = useState({ text: '', type: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [profileRes, providerRes] = await Promise.all([
        getMyProfile(),
        getProviderInfo(),
      ]);
      if (profileRes.success) {
        setProfile(profileRes.data);
        setEditForm({
          name: profileRes.data.name || '',
          phoneNumber: profileRes.data.phoneNumber || '',
          dateOfBirth: profileRes.data.dateOfBirth || '',
        });
      }
      if (providerRes.success) {
        setProviderInfo(providerRes.data);
      }
    } catch (err) {
      showMessage('정보를 불러오는데 실패했습니다.', 'danger');
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage({ text: '', type: '' }), 3000);
  };

  // 이메일 인증 요청
  const handleResendVerification = async () => {
    try {
      setSubmitting(true);
      const result = await resendVerificationEmail(profile.email);
      if (result.success) {
        showMessage('인증 이메일이 발송되었습니다. 메일함을 확인해주세요.');
      }
    } catch (err) {
      const msg = err.response?.data?.error?.message || '인증 이메일 발송에 실패했습니다.';
      showMessage(msg, 'danger');
    } finally {
      setSubmitting(false);
    }
  };

  // 프로필 수정
  const handleEditProfile = () => {
    setEditMode(true);
    setEditForm({
      name: profile?.name || '',
      phoneNumber: profile?.phoneNumber || '',
      dateOfBirth: profile?.dateOfBirth || '',
    });
  };

  const handleCancelEdit = () => {
    setEditMode(false);
  };

  const handleSaveProfile = async () => {
    try {
      setSubmitting(true);
      const data = {};
      if (editForm.name) data.name = editForm.name;
      if (editForm.phoneNumber) data.phoneNumber = editForm.phoneNumber;
      if (editForm.dateOfBirth) data.dateOfBirth = editForm.dateOfBirth;

      const result = await updateProfile(data);
      if (result.success) {
        setProfile(result.data);
        updateUser(result.data);
        setEditMode(false);
        showMessage('프로필이 수정되었습니다.');
      }
    } catch (err) {
      const msg = err.response?.data?.error?.message || '프로필 수정에 실패했습니다.';
      showMessage(msg, 'danger');
    } finally {
      setSubmitting(false);
    }
  };

  // 아이디 변경
  const handleChangeUsername = async () => {
    if (!newUsername || newUsername.length < 4) {
      showMessage('아이디는 4자 이상이어야 합니다.', 'warning');
      return;
    }
    try {
      setSubmitting(true);
      const result = await changeUsername(newUsername);
      if (result.success) {
        setProfile(result.data);
        updateUser(result.data);
        setShowUsernameChange(false);
        setNewUsername('');
        showMessage('아이디가 변경되었습니다.');
      }
    } catch (err) {
      const msg = err.response?.data?.error?.message || '아이디 변경에 실패했습니다.';
      showMessage(msg, 'danger');
    } finally {
      setSubmitting(false);
    }
  };

  // 비밀번호 변경
  const handleChangePassword = async () => {
    if (passwordForm.newPassword.length < 8) {
      showMessage('새 비밀번호는 8자 이상이어야 합니다.', 'warning');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      showMessage('새 비밀번호가 일치하지 않습니다.', 'warning');
      return;
    }
    try {
      setSubmitting(true);
      const result = await changePassword(
        passwordForm.currentPassword,
        passwordForm.newPassword,
        passwordForm.confirmPassword
      );
      if (result.success) {
        setShowPasswordChange(false);
        setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
        showMessage('비밀번호가 변경되었습니다.');
      }
    } catch (err) {
      const msg = err.response?.data?.error?.message || '비밀번호 변경에 실패했습니다.';
      showMessage(msg, 'danger');
    } finally {
      setSubmitting(false);
    }
  };

  // 계정 삭제
  const handleDeleteAccount = async () => {
    if (deleteForm.confirmationText !== '계정을 삭제합니다') {
      showMessage("'계정을 삭제합니다'를 정확히 입력해주세요.", 'warning');
      return;
    }
    try {
      setSubmitting(true);
      const result = await deleteAccount(deleteForm.password || null, deleteForm.confirmationText);
      if (result.success) {
        await logout();
        navigate('/login', { replace: true });
      }
    } catch (err) {
      const msg = err.response?.data?.error?.message || '계정 삭제에 실패했습니다.';
      showMessage(msg, 'danger');
    } finally {
      setSubmitting(false);
    }
  };

  const providerName = {
    LOCAL: '이메일',
    GOOGLE: 'Google',
    KAKAO: 'Kakao',
    NAVER: 'Naver',
  };

  if (loading) {
    return (
      <div className="container-fluid px-4 pt-5 text-center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="container-fluid px-4 pt-3 pb-5 bg-white">
      <div className="d-flex align-items-center mb-4">
        <button
          className="btn btn-link text-dark p-0 me-3"
          onClick={() => navigate('/profile')}
        >
          <i className="fas fa-arrow-left fa-lg"></i>
        </button>
        <h2 className="mb-0 fw-bold">회원정보</h2>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type} py-2`} role="alert">
          {message.text}
        </div>
      )}

      {/* 기본 정보 */}
      <div className="card mb-3">
        <div className="card-body">
          <h6 className="fw-bold mb-3">
            <i className="fas fa-info-circle me-2"></i>기본 정보
          </h6>
          <div className="mb-2 pb-2 border-bottom">
            <small className="text-muted">이메일</small>
            <div>{profile?.email || '-'}</div>
          </div>
          <div className="mb-2 pb-2 border-bottom">
            <small className="text-muted">가입 방식</small>
            <div>{providerName[profile?.provider] || '-'}</div>
          </div>
          <div>
            <small className="text-muted">가입일</small>
            <div>{profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('ko-KR') : '-'}</div>
          </div>
        </div>
      </div>

      {/* 이메일 인증 */}
      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center">
            <h6 className="fw-bold mb-0">
              <i className="fas fa-shield-alt me-2"></i>이메일 인증
            </h6>
            {!profile?.emailVerified && (
              <button
                className="btn btn-sm btn-outline-primary"
                onClick={handleResendVerification}
                disabled={submitting}
              >
                {submitting ? '발송 중...' : '인증 요청'}
              </button>
            )}
          </div>
          <div className="mt-2">
            <span className={profile?.emailVerified ? 'text-success' : 'text-danger'}>
              {profile?.emailVerified ? '인증됨' : '미인증'}
            </span>
            {!profile?.emailVerified && (
              <p className="text-muted mb-0 small mt-1">인증 요청 버튼을 눌러 이메일 인증을 완료해주세요.</p>
            )}
          </div>
        </div>
      </div>

      {/* 프로필 정보 (수정 가능) */}
      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="fw-bold mb-0">
              <i className="fas fa-user-edit me-2"></i>프로필 정보
            </h6>
            {!editMode && (
              <button className="btn btn-sm btn-outline-primary" onClick={handleEditProfile}>
                수정
              </button>
            )}
          </div>

          {editMode ? (
            <>
              <div className="mb-3">
                <label className="form-label small text-muted">이름</label>
                <input
                  type="text"
                  className="form-control"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                />
              </div>
              <div className="mb-3">
                <label className="form-label small text-muted">전화번호</label>
                <input
                  type="tel"
                  className="form-control"
                  placeholder="010-1234-5678"
                  value={editForm.phoneNumber}
                  onChange={(e) => setEditForm({ ...editForm, phoneNumber: e.target.value })}
                />
              </div>
              <div className="mb-3">
                <label className="form-label small text-muted">생년월일</label>
                <input
                  type="date"
                  className="form-control"
                  value={editForm.dateOfBirth}
                  onChange={(e) => setEditForm({ ...editForm, dateOfBirth: e.target.value })}
                />
              </div>
              <div className="d-flex gap-2">
                <button
                  className="btn btn-primary flex-fill"
                  onClick={handleSaveProfile}
                  disabled={submitting}
                >
                  {submitting ? '저장 중...' : '저장'}
                </button>
                <button
                  className="btn btn-outline-secondary flex-fill"
                  onClick={handleCancelEdit}
                  disabled={submitting}
                >
                  취소
                </button>
              </div>
            </>
          ) : (
            <>
              <div className="mb-2 pb-2 border-bottom">
                <small className="text-muted">이름</small>
                <div>{profile?.name || '-'}</div>
              </div>
              <div className="mb-2 pb-2 border-bottom">
                <small className="text-muted">전화번호</small>
                <div>{profile?.phoneNumber || '-'}</div>
              </div>
              <div>
                <small className="text-muted">생년월일</small>
                <div>{profile?.dateOfBirth || '-'}</div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* 아이디 변경 */}
      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="fw-bold mb-0">
              <i className="fas fa-id-card me-2"></i>아이디
            </h6>
            {!showUsernameChange && (
              <button
                className="btn btn-sm btn-outline-primary"
                onClick={() => setShowUsernameChange(true)}
              >
                변경
              </button>
            )}
          </div>

          <div className="mb-2">
            <small className="text-muted">현재 아이디</small>
            <div>{profile?.username || '-'}</div>
          </div>

          {showUsernameChange && (
            <div className="mt-3">
              <div className="mb-3">
                <label className="form-label small text-muted">새 아이디 (4자 이상)</label>
                <input
                  type="text"
                  className="form-control"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  minLength={4}
                />
              </div>
              <div className="d-flex gap-2">
                <button
                  className="btn btn-primary flex-fill"
                  onClick={handleChangeUsername}
                  disabled={submitting}
                >
                  {submitting ? '변경 중...' : '변경'}
                </button>
                <button
                  className="btn btn-outline-secondary flex-fill"
                  onClick={() => { setShowUsernameChange(false); setNewUsername(''); }}
                  disabled={submitting}
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 비밀번호 변경 (LOCAL 사용자만) */}
      {providerInfo?.canChangePassword && (
        <div className="card mb-3">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">
                <i className="fas fa-lock me-2"></i>비밀번호
              </h6>
              {!showPasswordChange && (
                <button
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => setShowPasswordChange(true)}
                >
                  변경
                </button>
              )}
            </div>

            {showPasswordChange ? (
              <>
                <div className="mb-3">
                  <label className="form-label small text-muted">현재 비밀번호</label>
                  <input
                    type="password"
                    className="form-control"
                    value={passwordForm.currentPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, currentPassword: e.target.value })
                    }
                  />
                </div>
                <div className="mb-3">
                  <label className="form-label small text-muted">새 비밀번호 (8자 이상)</label>
                  <input
                    type="password"
                    className="form-control"
                    value={passwordForm.newPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                    }
                  />
                </div>
                <div className="mb-3">
                  <label className="form-label small text-muted">새 비밀번호 확인</label>
                  <input
                    type="password"
                    className="form-control"
                    value={passwordForm.confirmPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
                    }
                  />
                </div>
                <div className="d-flex gap-2">
                  <button
                    className="btn btn-primary flex-fill"
                    onClick={handleChangePassword}
                    disabled={submitting}
                  >
                    {submitting ? '변경 중...' : '변경'}
                  </button>
                  <button
                    className="btn btn-outline-secondary flex-fill"
                    onClick={() => {
                      setShowPasswordChange(false);
                      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
                    }}
                    disabled={submitting}
                  >
                    취소
                  </button>
                </div>
              </>
            ) : (
              <p className="text-muted mb-0 small">비밀번호를 변경할 수 있습니다.</p>
            )}
          </div>
        </div>
      )}

      {/* 계정 삭제 */}
      <div className="card mb-3 border-danger">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="fw-bold mb-0 text-danger">
              <i className="fas fa-exclamation-triangle me-2"></i>계정 삭제
            </h6>
            {!showDeleteAccount && (
              <button
                className="btn btn-sm btn-outline-danger"
                onClick={() => setShowDeleteAccount(true)}
              >
                삭제
              </button>
            )}
          </div>

          {showDeleteAccount ? (
            <>
              <div className="alert alert-danger py-2 small">
                계정을 삭제하면 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.
              </div>

              {providerInfo?.canChangePassword && (
                <div className="mb-3">
                  <label className="form-label small text-muted">비밀번호</label>
                  <input
                    type="password"
                    className="form-control"
                    value={deleteForm.password}
                    onChange={(e) =>
                      setDeleteForm({ ...deleteForm, password: e.target.value })
                    }
                  />
                </div>
              )}

              <div className="mb-3">
                <label className="form-label small text-muted">
                  확인을 위해 <strong>'계정을 삭제합니다'</strong>를 입력해주세요
                </label>
                <input
                  type="text"
                  className="form-control"
                  value={deleteForm.confirmationText}
                  onChange={(e) =>
                    setDeleteForm({ ...deleteForm, confirmationText: e.target.value })
                  }
                />
              </div>

              <div className="d-flex gap-2">
                <button
                  className="btn btn-danger flex-fill"
                  onClick={handleDeleteAccount}
                  disabled={submitting}
                >
                  {submitting ? '삭제 중...' : '계정 삭제'}
                </button>
                <button
                  className="btn btn-outline-secondary flex-fill"
                  onClick={() => {
                    setShowDeleteAccount(false);
                    setDeleteForm({ password: '', confirmationText: '' });
                  }}
                  disabled={submitting}
                >
                  취소
                </button>
              </div>
            </>
          ) : (
            <p className="text-muted mb-0 small">계정을 영구적으로 삭제합니다.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default ProfileDetail;
