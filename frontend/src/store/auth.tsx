import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';
import { fetchCurrentUser, logout as logoutRequest } from '../api/auth';
import type { AuthUser } from '../types';
import { clearStoredToken, getStoredToken, setStoredToken } from '../utils/auth';

type AuthContextValue = {
  currentUser: AuthUser | null;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
  signIn: (token: string, user: AuthUser) => void;
  refreshCurrentUser: () => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  const refreshCurrentUser = async () => {
    const token = getStoredToken();
    if (!token) {
      setCurrentUser(null);
      return;
    }

    try {
      const result = await fetchCurrentUser();
      setCurrentUser(result.data);
    } catch {
      clearStoredToken();
      setCurrentUser(null);
    }
  };

  useEffect(() => {
    refreshCurrentUser().finally(() => setIsBootstrapping(false));
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      setCurrentUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const signIn = (token: string, user: AuthUser) => {
    setStoredToken(token);
    setCurrentUser(user);
  };

  const signOut = async () => {
    try {
      if (getStoredToken()) {
        await logoutRequest();
      }
    } finally {
      clearStoredToken();
      setCurrentUser(null);
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      currentUser,
      isAuthenticated: Boolean(currentUser),
      isBootstrapping,
      signIn,
      refreshCurrentUser,
      signOut,
    }),
    [currentUser, isBootstrapping],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
