import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import AuthModal from '../components/AuthModal';
import { useAuth } from '../store';

const navItems = [
  { to: '/', label: '首页' },
  { to: '/news/1', label: '新闻详情' },
  { to: '/profile', label: '个人中心' },
  { to: '/admin', label: '管理后台' },
];

function MainLayout() {
  const { currentUser, isAuthenticated, isBootstrapping, signOut } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');

  useEffect(() => {
    const handleOpenAuthModal = (event: Event) => {
      const customEvent = event as CustomEvent<{ mode?: 'login' | 'register' }>;
      setAuthMode(customEvent.detail?.mode ?? 'login');
      setIsAuthModalOpen(true);
    };

    window.addEventListener('app:open-auth-modal', handleOpenAuthModal);
    return () => window.removeEventListener('app:open-auth-modal', handleOpenAuthModal);
  }, []);

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          TrendPulse
        </Link>
        <nav className="main-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="auth-panel">
          {isBootstrapping ? (
            <span className="auth-status">正在恢复登录状态...</span>
          ) : isAuthenticated && currentUser ? (
            <>
              <span className="auth-status">
                {currentUser.nickname || currentUser.username}
                <small>{currentUser.role}</small>
              </span>
              <button type="button" className="ghost-button" onClick={() => void signOut()}>
                退出登录
              </button>
            </>
          ) : (
            <button
              type="button"
              className="primary-button small"
              onClick={() => {
                setAuthMode('login');
                setIsAuthModalOpen(true);
              }}
            >
              登录 / 注册
            </button>
          )}
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <AuthModal
        isOpen={isAuthModalOpen}
        initialMode={authMode}
        onClose={() => setIsAuthModalOpen(false)}
      />
    </div>
  );
}

export default MainLayout;
