import { http } from './http';
import type { AdminDashboardSummary, ApiResponse } from '../types';

export async function fetchAdminDashboardSummary() {
  const response = await http.get<ApiResponse<AdminDashboardSummary>>('/admin/dashboard/summary');
  return response.data;
}
