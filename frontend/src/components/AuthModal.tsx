import { useEffect, useState } from 'react';
import { login, register } from '../api/auth';
import { useAuth } from '../store';
import type { LoginRequest, RegisterRequest } from '../types';
import { getErrorMessage } from '../utils/request';

type AuthMode = 'login' | 'register';

type AuthModalProps = {
  isOpen: boolean;
  initialMode?: AuthMode;
  onClose: () => void;
};

const initialLoginForm: LoginRequest = {
  username: '',
  password: '',
};

const initialRegisterForm: RegisterRequest = {
  username: '',
  password: '',
  email: '',
  nickname: '',
};

function AuthModal({ isOpen, initialMode = 'login', onClose }: AuthModalProps) {
  const { signIn } = useAuth();
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [loginForm, setLoginForm] = useState<LoginRequest>(initialLoginForm);
  const [registerForm, setRegisterForm] = useState<RegisterRequest>(initialRegisterForm);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setMode(initialMode);
    setErrorMessage('');
    setSuccessMessage('');
  }, [initialMode, isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleLoginSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const result = await login(loginForm);
      signIn(result.data.token, result.data.user);
      onClose();
      setLoginForm(initialLoginForm);
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '登录失败，请检查用户名和密码'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegisterSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await register({
        ...registerForm,
        nickname: registerForm.nickname?.trim() || undefined,
      });
      setSuccessMessage('注册成功，请使用新账号登录');
      setMode('login');
      setLoginForm({
        username: registerForm.username,
        password: '',
      });
      setRegisterForm(initialRegisterForm);
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '注册失败，请稍后重试'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="auth-modal"
        role="dialog"
        aria-modal="true"
        aria-label="登录注册弹窗"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="auth-header">
          <div>
            <p className="auth-eyebrow">阶段 2</p>
            <h2>登录 / 注册</h2>
          </div>
          <button type="button" className="ghost-button" onClick={onClose}>
            关闭
          </button>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={mode === 'login' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setMode('login');
              setErrorMessage('');
              setSuccessMessage('');
            }}
          >
            登录
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setMode('register');
              setErrorMessage('');
              setSuccessMessage('');
            }}
          >
            注册
          </button>
        </div>

        {errorMessage ? <p className="auth-feedback error">{errorMessage}</p> : null}
        {successMessage ? <p className="auth-feedback success">{successMessage}</p> : null}

        {mode === 'login' ? (
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <label>
              用户名
              <input
                value={loginForm.username}
                onChange={(event) =>
                  setLoginForm((current) => ({ ...current, username: event.target.value }))
                }
                placeholder="请输入用户名"
                required
              />
            </label>
            <label>
              密码
              <input
                type="password"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm((current) => ({ ...current, password: event.target.value }))
                }
                placeholder="请输入密码"
                required
              />
            </label>
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? '登录中...' : '登录'}
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleRegisterSubmit}>
            <label>
              用户名
              <input
                value={registerForm.username}
                onChange={(event) =>
                  setRegisterForm((current) => ({ ...current, username: event.target.value }))
                }
                placeholder="4-50 位用户名"
                required
              />
            </label>
            <label>
              邮箱
              <input
                type="email"
                value={registerForm.email}
                onChange={(event) =>
                  setRegisterForm((current) => ({ ...current, email: event.target.value }))
                }
                placeholder="请输入邮箱"
                required
              />
            </label>
            <label>
              昵称
              <input
                value={registerForm.nickname}
                onChange={(event) =>
                  setRegisterForm((current) => ({ ...current, nickname: event.target.value }))
                }
                placeholder="可选，不填则默认使用用户名"
              />
            </label>
            <label>
              密码
              <input
                type="password"
                value={registerForm.password}
                onChange={(event) =>
                  setRegisterForm((current) => ({ ...current, password: event.target.value }))
                }
                placeholder="6-50 位密码"
                required
              />
            </label>
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? '注册中...' : '注册'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default AuthModal;
