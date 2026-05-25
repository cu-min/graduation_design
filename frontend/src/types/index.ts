export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
}

export interface AuthUser {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string | null;
  avatar: string | null;
  role: 'USER' | 'ADMIN';
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  nickname?: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface CategoryOption {
  id: number;
  name: string;
  code: string;
  description: string;
  sortOrder: number;
  status: number;
}

export interface TagOption {
  id: number;
  categoryId: number;
  name: string;
  code: string;
  sortOrder: number;
  status: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface AdminNewsListItem {
  id: number;
  title: string;
  summary: string;
  sourceName: string;
  coverImage: string;
  categoryId: number;
  categoryName: string;
  tagIds: number[];
  tagNames: string[];
  publishTime: string;
  status: number;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
}

export interface AdminNewsDetail {
  id: number;
  title: string;
  summary: string;
  content: string;
  sourceName: string;
  sourceUrl: string;
  coverImage: string;
  categoryId: number;
  categoryName: string;
  tagIds: number[];
  tagNames: string[];
  publishTime: string;
  crawlTime: string;
  status: number;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
}

export interface NewsFormPayload {
  title: string;
  summary: string;
  content: string;
  sourceName: string;
  sourceUrl: string;
  coverImage: string;
  categoryId: number;
  tagIds: number[];
  publishTime: string;
  heatScore: number;
  status: number;
}

export interface NewsListItem {
  id: number;
  title: string;
  summary: string;
  coverImage: string;
  sourceName: string;
  categoryId: number;
  categoryName: string;
  tagNames: string[];
  publishTime: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
}

export interface RelatedNewsItem extends NewsListItem {}

export interface NewsDetail {
  id: number;
  title: string;
  summary: string;
  content: string;
  coverImage: string;
  sourceName: string;
  sourceUrl: string;
  categoryId: number;
  categoryName: string;
  tagNames: string[];
  publishTime: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
  liked: boolean;
  favorited: boolean;
  disliked: boolean;
}

export interface HotNewsItem {
  id: number;
  title: string;
  coverImage: string;
  sourceName: string;
  categoryName: string;
  publishTime: string;
  heatScore: number;
}

export interface NewsActionStatus {
  liked: boolean;
  favorited: boolean;
  disliked: boolean;
  likeCount: number;
  favoriteCount: number;
}

export interface CommentReply {
  id: number;
  newsId: number;
  userId: number;
  nickname: string;
  content: string;
  createdAt: string;
  canDelete: boolean;
}

export interface CommentItem {
  id: number;
  newsId: number;
  userId: number;
  nickname: string;
  content: string;
  createdAt: string;
  canDelete: boolean;
  replies: CommentReply[];
}

export interface ProfileSummary {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string | null;
  avatar: string | null;
  role: 'USER' | 'ADMIN';
  favoriteCount: number;
  likeCount: number;
  commentCount: number;
  historyCount: number;
}

export interface ProfileUpdateRequest {
  nickname: string;
  email: string;
  phone?: string;
  avatar?: string;
}

export interface PasswordUpdateRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ProfileNewsItem {
  newsId: number;
  title: string;
  summary: string;
  coverImage: string;
  sourceName: string;
  categoryName: string;
  publishTime: string;
  behaviorTime: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
}

export interface ProfileCommentItem {
  id: number;
  newsId: number;
  newsTitle: string;
  parentId: number | null;
  content: string;
  createdAt: string;
}

export interface UserInterestUpdateRequest {
  tagIds: number[];
}

export interface RecommendNewsItem {
  id: number;
  title: string;
  summary: string;
  coverImage: string;
  sourceName: string;
  categoryId: number;
  categoryName: string;
  tagNames: string[];
  publishTime: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
  recommendScore: number;
  recommendReason: string;
}

export interface AdminCrawlConfigItem {
  id: number;
  sourceName: string;
  sourceUrl: string;
  sourceType: string;
  categoryId: number;
  categoryName: string | null;
  enabled: number;
  crawlInterval: number;
  lastCrawlTime: string | null;
  lastCrawlCount: number;
  lastStatus: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCrawlConfigDetail extends AdminCrawlConfigItem {}

export interface CrawlConfigFormPayload {
  sourceName: string;
  sourceUrl: string;
  sourceType: string;
  categoryId: number;
  enabled: number;
  crawlInterval: number;
}

export interface CrawlRunResult {
  crawlConfigId: number;
  sourceName: string;
  insertedCount: number;
  duplicateCount: number;
  lastStatus: string;
  message: string;
}

export interface AdminDashboardSummary {
  newsTotal: number;
  onlineNewsTotal: number;
  offlineNewsTotal: number;
  userTotal: number;
  commentTotal: number;
  crawlConfigTotal: number;
  enabledCrawlConfigTotal: number;
  todayCrawledNewsTotal: number;
  todayCommentTotal: number;
  viewBehaviorTotal: number;
  likeBehaviorTotal: number;
  favoriteBehaviorTotal: number;
  dislikeBehaviorTotal: number;
  shareBehaviorTotal: number;
  shareCount: number;
  latestCrawlTime: string | null;
  latestCrawlStatus: string | null;
  hotNews: AdminHotNewsItem[];
  categoryStats: AdminCategoryStatItem[];
}

export interface AdminHotNewsItem {
  id: number;
  title: string;
  heatScore: number;
  likeCount: number;
  favoriteCount: number;
}

export interface AdminCategoryStatItem {
  categoryId: number;
  categoryName: string;
  newsCount: number;
}

export interface AdminCategoryPayload {
  name: string;
  code: string;
  description: string;
  sortOrder: number;
  status: number;
}

export interface AdminTagPayload {
  categoryId: number;
  name: string;
  code: string;
  sortOrder: number;
  status: number;
}

export interface AdminUserItem {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string | null;
  avatar: string | null;
  role: 'USER' | 'ADMIN';
  status: number;
  createdAt: string;
  updatedAt: string;
}
