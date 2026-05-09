import { http } from './http';
import type { ApiResponse, CommentItem } from '../types';

export async function fetchNewsComments(newsId: number) {
  const response = await http.get<ApiResponse<CommentItem[]>>(`/news/${newsId}/comments`);
  return response.data;
}

export async function createNewsComment(newsId: number, content: string) {
  const response = await http.post<ApiResponse<null>>(`/news/${newsId}/comments`, { content });
  return response.data;
}

export async function replyComment(commentId: number, content: string) {
  const response = await http.post<ApiResponse<null>>(`/comments/${commentId}/replies`, { content });
  return response.data;
}

export async function deleteComment(commentId: number) {
  const response = await http.delete<ApiResponse<null>>(`/comments/${commentId}`);
  return response.data;
}
