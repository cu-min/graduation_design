import { http } from './http';
import type { ApiResponse, PageResult, RecommendNewsItem } from '../types';

type RecommendParams = {
  page: number;
  size: number;
};

export async function fetchRecommendNews(params: RecommendParams) {
  const response = await http.get<ApiResponse<PageResult<RecommendNewsItem>>>('/recommend/news', {
    params,
  });
  return response.data;
}
