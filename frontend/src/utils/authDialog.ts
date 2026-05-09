export function openAuthDialog(mode: 'login' | 'register' = 'login') {
  window.dispatchEvent(new CustomEvent('app:open-auth-modal', { detail: { mode } }));
}
