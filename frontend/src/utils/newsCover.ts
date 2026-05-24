const CATEGORY_COVER_MAP: Array<[string, string]> = [
  ['前沿科技', '/news-covers/frontier-tech.svg'],
  ['成长学习', '/news-covers/growth-learning.svg'],
  ['职业机会', '/news-covers/career-opportunity.svg'],
  ['数字生活', '/news-covers/digital-life.svg'],
  ['热点趋势', '/news-covers/hot-trend.svg'],
];

export function getNewsCoverFallback(categoryName?: string) {
  const normalizedCategoryName = categoryName?.trim() ?? '';
  const matchedCover = CATEGORY_COVER_MAP.find(([name]) => normalizedCategoryName.includes(name));
  return matchedCover?.[1] ?? '/news-covers/hot-trend.svg';
}

export function getDisplayNewsCover(imageUrl: string | null | undefined, categoryName?: string, hasError = false) {
  const normalizedImageUrl = imageUrl?.trim();
  const hasUsableImageUrl = normalizedImageUrl
    && normalizedImageUrl !== 'null'
    && normalizedImageUrl !== 'undefined';
  if (!hasError && hasUsableImageUrl) {
    return normalizedImageUrl;
  }
  return getNewsCoverFallback(categoryName);
}
