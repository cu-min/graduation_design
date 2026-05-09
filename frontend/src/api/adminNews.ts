import { http } from './http';
import type {
  AdminNewsDetail,
  AdminNewsListItem,
  ApiResponse,
  NewsFormPayload,
  PageResult,
} from '../types';

type NewsListParams = {
  page: number;
  size: number;
  keyword?: string;
  categoryId?: number;
  status?: number;
};

export async function fetchAdminNews(params: NewsListParams) {
  const response = await http.get<ApiResponse<PageResult<AdminNewsListItem>>>('/admin/news', {
    params,
  });
  return response.data;
}

export async function fetchAdminNewsDetail(id: number) {
  const response = await http.get<ApiResponse<AdminNewsDetail>>(`/admin/news/${id}`);
  return response.data;
}

export async function createAdminNews(payload: NewsFormPayload) {
  const response = await http.post<ApiResponse<null>>('/admin/news', payload);
  return response.data;
}

export async function updateAdminNews(id: number, payload: NewsFormPayload) {
  const response = await http.put<ApiResponse<null>>(`/admin/news/${id}`, payload);
  return response.data;
}

export async function deleteAdminNews(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/admin/news/${id}`);
  return response.data;
}

export async function updateAdminNewsStatus(id: number, status: number) {
  const response = await http.put<ApiResponse<null>>(`/admin/news/${id}/status`, { status });
  return response.data;
}
