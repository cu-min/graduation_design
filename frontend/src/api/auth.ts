import { http } from './http';
import type { ApiResponse, AuthUser, LoginRequest, LoginResponse, RegisterRequest } from '../types';

export async function register(payload: RegisterRequest) {
  const response = await http.post<ApiResponse<AuthUser>>('/auth/register', payload);
  return response.data;
}

export async function login(payload: LoginRequest) {
  const response = await http.post<ApiResponse<LoginResponse>>('/auth/login', payload);
  return response.data;
}

export async function fetchCurrentUser() {
  const response = await http.get<ApiResponse<AuthUser>>('/auth/me');
  return response.data;
}

export async function logout() {
  const response = await http.post<ApiResponse<null>>('/auth/logout');
  return response.data;
}
