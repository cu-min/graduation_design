import { http } from './http';
import type {
  ApiResponse,
  AuthUser,
  PageResult,
  PasswordUpdateRequest,
  ProfileCommentItem,
  ProfileNewsItem,
  ProfileSummary,
  ProfileUpdateRequest,
  TagOption,
  UserInterestUpdateRequest,
} from '../types';

type PageParams = {
  page: number;
  size: number;
};

export async function fetchProfileSummary() {
  const response = await http.get<ApiResponse<ProfileSummary>>('/profile/summary');
  return response.data;
}

export async function fetchProfileFavorites(params: PageParams) {
  const response = await http.get<ApiResponse<PageResult<ProfileNewsItem>>>('/profile/favorites', { params });
  return response.data;
}

export async function fetchProfileLikes(params: PageParams) {
  const response = await http.get<ApiResponse<PageResult<ProfileNewsItem>>>('/profile/likes', { params });
  return response.data;
}

export async function fetchProfileHistory(params: PageParams) {
  const response = await http.get<ApiResponse<PageResult<ProfileNewsItem>>>('/profile/history', { params });
  return response.data;
}

export async function fetchProfileComments(params: PageParams) {
  const response = await http.get<ApiResponse<PageResult<ProfileCommentItem>>>('/profile/comments', { params });
  return response.data;
}

export async function fetchProfileInterests() {
  const response = await http.get<ApiResponse<TagOption[]>>('/profile/interests');
  return response.data;
}

export async function updateProfileBasic(payload: ProfileUpdateRequest) {
  const response = await http.put<ApiResponse<AuthUser>>('/profile/basic', payload);
  return response.data;
}

export async function updateProfilePassword(payload: PasswordUpdateRequest) {
  const response = await http.put<ApiResponse<null>>('/profile/password', payload);
  return response.data;
}

export async function updateProfileInterests(payload: UserInterestUpdateRequest) {
  const response = await http.put<ApiResponse<null>>('/profile/interests', payload);
  return response.data;
}
