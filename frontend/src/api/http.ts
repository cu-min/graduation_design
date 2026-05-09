import axios from 'axios';
import { getApiBaseUrl } from '../utils/env';
import { clearStoredToken, getStoredToken } from '../utils/auth';
import { openAuthDialog } from '../utils/authDialog';
import type { ApiResponse } from '../types';

export const http = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredToken();
      window.dispatchEvent(new Event('auth:unauthorized'));
      openAuthDialog('login');
    }
    return Promise.reject(error);
  },
);

export async function getHealth() {
  const response = await http.get<ApiResponse<{ status: string; service: string }>>('/health');
  return response.data;
}
