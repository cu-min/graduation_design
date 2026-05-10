import { http } from './http';
import type { AdminTagPayload, ApiResponse, TagOption } from '../types';

export async function fetchAdminTags(categoryId?: number) {
  const response = await http.get<ApiResponse<TagOption[]>>('/admin/tags', {
    params: categoryId ? { categoryId } : {},
  });
  return response.data;
}

export async function createAdminTag(payload: AdminTagPayload) {
  const response = await http.post<ApiResponse<null>>('/admin/tags', payload);
  return response.data;
}

export async function updateAdminTag(id: number, payload: AdminTagPayload) {
  const response = await http.put<ApiResponse<null>>(`/admin/tags/${id}`, payload);
  return response.data;
}

export async function deleteAdminTag(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/admin/tags/${id}`);
  return response.data;
}

export async function updateAdminTagStatus(id: number, status: number) {
  const response = await http.put<ApiResponse<null>>(`/admin/tags/${id}/status`, { status });
  return response.data;
}
