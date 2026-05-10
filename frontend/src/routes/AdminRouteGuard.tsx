import type { ReactElement } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../store';

type AdminRouteGuardProps = {
  children: ReactElement;
};

function AdminRouteGuard({ children }: AdminRouteGuardProps) {
  const { currentUser, isAuthenticated, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return <div className="page-card news-state-card">正在校验管理员权限...</div>;
  }

  if (!isAuthenticated || currentUser?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default AdminRouteGuard;
