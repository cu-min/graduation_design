import { http } from './http';
import type { ApiResponse, CategoryOption, TagOption } from '../types';

export async function fetchCategories() {
  const response = await http.get<ApiResponse<CategoryOption[]>>('/categories');
  return response.data;
}

export async function fetchTags(categoryId?: number) {
  const response = await http.get<ApiResponse<TagOption[]>>('/tags', {
    params: categoryId ? { categoryId } : {},
  });
  return response.data;
}
