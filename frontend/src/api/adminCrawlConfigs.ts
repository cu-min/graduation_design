import { http } from './http';
import type {
  AdminCrawlConfigDetail,
  AdminCrawlConfigItem,
  ApiResponse,
  CrawlConfigFormPayload,
  CrawlRunResult,
} from '../types';

export async function fetchAdminCrawlConfigs() {
  const response = await http.get<ApiResponse<AdminCrawlConfigItem[]>>('/admin/crawl-configs');
  return response.data;
}

export async function fetchAdminCrawlConfigDetail(id: number) {
  const response = await http.get<ApiResponse<AdminCrawlConfigDetail>>(`/admin/crawl-configs/${id}`);
  return response.data;
}

export async function createAdminCrawlConfig(payload: CrawlConfigFormPayload) {
  const response = await http.post<ApiResponse<null>>('/admin/crawl-configs', payload);
  return response.data;
}

export async function updateAdminCrawlConfig(id: number, payload: CrawlConfigFormPayload) {
  const response = await http.put<ApiResponse<null>>(`/admin/crawl-configs/${id}`, payload);
  return response.data;
}

export async function deleteAdminCrawlConfig(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/admin/crawl-configs/${id}`);
  return response.data;
}

export async function updateAdminCrawlConfigStatus(id: number, enabled: number) {
  const response = await http.put<ApiResponse<null>>(`/admin/crawl-configs/${id}/status`, { enabled });
  return response.data;
}

export async function runAdminCrawlConfig(id: number) {
  const response = await http.post<ApiResponse<CrawlRunResult>>(`/admin/crawl-configs/${id}/run`);
  return response.data;
}
