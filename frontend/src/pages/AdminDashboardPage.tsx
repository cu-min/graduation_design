import { useEffect, useMemo, useState } from 'react';
import { fetchAdminCategories, createAdminCategory, deleteAdminCategory, updateAdminCategory, updateAdminCategoryStatus } from '../api/adminCategories';
import {
  createAdminCrawlConfig,
  deleteAdminCrawlConfig,
  fetchAdminCrawlConfigDetail,
  fetchAdminCrawlConfigs,
  runAdminCrawlConfig,
  updateAdminCrawlConfig,
  updateAdminCrawlConfigStatus,
} from '../api/adminCrawlConfigs';
import { fetchAdminDashboardSummary } from '../api/adminDashboard';
import {
  createAdminNews,
  deleteAdminNews,
  fetchAdminNews,
  fetchAdminNewsDetail,
  updateAdminNews,
  updateAdminNewsStatus,
} from '../api/adminNews';
import { fetchAdminTags, createAdminTag, deleteAdminTag, updateAdminTag, updateAdminTagStatus } from '../api/adminTags';
import { fetchAdminUsers, updateAdminUserStatus } from '../api/adminUsers';
import PagePlaceholder from '../components/PagePlaceholder';
import { useAuth } from '../store';
import type {
  AdminCategoryPayload,
  AdminCrawlConfigItem,
  AdminDashboardSummary,
  AdminNewsListItem,
  AdminTagPayload,
  AdminUserItem,
  CategoryOption,
  CrawlConfigFormPayload,
  NewsFormPayload,
  PageResult,
  TagOption,
} from '../types';
import { getErrorMessage } from '../utils/request';

type AdminSection = 'news' | 'categories' | 'tags' | 'users' | 'crawl';

type NewsFilterState = {
  keyword: string;
  categoryId: string;
  status: string;
};

type UserFilterState = {
  keyword: string;
  role: string;
  status: string;
};

const initialNewsFilters: NewsFilterState = {
  keyword: '',
  categoryId: '',
  status: '',
};

const initialUserFilters: UserFilterState = {
  keyword: '',
  role: '',
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

const initialCategoryFormState: AdminCategoryPayload = {
  name: '',
  code: '',
  description: '',
  sortOrder: 1,
  status: 1,
};

const initialTagFormState: AdminTagPayload = {
  categoryId: 1,
  name: '',
  code: '',
  sortOrder: 1,
  status: 1,
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
  const [summary, setSummary] = useState<AdminDashboardSummary>(emptySummary);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [tags, setTags] = useState<TagOption[]>([]);
  const [crawlConfigs, setCrawlConfigs] = useState<AdminCrawlConfigItem[]>([]);

  const [newsFilters, setNewsFilters] = useState<NewsFilterState>(initialNewsFilters);
  const [newsPageData, setNewsPageData] = useState<PageResult<AdminNewsListItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 8,
  });

  const [userFilters, setUserFilters] = useState<UserFilterState>(initialUserFilters);
  const [userPageData, setUserPageData] = useState<PageResult<AdminUserItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 8,
  });

  const [pageError, setPageError] = useState('');
  const [pageSuccess, setPageSuccess] = useState('');
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);
  const [isNewsLoading, setIsNewsLoading] = useState(false);
  const [isUsersLoading, setIsUsersLoading] = useState(false);
  const [isMetaLoading, setIsMetaLoading] = useState(false);
  const [isCrawlLoading, setIsCrawlLoading] = useState(false);

  const [isNewsFormOpen, setIsNewsFormOpen] = useState(false);
  const [isNewsSubmitting, setIsNewsSubmitting] = useState(false);
  const [editingNewsId, setEditingNewsId] = useState<number | null>(null);
  const [newsFormState, setNewsFormState] = useState<NewsFormPayload>(initialNewsFormState);

  const [isCategoryFormOpen, setIsCategoryFormOpen] = useState(false);
  const [isCategorySubmitting, setIsCategorySubmitting] = useState(false);
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null);
  const [categoryFormState, setCategoryFormState] = useState<AdminCategoryPayload>(initialCategoryFormState);

  const [isTagFormOpen, setIsTagFormOpen] = useState(false);
  const [isTagSubmitting, setIsTagSubmitting] = useState(false);
  const [editingTagId, setEditingTagId] = useState<number | null>(null);
  const [tagFormState, setTagFormState] = useState<AdminTagPayload>(initialTagFormState);

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

  const loadSummary = async () => {
    setIsSummaryLoading(true);
    try {
      const result = await fetchAdminDashboardSummary();
      setSummary(result.data);
    } finally {
      setIsSummaryLoading(false);
    }
  };

  const loadMeta = async () => {
    setIsMetaLoading(true);
    try {
      const [categoryResult, tagResult] = await Promise.all([fetchAdminCategories(), fetchAdminTags()]);
      setCategories(categoryResult.data);
      setTags(tagResult.data);
      setNewsFormState((current) => ({
        ...current,
        categoryId: categoryResult.data[0]?.id ?? current.categoryId,
      }));
      setTagFormState((current) => ({
        ...current,
        categoryId: categoryResult.data[0]?.id ?? current.categoryId,
      }));
      setCrawlFormState((current) => ({
        ...current,
        categoryId: categoryResult.data[0]?.id ?? current.categoryId,
      }));
    } finally {
      setIsMetaLoading(false);
    }
  };

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

  const loadUsers = async (page = userPageData.page, size = userPageData.size) => {
    setIsUsersLoading(true);
    try {
      const result = await fetchAdminUsers({
        page,
        size,
        keyword: userFilters.keyword.trim() || undefined,
        role: userFilters.role || undefined,
        status: userFilters.status ? Number(userFilters.status) : undefined,
      });
      setUserPageData(result.data);
    } finally {
      setIsUsersLoading(false);
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

  useEffect(() => {
    if (!isAdmin) {
      return;
    }

    const loadInitialData = async () => {
      setPageError('');
      try {
        await Promise.all([loadSummary(), loadMeta(), loadNews(1, 8), loadUsers(1, 8), loadCrawlConfigs()]);
      } catch (error) {
        setPageError(getErrorMessage(error, '初始化后台管理数据失败'));
      }
    };

    void loadInitialData();
  }, [isAdmin]);

  if (isBootstrapping) {
    return (
      <PagePlaceholder
        eyebrow=""
        title="正在加载管理员信息"
        description="正在恢复登录状态，并校验当前用户是否具备后台管理权限。"
      />
    );
  }

  if (!isAdmin) {
    return (
      <PagePlaceholder
        eyebrow=""
        title="无权限访问管理后台"
        description="当前页面仅允许 ADMIN 角色访问，请使用管理员账号登录后再进入 /admin。"
      />
    );
  }

  const refreshMeta = async () => {
    await loadMeta();
  };

  const refreshAdminData = async () => {
    await Promise.all([loadSummary(), loadNews(newsPageData.page, newsPageData.size), loadUsers(userPageData.page, userPageData.size), loadCrawlConfigs()]);
  };

  const resetNewsForm = () => {
    setEditingNewsId(null);
    setNewsFormState({
      ...initialNewsFormState,
      categoryId: categories[0]?.id ?? 1,
    });
  };

  const resetCategoryForm = () => {
    setEditingCategoryId(null);
    setCategoryFormState({
      ...initialCategoryFormState,
      sortOrder: categories.length + 1,
    });
  };

  const resetTagForm = () => {
    setEditingTagId(null);
    setTagFormState({
      ...initialTagFormState,
      categoryId: categories[0]?.id ?? 1,
      sortOrder: tags.length + 1,
    });
  };

  const resetCrawlForm = () => {
    setEditingCrawlId(null);
    setCrawlFormState({
      ...initialCrawlFormState,
      categoryId: categories[0]?.id ?? 1,
    });
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

  const openEditCategoryForm = (item: CategoryOption) => {
    setEditingCategoryId(item.id);
    setCategoryFormState({
      name: item.name,
      code: item.code,
      description: item.description ?? '',
      sortOrder: item.sortOrder,
      status: item.status,
    });
    setIsCategoryFormOpen(true);
  };

  const openEditTagForm = (item: TagOption) => {
    setEditingTagId(item.id);
    setTagFormState({
      categoryId: item.categoryId,
      name: item.name,
      code: item.code,
      sortOrder: item.sortOrder,
      status: item.status,
    });
    setIsTagFormOpen(true);
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
      if (editingNewsId) {
        await updateAdminNews(editingNewsId, newsFormState);
        setPageSuccess('新闻已更新');
      } else {
        await createAdminNews(newsFormState);
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

  const handleSaveCategory = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsCategorySubmitting(true);
    setPageError('');
    setPageSuccess('');

    try {
      if (editingCategoryId) {
        await updateAdminCategory(editingCategoryId, categoryFormState);
        setPageSuccess('分类已更新');
      } else {
        await createAdminCategory(categoryFormState);
        setPageSuccess('分类已创建');
      }
      setIsCategoryFormOpen(false);
      resetCategoryForm();
      await Promise.all([loadSummary(), refreshMeta()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '保存分类失败'));
    } finally {
      setIsCategorySubmitting(false);
    }
  };

  const handleSaveTag = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsTagSubmitting(true);
    setPageError('');
    setPageSuccess('');

    try {
      if (editingTagId) {
        await updateAdminTag(editingTagId, tagFormState);
        setPageSuccess('标签已更新');
      } else {
        await createAdminTag(tagFormState);
        setPageSuccess('标签已创建');
      }
      setIsTagFormOpen(false);
      resetTagForm();
      await refreshMeta();
    } catch (error) {
      setPageError(getErrorMessage(error, '保存标签失败'));
    } finally {
      setIsTagSubmitting(false);
    }
  };

  const handleSaveCrawlConfig = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsCrawlSubmitting(true);
    setPageError('');
    setPageSuccess('');

    try {
      if (editingCrawlId) {
        await updateAdminCrawlConfig(editingCrawlId, crawlFormState);
        setPageSuccess('采集源已更新');
      } else {
        await createAdminCrawlConfig(crawlFormState);
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

  const handleDeleteCategory = async (id: number) => {
    if (!window.confirm('确认删除这个分类吗？如果分类仍被标签、新闻或采集源使用，将无法删除。')) {
      return;
    }

    try {
      await deleteAdminCategory(id);
      setPageSuccess('分类已删除');
      await Promise.all([loadSummary(), refreshMeta()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '删除分类失败'));
    }
  };

  const handleDeleteTag = async (id: number) => {
    if (!window.confirm('确认删除这个标签吗？如果标签仍被新闻或兴趣绑定，将无法删除。')) {
      return;
    }

    try {
      await deleteAdminTag(id);
      setPageSuccess('标签已删除');
      await refreshMeta();
    } catch (error) {
      setPageError(getErrorMessage(error, '删除标签失败'));
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

  const handleToggleNewsStatus = async (item: AdminNewsListItem) => {
    try {
      await updateAdminNewsStatus(item.id, item.status === 1 ? 0 : 1);
      setPageSuccess(item.status === 1 ? '新闻已下架' : '新闻已上架');
      await Promise.all([loadSummary(), loadNews(newsPageData.page, newsPageData.size)]);
    } catch (error) {
      setPageError(getErrorMessage(error, '更新新闻状态失败'));
    }
  };

  const handleToggleCategoryStatus = async (item: CategoryOption) => {
    try {
      await updateAdminCategoryStatus(item.id, item.status === 1 ? 0 : 1);
      setPageSuccess(item.status === 1 ? '分类已停用' : '分类已启用');
      await Promise.all([loadSummary(), refreshMeta()]);
    } catch (error) {
      setPageError(getErrorMessage(error, '更新分类状态失败'));
    }
  };

  const handleToggleTagStatus = async (item: TagOption) => {
    try {
      await updateAdminTagStatus(item.id, item.status === 1 ? 0 : 1);
      setPageSuccess(item.status === 1 ? '标签已停用' : '标签已启用');
      await refreshMeta();
    } catch (error) {
      setPageError(getErrorMessage(error, '更新标签状态失败'));
    }
  };

  const handleToggleUserStatus = async (item: AdminUserItem) => {
    try {
      await updateAdminUserStatus(item.id, item.status === 1 ? 0 : 1);
      setPageSuccess(item.status === 1 ? '用户已禁用' : '用户已启用');
      await Promise.all([loadSummary(), loadUsers(userPageData.page, userPageData.size)]);
    } catch (error) {
      setPageError(getErrorMessage(error, '更新用户状态失败'));
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
            <h1>推荐系统展示与后台管理</h1>
            <p className="page-description">
              在这里可以查看系统概览，并完成新闻、分类、标签、用户和采集源的后台管理。
            </p>
          </div>
          <button type="button" className="ghost-button" onClick={() => void refreshAdminData()}>
            刷新概览
          </button>
        </div>

        {pageError ? <p className="auth-feedback error">{pageError}</p> : null}
        {pageSuccess ? <p className="auth-feedback success">{pageSuccess}</p> : null}

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
              <p className="page-eyebrow">行为概览</p>
              <h2>推荐行为统计</h2>
            </div>
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
              <p className="page-eyebrow">内容分布</p>
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
                        <p>热度 {item.heatScore} / 点赞 {item.likeCount} / 收藏 {item.favoriteCount}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>
            <article className="admin-insight-card">
              <h3>分类内容分布</h3>
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
          {[
            ['news', '新闻管理'],
            ['categories', '分类管理'],
            ['tags', '标签管理'],
            ['users', '用户管理'],
            ['crawl', '采集管理'],
          ].map(([key, label]) => (
            <button
              key={key}
              type="button"
              className={`admin-section-tab ${activeSection === key ? 'active' : ''}`}
              onClick={() => setActiveSection(key as AdminSection)}
            >
              {label}
            </button>
          ))}
        </div>

        {activeSection === 'news' ? (
          <NewsSection
            categories={categories}
            newsFilters={newsFilters}
            newsPageData={newsPageData}
            isLoading={isNewsLoading}
            onChangeFilters={setNewsFilters}
            onQuery={() => void loadNews(1, newsPageData.size)}
            onPageChange={(page) => void loadNews(page, newsPageData.size)}
            onCreate={() => {
              resetNewsForm();
              setIsNewsFormOpen(true);
            }}
            onEdit={openEditNewsForm}
            onDelete={handleDeleteNews}
            onToggleStatus={handleToggleNewsStatus}
          />
        ) : null}

        {activeSection === 'categories' ? (
          <CategorySection
            categories={categories}
            isLoading={isMetaLoading}
            onCreate={() => {
              resetCategoryForm();
              setIsCategoryFormOpen(true);
            }}
            onEdit={openEditCategoryForm}
            onDelete={handleDeleteCategory}
            onToggleStatus={handleToggleCategoryStatus}
          />
        ) : null}

        {activeSection === 'tags' ? (
          <TagSection
            categories={categories}
            tags={tags}
            isLoading={isMetaLoading}
            onCreate={() => {
              resetTagForm();
              setIsTagFormOpen(true);
            }}
            onEdit={openEditTagForm}
            onDelete={handleDeleteTag}
            onToggleStatus={handleToggleTagStatus}
          />
        ) : null}

        {activeSection === 'users' ? (
          <UserSection
            filters={userFilters}
            pageData={userPageData}
            isLoading={isUsersLoading}
            onChangeFilters={setUserFilters}
            onQuery={() => void loadUsers(1, userPageData.size)}
            onPageChange={(page) => void loadUsers(page, userPageData.size)}
            onToggleStatus={handleToggleUserStatus}
          />
        ) : null}

        {activeSection === 'crawl' ? (
          <CrawlSection
            crawlConfigs={crawlConfigs}
            isLoading={isCrawlLoading}
            runningCrawlId={runningCrawlId}
            onCreate={() => {
              resetCrawlForm();
              setIsCrawlFormOpen(true);
            }}
            onEdit={openEditCrawlForm}
            onDelete={handleDeleteCrawlConfig}
            onToggleStatus={handleToggleCrawlStatus}
            onRun={handleRunCrawl}
          />
        ) : null}
      </div>

      {isNewsFormOpen ? (
          <ModalFrame
            eyebrow=""
            title={editingNewsId ? '编辑新闻' : '新增新闻'}
            onClose={() => setIsNewsFormOpen(false)}
          >
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
                  onChange={(event) => setNewsFormState((current) => ({ ...current, heatScore: Number(event.target.value) }))}
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
        </ModalFrame>
      ) : null}

      {isCategoryFormOpen ? (
        <ModalFrame
          eyebrow="P0"
          title={editingCategoryId ? '编辑分类' : '新增分类'}
          onClose={() => setIsCategoryFormOpen(false)}
        >
          <form className="auth-form admin-news-form" onSubmit={handleSaveCategory}>
            <label>
              分类名称
              <input
                value={categoryFormState.name}
                onChange={(event) => setCategoryFormState((current) => ({ ...current, name: event.target.value }))}
                required
              />
            </label>
            <label>
              分类编码
              <input
                value={categoryFormState.code}
                onChange={(event) => setCategoryFormState((current) => ({ ...current, code: event.target.value }))}
                required
              />
            </label>
            <label>
              描述
              <textarea
                value={categoryFormState.description}
                onChange={(event) => setCategoryFormState((current) => ({ ...current, description: event.target.value }))}
                rows={3}
              />
            </label>
            <div className="admin-form-grid">
              <label>
                排序
                <input
                  type="number"
                  value={categoryFormState.sortOrder}
                  onChange={(event) => setCategoryFormState((current) => ({ ...current, sortOrder: Number(event.target.value) }))}
                  required
                />
              </label>
              <label>
                状态
                <select
                  value={categoryFormState.status}
                  onChange={(event) => setCategoryFormState((current) => ({ ...current, status: Number(event.target.value) }))}
                >
                  <option value={1}>启用</option>
                  <option value={0}>停用</option>
                </select>
              </label>
            </div>
            <button type="submit" className="primary-button" disabled={isCategorySubmitting}>
              {isCategorySubmitting ? '保存中...' : editingCategoryId ? '保存分类' : '创建分类'}
            </button>
          </form>
        </ModalFrame>
      ) : null}

      {isTagFormOpen ? (
        <ModalFrame
          eyebrow="P0"
          title={editingTagId ? '编辑标签' : '新增标签'}
          onClose={() => setIsTagFormOpen(false)}
        >
          <form className="auth-form admin-news-form" onSubmit={handleSaveTag}>
            <label>
              所属分类
              <select
                value={tagFormState.categoryId}
                onChange={(event) => setTagFormState((current) => ({ ...current, categoryId: Number(event.target.value) }))}
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
              标签名称
              <input
                value={tagFormState.name}
                onChange={(event) => setTagFormState((current) => ({ ...current, name: event.target.value }))}
                required
              />
            </label>
            <label>
              标签编码
              <input
                value={tagFormState.code}
                onChange={(event) => setTagFormState((current) => ({ ...current, code: event.target.value }))}
                required
              />
            </label>
            <div className="admin-form-grid">
              <label>
                排序
                <input
                  type="number"
                  value={tagFormState.sortOrder}
                  onChange={(event) => setTagFormState((current) => ({ ...current, sortOrder: Number(event.target.value) }))}
                  required
                />
              </label>
              <label>
                状态
                <select
                  value={tagFormState.status}
                  onChange={(event) => setTagFormState((current) => ({ ...current, status: Number(event.target.value) }))}
                >
                  <option value={1}>启用</option>
                  <option value={0}>停用</option>
                </select>
              </label>
            </div>
            <button type="submit" className="primary-button" disabled={isTagSubmitting}>
              {isTagSubmitting ? '保存中...' : editingTagId ? '保存标签' : '创建标签'}
            </button>
          </form>
        </ModalFrame>
      ) : null}

      {isCrawlFormOpen ? (
          <ModalFrame
            eyebrow=""
            title={editingCrawlId ? '编辑采集源' : '新增采集源'}
            onClose={() => setIsCrawlFormOpen(false)}
          >
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
                  onChange={(event) => setCrawlFormState((current) => ({ ...current, categoryId: Number(event.target.value) }))}
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
                  onChange={(event) => setCrawlFormState((current) => ({ ...current, enabled: Number(event.target.value) }))}
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
                  onChange={(event) => setCrawlFormState((current) => ({ ...current, crawlInterval: Number(event.target.value) }))}
                  required
                />
              </label>
            </div>
            <button type="submit" className="primary-button" disabled={isCrawlSubmitting}>
              {isCrawlSubmitting ? '保存中...' : editingCrawlId ? '保存采集源' : '创建采集源'}
            </button>
          </form>
        </ModalFrame>
      ) : null}
    </section>
  );
}

function NewsSection({
  categories,
  newsFilters,
  newsPageData,
  isLoading,
  onChangeFilters,
  onQuery,
  onPageChange,
  onCreate,
  onEdit,
  onDelete,
  onToggleStatus,
}: {
  categories: CategoryOption[];
  newsFilters: NewsFilterState;
  newsPageData: PageResult<AdminNewsListItem>;
  isLoading: boolean;
  onChangeFilters: React.Dispatch<React.SetStateAction<NewsFilterState>>;
  onQuery: () => void;
  onPageChange: (page: number) => void;
  onCreate: () => void;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
  onToggleStatus: (item: AdminNewsListItem) => void;
}) {
  return (
    <>
      <div className="admin-toolbar-row">
        <div>
          <h2>新闻管理</h2>
          <p className="section-description">管理手工录入和采集入库的新闻，支持搜索、筛选、编辑和上下架。</p>
        </div>
        <button type="button" className="primary-button" onClick={onCreate}>
          新增新闻
        </button>
      </div>

      <form
        className="admin-toolbar-row admin-filter-row"
        onSubmit={(event) => {
          event.preventDefault();
          onQuery();
        }}
      >
        <input
          className="toolbar-input"
          value={newsFilters.keyword}
          onChange={(event) => onChangeFilters((current) => ({ ...current, keyword: event.target.value }))}
          placeholder="搜索标题或摘要"
        />
        <select
          className="toolbar-select"
          value={newsFilters.categoryId}
          onChange={(event) => onChangeFilters((current) => ({ ...current, categoryId: event.target.value }))}
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
          onChange={(event) => onChangeFilters((current) => ({ ...current, status: event.target.value }))}
        >
          <option value="">全部状态</option>
          <option value="1">已上架</option>
          <option value="0">已下架</option>
        </select>
        <button type="submit" className="ghost-button">
          查询
        </button>
      </form>

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
            {isLoading ? (
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
                  <td>浏览 {item.viewCount} / 点赞 {item.likeCount} / 收藏 {item.favoriteCount}</td>
                  <td>
                    <div className="row-actions">
                      <button type="button" className="ghost-button" onClick={() => onEdit(item.id)}>
                        编辑
                      </button>
                      <button type="button" className="ghost-button" onClick={() => onToggleStatus(item)}>
                        {item.status === 1 ? '下架' : '上架'}
                      </button>
                      <button type="button" className="ghost-button danger" onClick={() => onDelete(item.id)}>
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

      <PaginationBar page={newsPageData.page} total={newsPageData.total} size={newsPageData.size} onPageChange={onPageChange} />
    </>
  );
}

function CategorySection({
  categories,
  isLoading,
  onCreate,
  onEdit,
  onDelete,
  onToggleStatus,
}: {
  categories: CategoryOption[];
  isLoading: boolean;
  onCreate: () => void;
  onEdit: (item: CategoryOption) => void;
  onDelete: (id: number) => void;
  onToggleStatus: (item: CategoryOption) => void;
}) {
  return (
    <>
      <div className="admin-toolbar-row">
        <div>
          <h2>分类管理</h2>
          <p className="section-description">维护系统分类，用于新闻归类、标签组织和采集源默认分类配置。</p>
        </div>
        <button type="button" className="primary-button" onClick={onCreate}>
          新增分类
        </button>
      </div>

      <div className="admin-news-table-wrap">
        <table className="admin-news-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>编码</th>
              <th>描述</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={6}>加载中...</td>
              </tr>
            ) : categories.length === 0 ? (
              <tr>
                <td colSpan={6}>暂无分类数据</td>
              </tr>
            ) : (
              categories.map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td>{item.code}</td>
                  <td>{item.description || '-'}</td>
                  <td>{item.sortOrder}</td>
                  <td>
                    <span className={item.status === 1 ? 'status-pill online' : 'status-pill offline'}>
                      {item.status === 1 ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button type="button" className="ghost-button" onClick={() => onEdit(item)}>
                        编辑
                      </button>
                      <button type="button" className="ghost-button" onClick={() => onToggleStatus(item)}>
                        {item.status === 1 ? '停用' : '启用'}
                      </button>
                      <button type="button" className="ghost-button danger" onClick={() => onDelete(item.id)}>
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
  );
}

function TagSection({
  categories,
  tags,
  isLoading,
  onCreate,
  onEdit,
  onDelete,
  onToggleStatus,
}: {
  categories: CategoryOption[];
  tags: TagOption[];
  isLoading: boolean;
  onCreate: () => void;
  onEdit: (item: TagOption) => void;
  onDelete: (id: number) => void;
  onToggleStatus: (item: TagOption) => void;
}) {
  const categoryMap = useMemo(() => new Map(categories.map((item) => [item.id, item.name])), [categories]);

  return (
    <>
      <div className="admin-toolbar-row">
        <div>
          <h2>标签管理</h2>
          <p className="section-description">维护新闻标签和兴趣标签，供新闻内容和用户画像复用。</p>
        </div>
        <button type="button" className="primary-button" onClick={onCreate}>
          新增标签
        </button>
      </div>

      <div className="admin-news-table-wrap">
        <table className="admin-news-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>编码</th>
              <th>所属分类</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={6}>加载中...</td>
              </tr>
            ) : tags.length === 0 ? (
              <tr>
                <td colSpan={6}>暂无标签数据</td>
              </tr>
            ) : (
              tags.map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td>{item.code}</td>
                  <td>{categoryMap.get(item.categoryId) ?? '-'}</td>
                  <td>{item.sortOrder}</td>
                  <td>
                    <span className={item.status === 1 ? 'status-pill online' : 'status-pill offline'}>
                      {item.status === 1 ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button type="button" className="ghost-button" onClick={() => onEdit(item)}>
                        编辑
                      </button>
                      <button type="button" className="ghost-button" onClick={() => onToggleStatus(item)}>
                        {item.status === 1 ? '停用' : '启用'}
                      </button>
                      <button type="button" className="ghost-button danger" onClick={() => onDelete(item.id)}>
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
  );
}

function UserSection({
  filters,
  pageData,
  isLoading,
  onChangeFilters,
  onQuery,
  onPageChange,
  onToggleStatus,
}: {
  filters: UserFilterState;
  pageData: PageResult<AdminUserItem>;
  isLoading: boolean;
  onChangeFilters: React.Dispatch<React.SetStateAction<UserFilterState>>;
  onQuery: () => void;
  onPageChange: (page: number) => void;
  onToggleStatus: (item: AdminUserItem) => void;
}) {
  return (
    <>
      <div className="admin-toolbar-row">
        <div>
          <h2>用户管理</h2>
          <p className="section-description">查看用户列表，并控制账户启用/禁用状态。</p>
        </div>
      </div>

      <form
        className="admin-toolbar-row admin-filter-row"
        onSubmit={(event) => {
          event.preventDefault();
          onQuery();
        }}
      >
        <input
          className="toolbar-input"
          value={filters.keyword}
          onChange={(event) => onChangeFilters((current) => ({ ...current, keyword: event.target.value }))}
          placeholder="搜索用户名、昵称、邮箱或手机号"
        />
        <select
          className="toolbar-select"
          value={filters.role}
          onChange={(event) => onChangeFilters((current) => ({ ...current, role: event.target.value }))}
        >
          <option value="">全部角色</option>
          <option value="USER">普通用户</option>
          <option value="ADMIN">管理员</option>
        </select>
        <select
          className="toolbar-select"
          value={filters.status}
          onChange={(event) => onChangeFilters((current) => ({ ...current, status: event.target.value }))}
        >
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <button type="submit" className="ghost-button">
          查询
        </button>
      </form>

      <div className="admin-news-table-wrap">
        <table className="admin-news-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>邮箱</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7}>加载中...</td>
              </tr>
            ) : pageData.records.length === 0 ? (
              <tr>
                <td colSpan={7}>暂无用户数据</td>
              </tr>
            ) : (
              pageData.records.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nickname || item.username}</strong>
                    <p>{item.username}</p>
                  </td>
                  <td>{item.email || '-'}</td>
                  <td>{item.phone || '-'}</td>
                  <td>{item.role}</td>
                  <td>
                    <span className={item.status === 1 ? 'status-pill online' : 'status-pill offline'}>
                      {item.status === 1 ? '启用' : '禁用'}
                    </span>
                  </td>
                  <td>{formatDisplayDate(item.createdAt)}</td>
                  <td>
                    <button type="button" className="ghost-button" onClick={() => onToggleStatus(item)}>
                      {item.status === 1 ? '禁用' : '启用'}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <PaginationBar page={pageData.page} total={pageData.total} size={pageData.size} onPageChange={onPageChange} />
    </>
  );
}

function CrawlSection({
  crawlConfigs,
  isLoading,
  runningCrawlId,
  onCreate,
  onEdit,
  onDelete,
  onToggleStatus,
  onRun,
}: {
  crawlConfigs: AdminCrawlConfigItem[];
  isLoading: boolean;
  runningCrawlId: number | null;
  onCreate: () => void;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
  onToggleStatus: (item: AdminCrawlConfigItem) => void;
  onRun: (id: number) => void;
}) {
  return (
    <>
      <div className="admin-toolbar-row">
        <div>
          <h2>采集管理</h2>
          <p className="section-description">管理 RSS 采集源，查看最近采集结果，并支持手动触发采集。</p>
        </div>
        <button type="button" className="primary-button" onClick={onCreate}>
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
            {isLoading ? (
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
                      <button type="button" className="ghost-button" onClick={() => onEdit(item.id)}>
                        编辑
                      </button>
                      <button type="button" className="ghost-button" onClick={() => onToggleStatus(item)}>
                        {item.enabled === 1 ? '停用' : '启用'}
                      </button>
                      <button
                        type="button"
                        className="ghost-button"
                        disabled={runningCrawlId === item.id}
                        onClick={() => onRun(item.id)}
                      >
                        {runningCrawlId === item.id ? '采集中...' : '手动采集'}
                      </button>
                      <button type="button" className="ghost-button danger" onClick={() => onDelete(item.id)}>
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
  );
}

function PaginationBar({
  page,
  total,
  size,
  onPageChange,
}: {
  page: number;
  total: number;
  size: number;
  onPageChange: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="pagination-bar">
      <button type="button" className="ghost-button" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
        上一页
      </button>
      <span>
        第 {page} / {totalPages} 页
      </span>
      <button
        type="button"
        className="ghost-button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        下一页
      </button>
    </div>
  );
}

function ModalFrame({
  eyebrow,
  title,
  onClose,
  children,
}: React.PropsWithChildren<{
  eyebrow: string;
  title: string;
  onClose: () => void;
}>) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div className="auth-modal admin-form-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="auth-header">
          <div>
            {eyebrow ? <p className="auth-eyebrow">{eyebrow}</p> : null}
            <h2>{title}</h2>
          </div>
          <button type="button" className="ghost-button" onClick={onClose}>
            关闭
          </button>
        </div>
        {children}
      </div>
    </div>
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
