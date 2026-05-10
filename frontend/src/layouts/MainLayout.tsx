import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import AuthModal from '../components/AuthModal';
import { useAuth } from '../store';

function MainLayout() {
  const { currentUser, isAuthenticated, isBootstrapping } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const location = useLocation();
  const userMenuRef = useRef<HTMLDivElement | null>(null);

  const isAdminUser = currentUser?.role === 'ADMIN';
  const displayName = currentUser?.nickname || currentUser?.username || '未登录用户';

  const routeMeta = useMemo(() => {
    if (location.pathname.startsWith('/news/')) {
      return {
        label: '新闻详情',
        description: '点击新闻卡片后直接进入详情页，阅读、点赞、收藏和评论都在这里完成。',
      };
    }
    if (location.pathname.startsWith('/profile')) {
      return {
        label: '个人中心',
        description: '资料修改、账号设置、兴趣标签、历史记录和退出登录统一放在个人中心。',
      };
    }
    if (location.pathname.startsWith('/admin')) {
      return {
        label: '管理后台',
        description: '仅管理员可见，用于内容运营、采集配置与后台管理。',
      };
    }
    return {
      label: '首页',
      description: '进入系统默认看到首页推荐流，不再把详情页和个人中心当成主导航并列展示。',
    };
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
            <span className="header-route-pill">{routeMeta.label}</span>
            <p>{routeMeta.description}</p>
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
                  {currentUser.avatar ? (
                    <img src={currentUser.avatar} alt={displayName} />
                  ) : (
                    getInitials(displayName)
                  )}
                </span>
                <span className="user-entry-copy">
                  <strong>{displayName}</strong>
                  <small>{isAdminUser ? '管理员账户' : '个人中心入口'}</small>
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
                      <span>仅管理员可见，用于新闻运营、采集管理和后台维护。</span>
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
