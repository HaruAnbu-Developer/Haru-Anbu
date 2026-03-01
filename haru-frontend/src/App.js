import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "@fortawesome/fontawesome-free/css/all.min.css";
import "./styles.css";

import { AuthProvider, useAuth } from "./contexts/AuthContext";

import SplashScreen from "./components/SplashScreen";
import MainLayout from "./components/MainLayout";
import ProtectedRoute from "./components/ProtectedRoute";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Chat from "./pages/Chat";
import Alerts from "./pages/Alerts";
import Profile from "./pages/Profile";
import ProfileDetail from "./pages/ProfileDetail";
import OAuthCallback from "./pages/OAuthCallback";
import Signup from "./pages/Signup";

function AppContent() {
  const [fadeOut, setFadeOut] = useState(false);
  const [showSplash, setShowSplash] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (!showSplash) return;
    if (loading) return;

    const fadeTimer = setTimeout(() => {
      setFadeOut(true);
    }, 500);

    const removeTimer = setTimeout(() => {
      setShowSplash(false);
      if (isAuthenticated) {
        navigate('/', { replace: true });
      } else {
        navigate('/login', { replace: true });
      }
    }, 800);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(removeTimer);
    };
  }, [navigate, showSplash, loading, isAuthenticated]);

  const isOAuthCallback = location.pathname === '/oauth/callback';

  return (
    <div style={{
      maxWidth: '430px',
      margin: '0 auto',
      position: 'relative',
      boxShadow: '0 0 20px rgba(0,0,0,0.1)',
      minHeight: '100vh'
    }}>
      {showSplash && !isOAuthCallback && <SplashScreen fadeOut={fadeOut} />}

      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/oauth/callback" element={<OAuthCallback />} />
        <Route path="/" element={
          <ProtectedRoute>
            <MainLayout><Dashboard /></MainLayout>
          </ProtectedRoute>
        } />
        <Route path="/chat" element={
          <ProtectedRoute>
            <MainLayout><Chat /></MainLayout>
          </ProtectedRoute>
        } />
        <Route path="/alerts" element={
          <ProtectedRoute>
            <MainLayout><Alerts /></MainLayout>
          </ProtectedRoute>
        } />
        <Route path="/profile" element={
          <ProtectedRoute>
            <MainLayout><Profile /></MainLayout>
          </ProtectedRoute>
        } />
        <Route path="/profile/detail" element={
          <ProtectedRoute>
            <MainLayout><ProfileDetail /></MainLayout>
          </ProtectedRoute>
        } />
      </Routes>
    </div>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  );
}

export default App;
