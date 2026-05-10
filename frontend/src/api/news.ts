import { http } from './http';
import type {
  ApiResponse,
  HotNewsItem,
  NewsDetail,
  NewsListItem,
  PageResult,
  RelatedNewsItem,
} from '../types';

type NewsListParams = {
  page: number;
  size: number;
  keyword?: string;
  categoryId?: number;
};

export async function fetchNewsList(params: NewsListParams) {
  const response = await http.get<ApiResponse<PageResult<NewsListItem>>>('/news', {
    params,
  });
  return response.data;
}

export async function fetchNewsDetail(id: number) {
  const response = await http.get<ApiResponse<NewsDetail>>(`/news/${id}`);
  return response.data;
}

export async function fetchHotNews(limit = 10) {
  const response = await http.get<ApiResponse<HotNewsItem[]>>('/news/hot', {
    params: { limit },
  });
  return response.data;
}

export async function fetchRelatedNews(id: number, limit = 4) {
  const response = await http.get<ApiResponse<RelatedNewsItem[]>>(`/news/${id}/related`, {
    params: { limit },
  });
  return response.data;
}
