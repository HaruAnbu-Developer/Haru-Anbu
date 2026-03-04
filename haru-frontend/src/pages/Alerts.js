// 📁 src/pages/Alerts.js
function Alerts() {
  return (
    <div className="container-fluid px-4 pt-3 pb-5 bg-white">
      <h2 className="mb-4 text-center fw-bold">알림</h2>
      <div className="card">
        <div className="card-body text-center py-5">
          <i className="fas fa-bell fa-3x text-muted mb-3"></i>
          <p className="text-muted">새로운 알림이 없습니다</p>
        </div>
      </div>
    </div>
  );
}

export default Alerts;