import { http } from './http';
import type { AdminCategoryPayload, ApiResponse, CategoryOption } from '../types';

export async function fetchAdminCategories() {
  const response = await http.get<ApiResponse<CategoryOption[]>>('/admin/categories');
  return response.data;
}

export async function createAdminCategory(payload: AdminCategoryPayload) {
  const response = await http.post<ApiResponse<null>>('/admin/categories', payload);
  return response.data;
}

export async function updateAdminCategory(id: number, payload: AdminCategoryPayload) {
  const response = await http.put<ApiResponse<null>>(`/admin/categories/${id}`, payload);
  return response.data;
}

export async function deleteAdminCategory(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/admin/categories/${id}`);
  return response.data;
}

export async function updateAdminCategoryStatus(id: number, status: number) {
  const response = await http.put<ApiResponse<null>>(`/admin/categories/${id}/status`, { status });
  return response.data;
}
