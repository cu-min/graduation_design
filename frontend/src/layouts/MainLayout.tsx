import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { fetchProfileInterests } from '../api/profile';
import AuthModal from '../components/AuthModal';
import InterestOnboardingModal from '../components/InterestOnboardingModal';
import { useAuth } from '../store';

function MainLayout() {
  const { currentUser, isAuthenticated, isBootstrapping } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isInterestOnboardingOpen, setIsInterestOnboardingOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const userMenuRef = useRef<HTMLDivElement | null>(null);
  const checkedInterestUserIdRef = useRef<number | null>(null);

  const isAdminUser = currentUser?.role === 'ADMIN';
  const displayName = currentUser?.nickname || currentUser?.username || '未登录用户';

  const routeLabel = useMemo(() => {
    if (location.pathname.startsWith('/news/')) {
      return '新闻详情';
    }
    if (location.pathname.startsWith('/profile')) {
      return '个人中心';
    }
    if (location.pathname.startsWith('/admin')) {
      return '管理后台';
    }
    return '首页';
  }, [location.pathname]);

  useEffect(() => {
    const handleOpenAuthModal = (event: Event) => {
      const customEvent = event as CustomEvent<{ mode?: 'login' | 'register' }>;
      setAuthMode(customEvent.detail?.mode ?? 'login');
      setIsAuthModalOpen(true);
    };

    window.addEventListener('app:open-auth-modal', handleOpenAuthModal);
    return () => window.removeEventListener('app:open-auth-modal', handleOpenAuthModal);
  }, []);

  useEffect(() => {
    setIsUserMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const handleDocumentClick = (event: MouseEvent) => {
      if (!userMenuRef.current?.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleDocumentClick);
    return () => document.removeEventListener('mousedown', handleDocumentClick);
  }, []);

  useEffect(() => {
    if (!isAuthenticated || !currentUser) {
      checkedInterestUserIdRef.current = null;
      setIsInterestOnboardingOpen(false);
      return;
    }

    if (checkedInterestUserIdRef.current === currentUser.id) {
      return;
    }

    checkedInterestUserIdRef.current = currentUser.id;
    let cancelled = false;

    const checkInterestStatus = async () => {
      try {
        const result = await fetchProfileInterests();
        if (!cancelled && result.data.length === 0) {
          setIsInterestOnboardingOpen(true);
        }
      } catch {
        if (!cancelled) {
          checkedInterestUserIdRef.current = null;
        }
      }
    };

    void checkInterestStatus();

    return () => {
      cancelled = true;
    };
  }, [currentUser, isAuthenticated]);

  const handleInterestOnboardingComplete = () => {
    setIsInterestOnboardingOpen(false);
    navigate('/');
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="header-brand-cluster">
          <Link to="/" className="brand brand-link">
            <span className="brand-mark">{getInitials('TrendPulse')}</span>
            <span className="brand-copy">
              <strong>TrendPulse</strong>
              <small>个性化新闻推荐系统</small>
            </span>
          </Link>
          <div className="header-route-meta">
            <span className="header-route-pill">{routeLabel}</span>
          </div>
        </div>

        <div className="auth-panel header-actions">
          {location.pathname !== '/' ? (
            <Link to="/" className="ghost-button header-home-link">
              返回首页
            </Link>
          ) : null}

          {isBootstrapping ? (
            <span className="auth-status">正在恢复登录状态...</span>
          ) : isAuthenticated && currentUser ? (
            <div className="user-menu-wrapper" ref={userMenuRef}>
              <button
                type="button"
                className="user-entry-button"
                onClick={() => setIsUserMenuOpen((current) => !current)}
                aria-haspopup="menu"
                aria-expanded={isUserMenuOpen}
              >
                <span className="user-avatar" aria-hidden="true">
                  {currentUser.avatar ? <img src={currentUser.avatar} alt={displayName} /> : getInitials(displayName)}
                </span>
                <span className="user-entry-copy">
                  <strong>{displayName}</strong>
                  <small>{isAdminUser ? '管理员账号' : '个人中心入口'}</small>
                </span>
                <span className="user-entry-caret" aria-hidden="true">
                  ▾
                </span>
              </button>

              {isUserMenuOpen ? (
                <div className="user-menu-dropdown" role="menu">
                  <Link
                    to="/profile"
                    className="user-menu-link"
                    role="menuitem"
                    onClick={() => setIsUserMenuOpen(false)}
                  >
                    <strong>进入个人中心</strong>
                    <span>资料修改、兴趣设置、历史记录和退出登录都在这里。</span>
                  </Link>
                  {isAdminUser ? (
                    <Link
                      to="/admin"
                      className="user-menu-link admin"
                      role="menuitem"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      <strong>进入管理后台</strong>
                      <span>仅管理员可见，用于新闻运营、采集配置和后台维护。</span>
                    </Link>
                  ) : null}
                </div>
              ) : null}
            </div>
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
      <InterestOnboardingModal
        isOpen={isInterestOnboardingOpen}
        onComplete={handleInterestOnboardingComplete}
      />
    </div>
  );
}

function getInitials(value: string) {
  const normalized = value.trim();
  if (!normalized) {
    return 'TP';
  }

  if (/^[A-Za-z0-9 ]+$/.test(normalized)) {
    const initials = normalized
      .split(/\s+/)
      .slice(0, 2)
      .map((segment) => segment[0]?.toUpperCase() ?? '')
      .join('');
    return initials || normalized.slice(0, 2).toUpperCase();
  }

  return normalized.slice(0, 2).toUpperCase();
}

export default MainLayout;
