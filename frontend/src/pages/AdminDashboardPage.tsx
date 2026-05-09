import { useEffect, useMemo, useState } from 'react';
import { fetchAdminDashboardSummary } from '../api/adminDashboard';
import {
  createAdminCrawlConfig,
  deleteAdminCrawlConfig,
  fetchAdminCrawlConfigDetail,
  fetchAdminCrawlConfigs,
  runAdminCrawlConfig,
  updateAdminCrawlConfig,
  updateAdminCrawlConfigStatus,
} from '../api/adminCrawlConfigs';
import {
  createAdminNews,
  deleteAdminNews,
  fetchAdminNews,
  fetchAdminNewsDetail,
  updateAdminNews,
  updateAdminNewsStatus,
} from '../api/adminNews';
import { fetchCategories, fetchTags } from '../api/metadata';
import PagePlaceholder from '../components/PagePlaceholder';
import { useAuth } from '../store';
import type {
  AdminCrawlConfigItem,
  AdminDashboardSummary,
  AdminNewsListItem,
  CategoryOption,
  CrawlConfigFormPayload,
  NewsFormPayload,
  PageResult,
  TagOption,
} from '../types';
import { getErrorMessage } from '../utils/request';

type AdminSection = 'news' | 'crawl';

type NewsFilterState = {
  keyword: string;
  categoryId: string;
  status: string;
};

const initialNewsFilters: NewsFilterState = {
  keyword: '',
  categoryId: '',
  status: '',
};

const initialNewsFormState: NewsFormPayload = {
  title: '',
  summary: '',
  content: '',
  sourceName: '',
  sourceUrl: '',
  coverImage: '',
  categoryId: 1,
  tagIds: [],
  publishTime: '',
  heatScore: 60,
  status: 1,
};

const initialCrawlFormState: CrawlConfigFormPayload = {
  sourceName: '',
  sourceUrl: '',
  sourceType: 'RSS',
  categoryId: 1,
  enabled: 1,
  crawlInterval: 30,
};

const emptySummary: AdminDashboardSummary = {
  newsTotal: 0,
  onlineNewsTotal: 0,
  offlineNewsTotal: 0,
  userTotal: 0,
  commentTotal: 0,
  crawlConfigTotal: 0,
  enabledCrawlConfigTotal: 0,
  todayCrawledNewsTotal: 0,
  todayCommentTotal: 0,
  viewBehaviorTotal: 0,
  likeBehaviorTotal: 0,
  favoriteBehaviorTotal: 0,
  dislikeBehaviorTotal: 0,
  shareBehaviorTotal: 0,
  latestCrawlTime: null,
  latestCrawlStatus: null,
  hotNews: [],
  categoryStats: [],
};

function AdminDashboardPage() {
  const { currentUser, isBootstrapping } = useAuth();
  const isAdmin = currentUser?.role === 'ADMIN';

  const [activeSection, setActiveSection] = useState<AdminSection>('news');
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [tags, setTags] = useState<TagOption[]>([]);
  const [summary, setSummary] = useState<AdminDashboardSummary>(emptySummary);

  const [newsFilters, setNewsFilters] = useState<NewsFilterState>(initialNewsFilters);
  const [newsPageData, setNewsPageData] = useState<PageResult<AdminNewsListItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 8,
  });
  const [crawlConfigs, setCrawlConfigs] = useState<AdminCrawlConfigItem[]>([]);

  const [isNewsLoading, setIsNewsLoading] = useState(false);
  const [isCrawlLoading, setIsCrawlLoading] = useState(false);
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);
  const [pageError, setPageError] = useState('');
  const [pageSuccess, setPageSuccess] = useState('');

  const [isNewsFormOpen, setIsNewsFormOpen] = useState(false);
  const [isNewsSubmitting, setIsNewsSubmitting] = useState(false);
  const [editingNewsId, setEditingNewsId] = useState<number | null>(null);
  const [newsFormState, setNewsFormState] = useState<NewsFormPayload>(initialNewsFormState);

  const [isCrawlFormOpen, setIsCrawlFormOpen] = useState(false);
  const [isCrawlSubmitting, setIsCrawlSubmitting] = useState(false);
  const [editingCrawlId, setEditingCrawlId] = useState<number | null>(null);
  const [runningCrawlId, setRunningCrawlId] = useState<number | null>(null);
  const [crawlFormState, setCrawlFormState] = useState<CrawlConfigFormPayload>(initialCrawlFormState);

  const visibleNewsTags = useMemo(
    () => tags.filter((tag) => tag.categoryId === Number(newsFormState.categoryId)),
    [newsFormState.categoryId, tags],
  );

  const summaryCards = useMemo(
    () => [
      { label: '新闻总数', value: summary.newsTotal, tone: 'blue' },
      { label: '已上架 / 已下架', value: `${summary.onlineNewsTotal} / ${summary.offlineNewsTotal}`, tone: 'green' },
      { label: '用户数', value: summary.userTotal, tone: 'gold' },
      { label: '评论数', value: summary.commentTotal, tone: 'blue' },
      { label: '采集源数量', value: summary.crawlConfigTotal, tone: 'green' },
      { label: '启用采集源', value: summary.enabledCrawlConfigTotal, tone: 'gold' },
      { label: '今日采集新闻', value: summary.todayCrawledNewsTotal, tone: 'blue' },
      { label: '今日评论数', value: summary.todayCommentTotal, tone: 'green' },
      { label: '最近采集时间', value: summary.latestCrawlTime ? formatDisplayDate(summary.latestCrawlTime) : '-', tone: 'gold' },
      { label: '最近采集状态', value: summary.latestCrawlStatus ?? '-', tone: summary.latestCrawlStatus === 'FAILED' ? 'red' : 'blue' },
    ],
    [summary],
  );

  const behaviorCards = useMemo(
    () => [
      { label: '浏览行为', value: summary.viewBehaviorTotal },
      { label: '点赞行为', value: summary.likeBehaviorTotal },
      { label: '收藏行为', value: summary.favoriteBehaviorTotal },
      { label: '不感兴趣行为', value: summary.dislikeBehaviorTotal },
      { label: '分享行为', value: summary.shareBehaviorTotal },
    ],
    [summary],
  );

  const loadNews = async (page = newsPageData.page, size = newsPageData.size) => {
    setIsNewsLoading(true);
    try {
      const result = await fetchAdminNews({
        page,
        size,
        keyword: newsFilters.keyword.trim() || undefined,
        categoryId: newsFilters.categoryId ? Number(newsFilters.categoryId) : undefined,
        status: newsFilters.status ? Number(newsFilters.status) : undefined,
      });
      setNewsPageData(result.data);
    } finally {
      setIsNewsLoading(false);
    }
  };

  const loadCrawlConfigs = async () => {
    setIsCrawlLoading(true);
    try {
      const result = await fetchAdminCrawlConfigs();
      setCrawlConfigs(result.data);
    } finally {
      setIsCrawlLoading(false);
    }
  };

  const loadSummary = async () => {
    setIsSummaryLoading(true);
    try {
      const result = await fetchAdminDashboardSummary();
      setSummary(result.data);
    } finally {
      setIsSummaryLoading(false);
    }
  };

  useEffect(() => {
    if (!isAdmin) {
      return;
    }

    const loadInitialData = async () => {
      setPageError('');
      try {
        const [categoryResult, tagResult] = await Promise.all([fetchCategories(), fetchTags()]);
        setCategories(categoryResult.data);
        setTags(tagResult.data);
        setNewsFormState((current) => ({
          ...current,
          categoryId: categoryResult.data[0]?.id ?? current.categoryId,
        }));
        setCrawlFormState((current) => ({
          ...current,
          categoryId: categoryResult.data[0]?.id ?? current.categoryId,
        }));

        await Promise.all([loadSummary(), loadNews(1, newsPageData.size), loadCrawlConfigs()]);
      } catch (error) {
        setPageError(getErrorMessage(error, '初始化后台管理数据失败'));
      }
    };

    void loadInitialData();
  }, [isAdmin]);

  if (isBootstrapping) {
    return (
      <PagePlaceholder
        eyebrow="阶段 9"
        title="正在加载管理员信息"
        description="正在恢复登录状态，并校验当前用户是否具备后台管理权限。"
      />
    );
  }

  if (!isAdmin) {
    return (
      <PagePlaceholder
        eyebrow="阶段 9"
        title="无权限访问管理后台"
        description="当前页面仅允许 ADMIN 角色访问，请使用管理员账号登录后再进入 /admin。"
      />
    );
  }

  const refreshAdminData = async () => {
    await Promise.all([loadSummary(), loadCrawlConfigs(), loadNews(newsPageData.page, newsPageData.size)]);
  };

  const resetNewsForm = () => {
    setEditingNewsId(null);
    setNewsFormState({
      ...initialNewsFormState,
      categoryId: categories[0]?.id ?? 1,
    });
  };

  const resetCrawlForm = () => {
    setEditingCrawlId(null);
    setCrawlFormState({
      ...initialCrawlFormState,
      categoryId: categories[0]?.id ?? 1,
    });
  };

  const openCreateNewsForm = () => {
    resetNewsForm();
    setIsNewsFormOpen(true);
  };

  const openCreateCrawlForm = () => {
    resetCrawlForm();
    setIsCrawlFormOpen(true);
  };

  const openEditNewsForm = async (id: number) => {
    setIsNewsLoading(true);
    setPageError('');
    try {
      const result = await fetchAdminNewsDetail(id);
      setEditingNewsId(id);
      setNewsFormState({
        title: result.data.title,
        summary: result.data.summary,
        content: result.data.content,
        sourceName: result.data.sourceName,
        sourceUrl: result.data.sourceUrl,
        coverImage: result.data.coverImage,
        categoryId: result.data.categoryId,
        tagIds: result.data.tagIds,
        publishTime: toDateTimeLocal(result.data.publishTime),
        heatScore: result.data.heatScore,
        status: result.data.status,
      });
      setIsNewsFormOpen(true);
    } catch (error) {
      setPageError(getErrorMessage(error, '加载新闻详情失败'));
    } finally {
      setIsNewsLoading(false);
    }
  };

  const openEditCrawlForm = async (id: number) => {
    setIsCrawlLoading(true);
    setPageError('');
    try {
      const result = await fetchAdminCrawlConfigDetail(id);
      setEditingCrawlId(id);
      setCrawlFormState({
        sourceName: result.data.sourceName,
        sourceUrl: result.data.sourceUrl,
        sourceType: result.data.sourceType,
        categoryId: result.data.categoryId,
        enabled: result.data.enabled,
        crawlInterval: result.data.crawlInterval,
      });
      setIsCrawlFormOpen(true);
    } catch (error) {
      setPageError(getErrorMessage(error, '加载采集源详情失败'));
    } finally {
      setIsCrawlLoading(false);
    }
  };

  const handleNewsCategoryChange = (value: string) => {
    const categoryId = Number(value);
    setNewsFormState((current) => ({
      ...current,
      categoryId,
      tagIds: current.tagIds.filter((tagId) => tags.some((tag) => tag.id === tagId && tag.categoryId === categoryId)),
    }));
  };

  const handleNewsTagToggle = (tagId: number, checked: boolean) => {
    setNewsFormState((current) => ({
      ...current,
      tagIds: checked
        ? Array.from(new Set([...current.tagIds, tagId]))
        : current.tagIds.filter((item) => item !== tagId),
    }));
  };

  const handleSaveNews = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsNewsSubmitting(true);
    setPageError('');
    setPageSuccess('');

    try {
      const payload: NewsFormPayload = {
        ...newsFormState,
        categoryId: Number(newsFormState.categoryId),
        heatScore: Number(newsFormState.heatScore),
        status: Number(newsFormState.status),
      };

      if (editingNewsId) {
        await updateAdminNews(editingNewsId, payload);
        setPageSuccess('新闻已更新');
      } else {
        await createAdminNews(payload);
        setPageSuccess('新闻已创建');
      }

      setIsNewsFormOpen(false);
      resetNewsForm();
      await Promise.all([loadSummary(), loadNews(editingNewsId ? newsPageData.page : 1, newsPageData.size)]);
    } catch (error) {
      setPageError(getErrorMessage(error, '保存新闻失败'));
    } finally {
      setIsNewsSubmitting(false);
    }
  };

  const handleSaveCrawlConfig = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsCrawlSubmitting(true);
    setPageError('');
    setPageSuccess('');

    try {
      const payload: CrawlConfigFormPayload = {
        ...crawlFormState,
        categoryId: Number(crawlFormState.categoryId),
        enabled: Number(crawlFormState.enabled),
        crawlInterval: Number(crawlFormState.crawlInterval),
        sourceType: crawlFormState.sourceType.toUpperCase(),
      };

      if (editingCrawlId) {
        await updateAdminCrawlConfig(editingCrawlId, payload);
        setPageSuccess('采集源已更新');
      } else {
        await createAdminCrawlConfig(payload);
        setPageSuccess('采集源已创建');
      }

      setIsCrawlFormOpen(false);
      resetCrawlForm();
      await Promise.all([loadSummary(), loadCrawlConfigs()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '保存采集源失败'));
    } finally {
      setIsCrawlSubmitting(false);
    }
  };

  const handleDeleteNews = async (id: number) => {
    if (!window.confirm('确认删除这条新闻吗？删除后关联标签也会一并移除。')) {
      return;
    }

    try {
      await deleteAdminNews(id);
      setPageSuccess('新闻已删除');
      const nextPage = newsPageData.records.length === 1 && newsPageData.page > 1 ? newsPageData.page - 1 : newsPageData.page;
      await Promise.all([loadSummary(), loadNews(nextPage, newsPageData.size)]);
    } catch (error) {
      setPageError(getErrorMessage(error, '删除新闻失败'));
    }
  };

  const handleToggleNewsStatus = async (item: AdminNewsListItem) => {
    try {
      await updateAdminNewsStatus(item.id, item.status === 1 ? 0 : 1);
      setPageSuccess(item.status === 1 ? '新闻已下架' : '新闻已上架');
      await Promise.all([loadSummary(), loadNews(newsPageData.page, newsPageData.size)]);
    } catch (error) {
      setPageError(getErrorMessage(error, '更新新闻状态失败'));
    }
  };

  const handleDeleteCrawlConfig = async (id: number) => {
    if (!window.confirm('确认删除这个采集源吗？已采集入库的新闻不会被删除。')) {
      return;
    }

    try {
      await deleteAdminCrawlConfig(id);
      setPageSuccess('采集源已删除');
      await Promise.all([loadSummary(), loadCrawlConfigs()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '删除采集源失败'));
    }
  };

  const handleToggleCrawlStatus = async (item: AdminCrawlConfigItem) => {
    try {
      await updateAdminCrawlConfigStatus(item.id, item.enabled === 1 ? 0 : 1);
      setPageSuccess(item.enabled === 1 ? '采集源已停用' : '采集源已启用');
      await Promise.all([loadSummary(), loadCrawlConfigs()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '更新采集源状态失败'));
    }
  };

  const handleRunCrawl = async (id: number) => {
    setRunningCrawlId(id);
    setPageError('');
    setPageSuccess('');
    try {
      const result = await runAdminCrawlConfig(id);
      setPageSuccess(`采集完成：新增 ${result.data.insertedCount} 篇，跳过 ${result.data.duplicateCount} 篇重复新闻`);
      await refreshAdminData();
    } catch (error) {
      setPageError(getErrorMessage(error, '手动采集失败'));
    } finally {
      setRunningCrawlId(null);
    }
  };

  return (
    <section className="admin-dashboard-page">
      <div className="page-card">
        <div className="admin-header">
          <div>
            <p className="page-eyebrow">阶段 9</p>
            <h1>推荐系统展示与后台管理</h1>
            <p className="page-description">
              当前阶段聚焦推荐效果展示、行为统计和答辩演示效果优化，帮助老师更直观看到推荐系统是如何工作的。
            </p>
          </div>
          <button type="button" className="ghost-button" onClick={() => void refreshAdminData()}>
            刷新概览
          </button>
        </div>

        <section className="admin-overview-section">
          <div className="section-heading">
            <div>
              <p className="page-eyebrow">系统概览</p>
              <h2>基础统计</h2>
            </div>
            <span className="overview-status-text">{isSummaryLoading ? '统计更新中...' : '统计已更新'}</span>
          </div>
          <div className="admin-overview-grid">
            {summaryCards.map((card) => (
              <article key={card.label} className={`admin-overview-card ${card.tone}`}>
                <span>{card.label}</span>
                <strong>{card.value}</strong>
              </article>
            ))}
          </div>
        </section>

        <section className="admin-overview-section">
          <div className="section-heading compact">
            <div>
              <p className="page-eyebrow">推荐说明</p>
              <h2>推荐策略与行为统计</h2>
            </div>
          </div>
          <div className="admin-insight-grid">
            <article className="admin-insight-card">
              <h3>当前推荐策略</h3>
              <p>兴趣标签 + 用户行为 + 新闻热度 + 时间新鲜度</p>
              <p className="section-meta">当用户产生浏览、点赞、收藏、评论等行为后，推荐顺序会随之调整。</p>
            </article>
            <article className="admin-insight-card">
              <h3>负反馈过滤</h3>
              <p>不感兴趣内容不再推荐</p>
              <p className="section-meta">用户标记“不感兴趣”的新闻会从推荐候选中剔除，用于演示负反馈机制。</p>
            </article>
          </div>
          <div className="admin-behavior-grid">
            {behaviorCards.map((card) => (
              <article key={card.label} className="admin-mini-card">
                <span>{card.label}</span>
                <strong>{card.value}</strong>
              </article>
            ))}
          </div>
        </section>

        <section className="admin-overview-section">
          <div className="section-heading compact">
            <div>
              <p className="page-eyebrow">内容概览</p>
              <h2>热门新闻与分类分布</h2>
            </div>
          </div>
          <div className="admin-insight-grid">
            <article className="admin-insight-card">
              <h3>热门新闻 Top 5</h3>
              {summary.hotNews.length === 0 ? (
                <div className="news-state-card compact-empty-state">暂无热门新闻数据。</div>
              ) : (
                <div className="admin-ranked-list">
                  {summary.hotNews.map((item, index) => (
                    <div key={item.id} className="admin-ranked-item">
                      <span className="hot-news-rank">{String(index + 1).padStart(2, '0')}</span>
                      <div>
                        <strong>{item.title}</strong>
                        <p>热度 {item.heatScore} · 点赞 {item.likeCount} · 收藏 {item.favoriteCount}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>
            <article className="admin-insight-card">
              <h3>分类新闻数量</h3>
              {summary.categoryStats.length === 0 ? (
                <div className="news-state-card compact-empty-state">暂无分类统计数据。</div>
              ) : (
                <div className="admin-category-list">
                  {summary.categoryStats.map((item) => (
                    <div key={item.categoryId} className="admin-category-item">
                      <span>{item.categoryName}</span>
                      <strong>{item.newsCount}</strong>
                    </div>
                  ))}
                </div>
              )}
            </article>
          </div>
        </section>

        <div className="admin-section-tabs" role="tablist" aria-label="后台模块切换">
          <button
            type="button"
            className={`admin-section-tab ${activeSection === 'news' ? 'active' : ''}`}
            onClick={() => setActiveSection('news')}
          >
            新闻管理
          </button>
          <button
            type="button"
            className={`admin-section-tab ${activeSection === 'crawl' ? 'active' : ''}`}
            onClick={() => setActiveSection('crawl')}
          >
            采集管理
          </button>
        </div>

        {pageError ? <p className="auth-feedback error">{pageError}</p> : null}
        {pageSuccess ? <p className="auth-feedback success">{pageSuccess}</p> : null}

        {activeSection === 'news' ? (
          <>
            <div className="admin-toolbar-row">
              <div>
                <h2>新闻管理</h2>
                <p className="section-description">管理手工录入和采集入库的新闻，支持搜索、筛选、编辑和上下架。</p>
              </div>
              <button type="button" className="primary-button" onClick={openCreateNewsForm}>
                新增新闻
              </button>
            </div>

            <form
              className="admin-toolbar"
              onSubmit={(event) => {
                event.preventDefault();
                void loadNews(1, newsPageData.size);
              }}
            >
              <input
                className="toolbar-input"
                value={newsFilters.keyword}
                onChange={(event) => setNewsFilters((current) => ({ ...current, keyword: event.target.value }))}
                placeholder="搜索标题或摘要"
              />
              <select
                className="toolbar-select"
                value={newsFilters.categoryId}
                onChange={(event) => setNewsFilters((current) => ({ ...current, categoryId: event.target.value }))}
              >
                <option value="">全部分类</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              <select
                className="toolbar-select"
                value={newsFilters.status}
                onChange={(event) => setNewsFilters((current) => ({ ...current, status: event.target.value }))}
              >
                <option value="">全部状态</option>
                <option value="1">已上架</option>
                <option value="0">已下架</option>
              </select>
              <button type="submit" className="ghost-button">
                查询
              </button>
            </form>

            <div className="admin-news-summary">
              <span>当前页：{newsPageData.page}</span>
              <span>每页：{newsPageData.size}</span>
              <span>总数：{newsPageData.total}</span>
            </div>

            <div className="admin-news-table-wrap">
              <table className="admin-news-table">
                <thead>
                  <tr>
                    <th>标题</th>
                    <th>分类</th>
                    <th>标签</th>
                    <th>发布时间</th>
                    <th>热度</th>
                    <th>状态</th>
                    <th>数据</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {isNewsLoading ? (
                    <tr>
                      <td colSpan={8}>加载中...</td>
                    </tr>
                  ) : newsPageData.records.length === 0 ? (
                    <tr>
                      <td colSpan={8}>暂无新闻数据</td>
                    </tr>
                  ) : (
                    newsPageData.records.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.title}</strong>
                          <p>{item.summary}</p>
                        </td>
                        <td>{item.categoryName}</td>
                        <td>{item.tagNames.join(' / ')}</td>
                        <td>{formatDisplayDate(item.publishTime)}</td>
                        <td>{item.heatScore}</td>
                        <td>
                          <span className={item.status === 1 ? 'status-pill online' : 'status-pill offline'}>
                            {item.status === 1 ? '已上架' : '已下架'}
                          </span>
                        </td>
                        <td>
                          浏览 {item.viewCount} / 点赞 {item.likeCount} / 收藏 {item.favoriteCount}
                        </td>
                        <td>
                          <div className="row-actions">
                            <button type="button" className="ghost-button" onClick={() => void openEditNewsForm(item.id)}>
                              编辑
                            </button>
                            <button type="button" className="ghost-button" onClick={() => void handleToggleNewsStatus(item)}>
                              {item.status === 1 ? '下架' : '上架'}
                            </button>
                            <button type="button" className="ghost-button danger" onClick={() => void handleDeleteNews(item.id)}>
                              删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="pagination-bar">
              <button
                type="button"
                className="ghost-button"
                disabled={newsPageData.page <= 1}
                onClick={() => void loadNews(newsPageData.page - 1, newsPageData.size)}
              >
                上一页
              </button>
              <span>
                第 {newsPageData.page} / {Math.max(1, Math.ceil(newsPageData.total / newsPageData.size))} 页
              </span>
              <button
                type="button"
                className="ghost-button"
                disabled={newsPageData.page >= Math.ceil(newsPageData.total / newsPageData.size || 1)}
                onClick={() => void loadNews(newsPageData.page + 1, newsPageData.size)}
              >
                下一页
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="admin-toolbar-row">
              <div>
                <h2>采集管理</h2>
                <p className="section-description">管理 RSS 采集源，查看最近采集结果，并支持手动触发采集。</p>
              </div>
              <button type="button" className="primary-button" onClick={openCreateCrawlForm}>
                新增采集源
              </button>
            </div>

            <div className="admin-news-table-wrap">
              <table className="admin-news-table">
                <thead>
                  <tr>
                    <th>采集源名称</th>
                    <th>类型</th>
                    <th>默认分类</th>
                    <th>采集间隔</th>
                    <th>启用状态</th>
                    <th>上次采集时间</th>
                    <th>上次新增数量</th>
                    <th>上次状态</th>
                    <th>错误信息</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {isCrawlLoading ? (
                    <tr>
                      <td colSpan={10}>加载中...</td>
                    </tr>
                  ) : crawlConfigs.length === 0 ? (
                    <tr>
                      <td colSpan={10}>暂无采集源，请先新增一个 RSS 地址。</td>
                    </tr>
                  ) : (
                    crawlConfigs.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.sourceName}</strong>
                          <p className="admin-url-text">{item.sourceUrl}</p>
                        </td>
                        <td>{item.sourceType}</td>
                        <td>{item.categoryName ?? '-'}</td>
                        <td>{item.crawlInterval} 分钟</td>
                        <td>
                          <span className={item.enabled === 1 ? 'status-pill online' : 'status-pill offline'}>
                            {item.enabled === 1 ? '启用中' : '已停用'}
                          </span>
                        </td>
                        <td>{item.lastCrawlTime ? formatDisplayDate(item.lastCrawlTime) : '-'}</td>
                        <td>{item.lastCrawlCount}</td>
                        <td>{item.lastStatus ?? '-'}</td>
                        <td>{item.lastError ? truncateText(item.lastError, 60) : '-'}</td>
                        <td>
                          <div className="row-actions">
                            <button type="button" className="ghost-button" onClick={() => void openEditCrawlForm(item.id)}>
                              编辑
                            </button>
                            <button type="button" className="ghost-button" onClick={() => void handleToggleCrawlStatus(item)}>
                              {item.enabled === 1 ? '停用' : '启用'}
                            </button>
                            <button
                              type="button"
                              className="ghost-button"
                              disabled={runningCrawlId === item.id}
                              onClick={() => void handleRunCrawl(item.id)}
                            >
                              {runningCrawlId === item.id ? '采集中...' : '手动采集'}
                            </button>
                            <button type="button" className="ghost-button danger" onClick={() => void handleDeleteCrawlConfig(item.id)}>
                              删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {isNewsFormOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setIsNewsFormOpen(false)}>
          <div className="auth-modal admin-form-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="auth-header">
              <div>
                <p className="auth-eyebrow">阶段 3</p>
                <h2>{editingNewsId ? '编辑新闻' : '新增新闻'}</h2>
              </div>
              <button type="button" className="ghost-button" onClick={() => setIsNewsFormOpen(false)}>
                关闭
              </button>
            </div>

            <form className="auth-form admin-news-form" onSubmit={handleSaveNews}>
              <label>
                标题
                <input
                  value={newsFormState.title}
                  onChange={(event) => setNewsFormState((current) => ({ ...current, title: event.target.value }))}
                  required
                />
              </label>
              <label>
                摘要
                <textarea
                  value={newsFormState.summary}
                  onChange={(event) => setNewsFormState((current) => ({ ...current, summary: event.target.value }))}
                  rows={3}
                  required
                />
              </label>
              <label>
                正文
                <textarea
                  value={newsFormState.content}
                  onChange={(event) => setNewsFormState((current) => ({ ...current, content: event.target.value }))}
                  rows={6}
                  required
                />
              </label>
              <div className="admin-form-grid">
                <label>
                  来源名称
                  <input
                    value={newsFormState.sourceName}
                    onChange={(event) => setNewsFormState((current) => ({ ...current, sourceName: event.target.value }))}
                    required
                  />
                </label>
                <label>
                  原文链接
                  <input
                    value={newsFormState.sourceUrl}
                    onChange={(event) => setNewsFormState((current) => ({ ...current, sourceUrl: event.target.value }))}
                    required
                  />
                </label>
              </div>
              <label>
                封面图地址
                <input
                  value={newsFormState.coverImage}
                  onChange={(event) => setNewsFormState((current) => ({ ...current, coverImage: event.target.value }))}
                  required
                />
              </label>
              <div className="admin-form-grid">
                <label>
                  分类
                  <select
                    value={newsFormState.categoryId}
                    onChange={(event) => handleNewsCategoryChange(event.target.value)}
                    required
                  >
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  发布时间
                  <input
                    type="datetime-local"
                    value={newsFormState.publishTime}
                    onChange={(event) => setNewsFormState((current) => ({ ...current, publishTime: event.target.value }))}
                    required
                  />
                </label>
              </div>
              <div className="admin-form-grid">
                <label>
                  热度分
                  <input
                    type="number"
                    step="0.1"
                    value={newsFormState.heatScore}
                    onChange={(event) =>
                      setNewsFormState((current) => ({ ...current, heatScore: Number(event.target.value) }))
                    }
                    required
                  />
                </label>
                <label>
                  状态
                  <select
                    value={newsFormState.status}
                    onChange={(event) => setNewsFormState((current) => ({ ...current, status: Number(event.target.value) }))}
                  >
                    <option value={1}>上架</option>
                    <option value={0}>下架</option>
                  </select>
                </label>
              </div>
              <fieldset className="tag-selector">
                <legend>标签多选</legend>
                <div className="tag-option-list">
                  {visibleNewsTags.map((tag) => (
                    <label key={tag.id} className="tag-option">
                      <input
                        type="checkbox"
                        checked={newsFormState.tagIds.includes(tag.id)}
                        onChange={(event) => handleNewsTagToggle(tag.id, event.target.checked)}
                      />
                      <span>{tag.name}</span>
                    </label>
                  ))}
                </div>
              </fieldset>
              <button type="submit" className="primary-button" disabled={isNewsSubmitting}>
                {isNewsSubmitting ? '保存中...' : editingNewsId ? '保存修改' : '创建新闻'}
              </button>
            </form>
          </div>
        </div>
      ) : null}

      {isCrawlFormOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setIsCrawlFormOpen(false)}>
          <div className="auth-modal admin-form-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="auth-header">
              <div>
                <p className="auth-eyebrow">阶段 7</p>
                <h2>{editingCrawlId ? '编辑采集源' : '新增采集源'}</h2>
              </div>
              <button type="button" className="ghost-button" onClick={() => setIsCrawlFormOpen(false)}>
                关闭
              </button>
            </div>

            <form className="auth-form admin-news-form" onSubmit={handleSaveCrawlConfig}>
              <label>
                采集源名称
                <input
                  value={crawlFormState.sourceName}
                  onChange={(event) => setCrawlFormState((current) => ({ ...current, sourceName: event.target.value }))}
                  required
                />
              </label>
              <label>
                RSS 地址
                <input
                  value={crawlFormState.sourceUrl}
                  onChange={(event) => setCrawlFormState((current) => ({ ...current, sourceUrl: event.target.value }))}
                  placeholder="https://example.com/rss.xml"
                  required
                />
              </label>
              <div className="admin-form-grid">
                <label>
                  来源类型
                  <select
                    value={crawlFormState.sourceType}
                    onChange={(event) => setCrawlFormState((current) => ({ ...current, sourceType: event.target.value }))}
                  >
                    <option value="RSS">RSS</option>
                  </select>
                </label>
                <label>
                  默认分类
                  <select
                    value={crawlFormState.categoryId}
                    onChange={(event) =>
                      setCrawlFormState((current) => ({ ...current, categoryId: Number(event.target.value) }))
                    }
                    required
                  >
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="admin-form-grid">
                <label>
                  启用状态
                  <select
                    value={crawlFormState.enabled}
                    onChange={(event) =>
                      setCrawlFormState((current) => ({ ...current, enabled: Number(event.target.value) }))
                    }
                  >
                    <option value={1}>启用</option>
                    <option value={0}>停用</option>
                  </select>
                </label>
                <label>
                  采集间隔（分钟）
                  <input
                    type="number"
                    min={1}
                    value={crawlFormState.crawlInterval}
                    onChange={(event) =>
                      setCrawlFormState((current) => ({ ...current, crawlInterval: Number(event.target.value) }))
                    }
                    required
                  />
                </label>
              </div>
              <p className="form-helper-text">当前表结构没有单独的默认标签字段，入库时会自动绑定所选分类下已启用的标签。</p>
              <button type="submit" className="primary-button" disabled={isCrawlSubmitting}>
                {isCrawlSubmitting ? '保存中...' : editingCrawlId ? '保存采集源' : '创建采集源'}
              </button>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

function truncateText(value: string, maxLength: number) {
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

export default AdminDashboardPage;
