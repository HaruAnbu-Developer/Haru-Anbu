// 📁 src/App.js
import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "@fortawesome/fontawesome-free/css/all.min.css";
import "./styles.css";

// Components
import SplashScreen from "./components/SplashScreen";
import MainLayout from "./components/MainLayout";

// Pages
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Chat from "./pages/Chat";
import Alerts from "./pages/Alerts";
import Profile from "./pages/Profile";


function AppContent() {
  const [loading, setLoading] = useState(true);
  const [fadeOut, setFadeOut] = useState(false);
  const [showSplash, setShowSplash] = useState(true); // ← 추가!
  const navigate = useNavigate();

  useEffect(() => {
    // 스플래시를 한 번만 보여줌
    if (!showSplash) return;

    const fadeTimer = setTimeout(() => {
      setFadeOut(true);
    }, 500);

    const removeTimer = setTimeout(() => {
      setLoading(false);
      setShowSplash(false); // ← 스플래시 완료 표시
      navigate('/login', { replace: true });
    }, 800);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(removeTimer);
    };
  }, [navigate , showSplash]); 

  return (
    <div style={{ 
      maxWidth: '430px', 
      margin: '0 auto',
      position: 'relative',
      boxShadow: '0 0 20px rgba(0,0,0,0.1)',
      minHeight: '100vh'
    }}>
      {loading && <SplashScreen fadeOut={fadeOut} />}
      
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<MainLayout><Dashboard /></MainLayout>} />
        <Route path="/chat" element={<MainLayout><Chat /></MainLayout>} />
        <Route path="/alerts" element={<MainLayout><Alerts /></MainLayout>} />
        <Route path="/profile" element={<MainLayout><Profile /></MainLayout>} />
      </Routes>
    </div>
  );
}
function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;