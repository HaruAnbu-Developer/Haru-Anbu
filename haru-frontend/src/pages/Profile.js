// 📁 src/pages/Profile.js
function Profile() {
  return (
    <div className="container-fluid px-4 pt-3 pb-5 bg-white">
      <h2 className="mb-4 text-center fw-bold">프로필</h2>
      
      <div className="card mb-3">
        <div className="card-body text-center py-4">
          <div className="mb-3">
            <i className="fas fa-user-circle fa-5x text-secondary"></i>
          </div>
          <h4 className="mb-1">홍길동</h4>
          <p className="text-muted mb-0">사용자</p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-envelope me-2"></i>이메일</span>
              <span className="text-muted">hong@example.com</span>
            </div>
          </div>
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-phone me-2"></i>전화번호</span>
              <span className="text-muted">010-1234-5678</span>
            </div>
          </div>
          <div className="mb-3 pb-3 border-bottom">
            <div className="d-flex justify-content-between align-items-center">
              <span><i className="fas fa-calendar me-2"></i>가입일</span>
              <span className="text-muted">2025-01-01</span>
            </div>
          </div>
          <div>
            <button className="btn btn-outline-danger w-100">
              <i className="fas fa-sign-out-alt me-2"></i>로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;