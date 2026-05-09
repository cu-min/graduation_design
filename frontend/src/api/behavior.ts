import { http } from './http';
import type { ApiResponse, NewsActionStatus } from '../types';

export async function likeNews(id: number) {
  const response = await http.post<ApiResponse<NewsActionStatus>>(`/news/${id}/like`);
  return response.data;
}

export async function unlikeNews(id: number) {
  const response = await http.delete<ApiResponse<NewsActionStatus>>(`/news/${id}/like`);
  return response.data;
}

export async function favoriteNews(id: number) {
  const response = await http.post<ApiResponse<NewsActionStatus>>(`/news/${id}/favorite`);
  return response.data;
}

export async function unfavoriteNews(id: number) {
  const response = await http.delete<ApiResponse<NewsActionStatus>>(`/news/${id}/favorite`);
  return response.data;
}

export async function dislikeNews(id: number) {
  const response = await http.post<ApiResponse<NewsActionStatus>>(`/news/${id}/dislike`);
  return response.data;
}

export async function shareNews(id: number) {
  const response = await http.post<ApiResponse<NewsActionStatus>>(`/news/${id}/share`);
  return response.data;
}
