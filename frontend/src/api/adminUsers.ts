import { http } from './http';
import type { AdminUserItem, ApiResponse, PageResult } from '../types';

type AdminUserParams = {
  page: number;
  size: number;
  keyword?: string;
  role?: string;
  status?: number;
};

export async function fetchAdminUsers(params: AdminUserParams) {
  const response = await http.get<ApiResponse<PageResult<AdminUserItem>>>('/admin/users', { params });
  return response.data;
}

export async function updateAdminUserStatus(id: number, status: number) {
  const response = await http.put<ApiResponse<null>>(`/admin/users/${id}/status`, { status });
  return response.data;
}
