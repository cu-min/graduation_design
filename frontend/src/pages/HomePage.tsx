import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCategories } from '../api/metadata';
import { fetchHotNews, fetchNewsList } from '../api/news';
import { fetchRecommendNews } from '../api/recommend';
import { useAuth } from '../store';
import type {
  CategoryOption,
  HotNewsItem,
  NewsListItem,
  PageResult,
  RecommendNewsItem,
} from '../types';
import { getErrorMessage } from '../utils/request';

type FilterState = {
  keyword: string;
  categoryId: string;
};

type FeedMode = 'recommend' | 'latest';

const initialFilters: FilterState = {
  keyword: '',
  categoryId: '',
};

function HomePage() {
  const { isAuthenticated } = useAuth();
  const [feedMode, setFeedMode] = useState<FeedMode>('recommend');
  const [filters, setFilters] = useState<FilterState>(initialFilters);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [pageData, setPageData] = useState<PageResult<NewsListItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 6,
  });
  const [recommendPageData, setRecommendPageData] = useState<PageResult<RecommendNewsItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 6,
  });
  const [hotNews, setHotNews] = useState<HotNewsItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const hasActiveFilters = Boolean(filters.keyword.trim() || filters.categoryId);
  const isShowingRecommendFeed = feedMode === 'recommend' && !hasActiveFilters;
  const visiblePage = isShowingRecommendFeed ? recommendPageData : pageData;

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(visiblePage.total / visiblePage.size)),
    [visiblePage.size, visiblePage.total],
  );

  const loadLatestNews = async (
    page = 1,
    size = pageData.size,
    nextFilters: FilterState = filters,
  ) => {
    const result = await fetchNewsList({
      page,
      size,
      keyword: nextFilters.keyword.trim() || undefined,
      categoryId: nextFilters.categoryId ? Number(nextFilters.categoryId) : undefined,
    });
    setPageData(result.data);
  };

  const loadRecommendFeed = async (page = 1, size = recommendPageData.size) => {
    const result = await fetchRecommendNews({ page, size });
    setRecommendPageData(result.data);
  };

  const loadCurrentFeed = async (
    page = 1,
    mode = feedMode,
    nextFilters: FilterState = filters,
  ) => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const shouldUseRecommendFeed = mode === 'recommend' && !nextFilters.keyword.trim() && !nextFilters.categoryId;
      if (shouldUseRecommendFeed) {
        await loadRecommendFeed(page, recommendPageData.size);
      } else {
        await loadLatestNews(page, pageData.size, nextFilters);
      }
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '首页新闻数据加载失败，请稍后重试'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const loadInitialData = async () => {
      setIsLoading(true);
      setErrorMessage('');

      try {
        const [categoryResult, hotResult] = await Promise.all([fetchCategories(), fetchHotNews(8)]);
        setCategories(categoryResult.data);
        setHotNews(hotResult.data);

        if (isShowingRecommendFeed) {
          await loadRecommendFeed(1, recommendPageData.size);
        } else {
          await loadLatestNews(1, pageData.size, filters);
        }
      } catch (error) {
        setErrorMessage(getErrorMessage(error, '首页新闻数据加载失败，请稍后重试'));
      } finally {
        setIsLoading(false);
      }
    };

    void loadInitialData();
  }, [isAuthenticated]);

  useEffect(() => {
    const handleInterestsUpdated = () => {
      const nextFilters = { ...initialFilters };
      setFeedMode('recommend');
      setFilters(nextFilters);
      void loadCurrentFeed(1, 'recommend', nextFilters);
    };

    window.addEventListener('app:interests-updated', handleInterestsUpdated);
    return () => window.removeEventListener('app:interests-updated', handleInterestsUpdated);
  }, [feedMode, filters]);

  const handleSearchSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void loadCurrentFeed(1);
  };

  return (
    <section className="news-home-page">
      <div className="home-hero page-card">
        <h1>发现更适合你的新闻流</h1>
        <p className="page-description">
          登录后默认展示“为你推荐”，系统会结合兴趣标签和浏览行为持续调整结果，同时保留“最新资讯”便于查看完整新闻流。
        </p>

        <div className="feed-mode-tabs">
          <button
            type="button"
            className={feedMode === 'recommend' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setFeedMode('recommend');
              void loadCurrentFeed(1, 'recommend');
            }}
          >
            {isAuthenticated ? '为你推荐' : '热门推荐'}
          </button>
          <button
            type="button"
            className={feedMode === 'latest' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setFeedMode('latest');
              void loadCurrentFeed(1, 'latest');
            }}
          >
            最新资讯
          </button>
        </div>

        <form className="news-search-bar" onSubmit={handleSearchSubmit}>
          <input
            value={filters.keyword}
            onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value }))}
            placeholder="搜索标题或摘要"
          />
          <select
            value={filters.categoryId}
            onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))}
          >
            <option value="">全部分类</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <button type="submit" className="primary-button">
            搜索
          </button>
        </form>
      </div>

      {errorMessage ? <p className="auth-feedback error">{errorMessage}</p> : null}

      <div className="home-layout">
        <div className="news-feed">
          <div className="section-heading">
            <div>
              <p className="page-eyebrow">{isShowingRecommendFeed ? '推荐结果' : '实时内容'}</p>
              <h2>{isShowingRecommendFeed ? (isAuthenticated ? '为你推荐' : '热门推荐') : '最新资讯'}</h2>
            </div>
            <span className="section-meta">
              第 {visiblePage.page} 页 / 共 {totalPages} 页 · {visiblePage.total} 条
            </span>
          </div>

          {isLoading ? (
            <div className="page-card news-state-card">正在加载新闻列表...</div>
          ) : visiblePage.records.length === 0 ? (
            <div className="page-card news-state-card">
              <h3>{isShowingRecommendFeed ? '暂时还没有可展示的推荐内容' : '当前条件下暂无新闻'}</h3>
              <p>
                {isShowingRecommendFeed
                  ? isAuthenticated
                    ? '可以先完成兴趣选择，或者继续浏览几篇新闻，系统会逐步生成更贴近你的推荐结果。'
                    : '当前热门推荐为空，你可以切换到“最新资讯”查看普通新闻流。'
                  : '请尝试更换关键词、清空筛选条件，或稍后再回来看看。'}
              </p>
            </div>
          ) : isShowingRecommendFeed ? (
            <div className="news-card-list">
              {recommendPageData.records.map((item) => (
                <article key={item.id} className="news-card">
                  <Link to={`/news/${item.id}`} className="news-card-main">
                    <NewsCover imageUrl={item.coverImage} title={item.title} />
                    <div className="news-card-body">
                      <div className="news-card-topline">
                        <span className="news-category-chip">{item.categoryName}</span>
                        <span className="news-time">{formatDisplayDate(item.publishTime)}</span>
                      </div>
                      <h3>{item.title}</h3>
                      <p>{item.summary}</p>
                      <div className="news-tag-list">
                        {item.tagNames.map((tagName) => (
                          <span key={tagName} className="news-tag">
                            {tagName}
                          </span>
                        ))}
                      </div>
                      <div className="recommend-card-meta">
                        <span className="recommend-reason-chip">{item.recommendReason}</span>
                        <span className="recommend-score-chip">推荐指数 {formatRecommendScore(item.recommendScore)}</span>
                      </div>
                    </div>
                  </Link>
                  <div className="news-card-footer">
                    <span>{item.sourceName}</span>
                    <div className="news-metrics">
                      <span>热度 {item.heatScore}</span>
                      <span>浏览 {item.viewCount}</span>
                      <span>点赞 {item.likeCount}</span>
                      <span>收藏 {item.favoriteCount}</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="news-card-list">
              {pageData.records.map((item) => (
                <article key={item.id} className="news-card">
                  <Link to={`/news/${item.id}`} className="news-card-main">
                    <NewsCover imageUrl={item.coverImage} title={item.title} />
                    <div className="news-card-body">
                      <div className="news-card-topline">
                        <span className="news-category-chip">{item.categoryName}</span>
                        <span className="news-time">{formatDisplayDate(item.publishTime)}</span>
                      </div>
                      <h3>{item.title}</h3>
                      <p>{item.summary}</p>
                      <div className="news-tag-list">
                        {item.tagNames.map((tagName) => (
                          <span key={tagName} className="news-tag">
                            {tagName}
                          </span>
                        ))}
                      </div>
                    </div>
                  </Link>
                  <div className="news-card-footer">
                    <span>{item.sourceName}</span>
                    <div className="news-metrics">
                      <span>热度 {item.heatScore}</span>
                      <span>浏览 {item.viewCount}</span>
                      <span>点赞 {item.likeCount}</span>
                      <span>收藏 {item.favoriteCount}</span>
                      <span>评论 {item.commentCount}</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}

          <div className="pagination-bar home-pagination">
            <button
              type="button"
              className="ghost-button"
              disabled={visiblePage.page <= 1 || isLoading}
              onClick={() => void loadCurrentFeed(visiblePage.page - 1)}
            >
              上一页
            </button>
            <span>
              第 {visiblePage.page} / {totalPages} 页
            </span>
            <button
              type="button"
              className="ghost-button"
              disabled={visiblePage.page >= totalPages || isLoading}
              onClick={() => void loadCurrentFeed(visiblePage.page + 1)}
            >
              下一页
            </button>
          </div>
        </div>

        <aside className="hot-news-panel page-card">
          <div className="section-heading compact">
            <div>
              <p className="page-eyebrow">热度榜</p>
              <h2>热门新闻</h2>
            </div>
          </div>
          {hotNews.length === 0 ? (
            <div className="news-state-card compact-empty-state">暂时还没有热门新闻数据。</div>
          ) : (
            <div className="hot-news-list">
              {hotNews.map((item, index) => (
                <Link key={item.id} to={`/news/${item.id}`} className="hot-news-item">
                  <span className="hot-news-rank">{String(index + 1).padStart(2, '0')}</span>
                  <div className="hot-news-content">
                    <strong>{item.title}</strong>
                    <p>
                      {item.categoryName} · {item.sourceName}
                    </p>
                    <span>
                      热度 {item.heatScore} · {formatDisplayDate(item.publishTime)}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </aside>
      </div>
    </section>
  );
}

function NewsCover({ imageUrl, title }: { imageUrl: string; title: string }) {
  const [hasError, setHasError] = useState(false);

  if (!imageUrl || hasError) {
    return (
      <div className="news-cover news-cover-empty">
        <span>{title.slice(0, 18)}</span>
      </div>
    );
  }

  return (
    <div className="news-cover">
      <img src={imageUrl} alt={title} onError={() => setHasError(true)} />
    </div>
  );
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

function formatRecommendScore(value: number) {
  return Number.isFinite(value) ? value.toFixed(1) : '0.0';
}

export default HomePage;
