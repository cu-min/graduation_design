import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchTags } from '../api/metadata';
import {
  fetchProfileComments,
  fetchProfileFavorites,
  fetchProfileHistory,
  fetchProfileInterests,
  fetchProfileLikes,
  fetchProfileSummary,
  updateProfileInterests,
} from '../api/profile';
import { useAuth } from '../store';
import type {
  PageResult,
  ProfileCommentItem,
  ProfileNewsItem,
  ProfileSummary,
  TagOption,
} from '../types';
import { openAuthDialog } from '../utils/authDialog';
import { getErrorMessage } from '../utils/request';

type ProfileTab = 'summary' | 'interests' | 'history' | 'favorites' | 'likes' | 'comments';

const tabItems: { key: ProfileTab; label: string }[] = [
  { key: 'summary', label: '我的资料' },
  { key: 'interests', label: '兴趣标签' },
  { key: 'history', label: '浏览历史' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'likes', label: '我的点赞' },
  { key: 'comments', label: '我的评论' },
];

function ProfilePage() {
  const { currentUser, isAuthenticated, isBootstrapping } = useAuth();
  const [activeTab, setActiveTab] = useState<ProfileTab>('summary');
  const [summary, setSummary] = useState<ProfileSummary | null>(null);
  const [allTags, setAllTags] = useState<TagOption[]>([]);
  const [selectedInterestIds, setSelectedInterestIds] = useState<number[]>([]);
  const [newsPageData, setNewsPageData] = useState<PageResult<ProfileNewsItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 6,
  });
  const [commentPageData, setCommentPageData] = useState<PageResult<ProfileCommentItem>>({
    records: [],
    total: 0,
    page: 1,
    size: 6,
  });
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSavingInterests, setIsSavingInterests] = useState(false);

  const totalPages = useMemo(() => {
    const target = activeTab === 'comments' ? commentPageData : newsPageData;
    return Math.max(1, Math.ceil(target.total / target.size));
  }, [activeTab, commentPageData, newsPageData]);

  const groupedTags = useMemo(() => {
    const map = new Map<number, TagOption[]>();
    for (const tag of allTags) {
      const current = map.get(tag.categoryId) ?? [];
      current.push(tag);
      map.set(tag.categoryId, current);
    }
    return Array.from(map.entries());
  }, [allTags]);

  const selectedInterestTags = useMemo(
    () => allTags.filter((tag) => selectedInterestIds.includes(tag.id)),
    [allTags, selectedInterestIds],
  );

  const loadSummary = async () => {
    const result = await fetchProfileSummary();
    setSummary(result.data);
  };

  const loadInterests = async () => {
    const [allTagResult, interestResult] = await Promise.all([fetchTags(), fetchProfileInterests()]);
    setAllTags(allTagResult.data);
    setSelectedInterestIds(interestResult.data.map((tag) => tag.id));
  };

  const loadTabData = async (tab: ProfileTab, page = 1) => {
    setIsLoading(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      if (tab === 'summary') {
        await Promise.all([loadSummary(), loadInterests()]);
        return;
      }

      if (tab === 'interests') {
        await loadInterests();
        return;
      }

      if (tab === 'history') {
        const result = await fetchProfileHistory({ page, size: newsPageData.size });
        setNewsPageData(result.data);
        return;
      }

      if (tab === 'favorites') {
        const result = await fetchProfileFavorites({ page, size: newsPageData.size });
        setNewsPageData(result.data);
        return;
      }

      if (tab === 'likes') {
        const result = await fetchProfileLikes({ page, size: newsPageData.size });
        setNewsPageData(result.data);
        return;
      }

      const result = await fetchProfileComments({ page, size: commentPageData.size });
      setCommentPageData(result.data);
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '个人中心数据加载失败'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    void loadTabData(activeTab, 1);
  }, [activeTab, isAuthenticated]);

  const handleInterestToggle = (tagId: number, checked: boolean) => {
    setSelectedInterestIds((current) =>
      checked ? Array.from(new Set([...current, tagId])) : current.filter((id) => id !== tagId),
    );
  };

  const handleSaveInterests = async () => {
    setIsSavingInterests(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await updateProfileInterests({ tagIds: selectedInterestIds });
      setSuccessMessage('兴趣标签已保存');
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '兴趣标签保存失败'));
    } finally {
      setIsSavingInterests(false);
    }
  };

  if (isBootstrapping) {
    return <div className="page-card news-state-card">正在恢复登录状态...</div>;
  }

  if (!isAuthenticated || !currentUser) {
    return (
      <div className="page-card news-state-card">
        <p className="page-eyebrow">阶段 9</p>
        <h1>请先登录后查看个人中心</h1>
        <p className="page-description">
          登录后可以查看你的浏览历史、收藏、点赞、评论记录，并管理兴趣标签和基础用户画像。
        </p>
        <button type="button" className="primary-button" onClick={() => openAuthDialog('login')}>
          去登录
        </button>
      </div>
    );
  }

  return (
    <section className="profile-page">
      <div className="page-card profile-shell">
        <aside className="profile-sidebar">
          <div className="profile-user-card">
            <p className="page-eyebrow">阶段 9</p>
            <h1>{currentUser.nickname || currentUser.username}</h1>
            <p>{currentUser.email || '未设置邮箱'}</p>
            <span className="news-category-chip">{currentUser.role}</span>
          </div>

          <div className="profile-tab-list">
            {tabItems.map((item) => (
              <button
                key={item.key}
                type="button"
                className={activeTab === item.key ? 'profile-tab active' : 'profile-tab'}
                onClick={() => setActiveTab(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>
        </aside>

        <div className="profile-content">
          {errorMessage ? <p className="auth-feedback error">{errorMessage}</p> : null}
          {successMessage ? <p className="auth-feedback success">{successMessage}</p> : null}

          {activeTab === 'summary' ? (
            <ProfileSummaryPanel
              summary={summary}
              selectedInterestTags={selectedInterestTags}
              isLoading={isLoading}
            />
          ) : activeTab === 'interests' ? (
            <ProfileInterestsPanel
              groupedTags={groupedTags}
              selectedInterestIds={selectedInterestIds}
              selectedInterestTags={selectedInterestTags}
              isLoading={isLoading}
              isSaving={isSavingInterests}
              onToggle={handleInterestToggle}
              onSave={() => void handleSaveInterests()}
            />
          ) : activeTab === 'comments' ? (
            <ProfileCommentPanel pageData={commentPageData} isLoading={isLoading} />
          ) : (
            <ProfileNewsPanel pageData={newsPageData} isLoading={isLoading} emptyLabel={activeTab} />
          )}

          {activeTab !== 'summary' && activeTab !== 'interests' ? (
            <div className="pagination-bar home-pagination">
              <button
                type="button"
                className="ghost-button"
                disabled={(activeTab === 'comments' ? commentPageData.page : newsPageData.page) <= 1 || isLoading}
                onClick={() =>
                  void loadTabData(
                    activeTab,
                    (activeTab === 'comments' ? commentPageData.page : newsPageData.page) - 1,
                  )
                }
              >
                上一页
              </button>
              <span>
                第 {activeTab === 'comments' ? commentPageData.page : newsPageData.page} / {totalPages} 页
              </span>
              <button
                type="button"
                className="ghost-button"
                disabled={(activeTab === 'comments' ? commentPageData.page : newsPageData.page) >= totalPages || isLoading}
                onClick={() =>
                  void loadTabData(
                    activeTab,
                    (activeTab === 'comments' ? commentPageData.page : newsPageData.page) + 1,
                  )
                }
              >
                下一页
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}

function ProfileSummaryPanel({
  summary,
  selectedInterestTags,
  isLoading,
}: {
  summary: ProfileSummary | null;
  selectedInterestTags: TagOption[];
  isLoading: boolean;
}) {
  if (isLoading && !summary) {
    return <div className="news-state-card">正在加载个人资料...</div>;
  }

  if (!summary) {
    return <div className="news-state-card">暂时无法获取个人资料。</div>;
  }

  return (
    <div className="profile-summary-panel">
      <div className="section-heading compact">
        <div>
          <p className="page-eyebrow">账户信息</p>
          <h2>我的资料</h2>
        </div>
      </div>

      <div className="profile-info-grid">
        <div className="profile-info-card">
          <span>用户名</span>
          <strong>{summary.username}</strong>
        </div>
        <div className="profile-info-card">
          <span>昵称</span>
          <strong>{summary.nickname || summary.username}</strong>
        </div>
        <div className="profile-info-card">
          <span>邮箱</span>
          <strong>{summary.email || '未设置'}</strong>
        </div>
        <div className="profile-info-card">
          <span>角色</span>
          <strong>{summary.role}</strong>
        </div>
      </div>

      <div className="profile-stats-grid">
        <StatCard label="浏览数量" value={summary.historyCount} />
        <StatCard label="点赞数量" value={summary.likeCount} />
        <StatCard label="收藏数量" value={summary.favoriteCount} />
        <StatCard label="评论数量" value={summary.commentCount} />
      </div>

      <div className="profile-interest-preview">
        <div className="section-heading compact">
          <div>
            <p className="page-eyebrow">用户画像</p>
            <h2>兴趣与行为概况</h2>
          </div>
        </div>
        {selectedInterestTags.length === 0 ? (
          <div className="news-state-card compact-empty-state">
            你还没有选择兴趣标签。可以前往“兴趣标签”页签完善偏好，系统会更容易给出贴合你的推荐结果。
          </div>
        ) : (
          <>
            <div className="news-tag-list">
              {selectedInterestTags.map((tag) => (
                <span key={tag.id} className="news-tag">
                  {tag.name}
                </span>
              ))}
            </div>
            <p className="section-meta">
              当前已选择 {selectedInterestTags.length} 个兴趣标签，系统会优先推荐命中这些标签，同时结合你的浏览、点赞、收藏、评论行为进行排序。
            </p>
          </>
        )}
      </div>
    </div>
  );
}

function ProfileInterestsPanel({
  groupedTags,
  selectedInterestIds,
  selectedInterestTags,
  isLoading,
  isSaving,
  onToggle,
  onSave,
}: {
  groupedTags: [number, TagOption[]][];
  selectedInterestIds: number[];
  selectedInterestTags: TagOption[];
  isLoading: boolean;
  isSaving: boolean;
  onToggle: (tagId: number, checked: boolean) => void;
  onSave: () => void;
}) {
  if (isLoading && groupedTags.length === 0) {
    return <div className="news-state-card">正在加载兴趣标签...</div>;
  }

  return (
    <div className="profile-summary-panel">
      <div className="section-heading compact">
        <div>
          <p className="page-eyebrow">兴趣管理</p>
          <h2>兴趣标签</h2>
        </div>
      </div>

      <p className="section-meta">选择你感兴趣的标签，系统会优先推荐命中这些标签的新闻内容。</p>

      {selectedInterestTags.length > 0 ? (
        <div className="profile-interest-preview-card">
          <span className="section-meta">当前已选择</span>
          <div className="news-tag-list">
            {selectedInterestTags.map((tag) => (
              <span key={tag.id} className="news-tag">
                {tag.name}
              </span>
            ))}
          </div>
        </div>
      ) : (
        <div className="news-state-card compact-empty-state">你还没有选择兴趣标签，建议先选几个以获得更稳定的个性化推荐。</div>
      )}

      <div className="interest-group-list">
        {groupedTags.map(([categoryId, tags]) => (
          <div key={categoryId} className="interest-group-card">
            <div className="news-tag-list">
              {tags.map((tag) => (
                <label key={tag.id} className="tag-option profile-tag-option">
                  <input
                    type="checkbox"
                    checked={selectedInterestIds.includes(tag.id)}
                    onChange={(event) => onToggle(tag.id, event.target.checked)}
                  />
                  <span>{tag.name}</span>
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="news-detail-footer">
        <button type="button" className="primary-button" disabled={isSaving} onClick={onSave}>
          {isSaving ? '保存中...' : '保存兴趣标签'}
        </button>
      </div>
    </div>
  );
}

function ProfileNewsPanel({
  pageData,
  isLoading,
  emptyLabel,
}: {
  pageData: PageResult<ProfileNewsItem>;
  isLoading: boolean;
  emptyLabel: Exclude<ProfileTab, 'summary' | 'interests' | 'comments'>;
}) {
  if (isLoading && pageData.records.length === 0) {
    return <div className="news-state-card">正在加载列表...</div>;
  }

  if (pageData.records.length === 0) {
    return <div className="news-state-card">{getProfileNewsEmptyText(emptyLabel)}</div>;
  }

  return (
    <div className="profile-news-list">
      {pageData.records.map((item) => (
        <article key={`${item.newsId}-${item.behaviorTime}`} className="news-card compact">
          <Link to={`/news/${item.newsId}`} className="news-card-main">
            <div className="news-cover">
              {item.coverImage ? <img src={item.coverImage} alt={item.title} /> : <span>{item.title.slice(0, 18)}</span>}
            </div>
            <div className="news-card-body">
              <div className="news-card-topline">
                <span className="news-category-chip">{item.categoryName}</span>
                <span className="news-time">{formatDisplayDate(item.behaviorTime)}</span>
              </div>
              <h3>{item.title}</h3>
              <p>{item.summary}</p>
            </div>
          </Link>
          <div className="news-card-footer">
            <span>{item.sourceName}</span>
            <div className="news-metrics">
              <span>发布时间 {formatDisplayDate(item.publishTime)}</span>
              <span>热度 {item.heatScore}</span>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}

function ProfileCommentPanel({
  pageData,
  isLoading,
}: {
  pageData: PageResult<ProfileCommentItem>;
  isLoading: boolean;
}) {
  if (isLoading && pageData.records.length === 0) {
    return <div className="news-state-card">正在加载评论记录...</div>;
  }

  if (pageData.records.length === 0) {
    return <div className="news-state-card">你还没有留下评论，可以去新闻详情页发表第一条看法。</div>;
  }

  return (
    <div className="profile-comment-list">
      {pageData.records.map((item) => (
        <article key={item.id} className="comment-card">
          <div className="comment-header">
            <div>
              <strong>{item.parentId ? '回复评论' : '一级评论'}</strong>
              <span>{formatDisplayDate(item.createdAt)}</span>
            </div>
            <Link to={`/news/${item.newsId}`} className="ghost-button">
              查看新闻
            </Link>
          </div>
          <p>{item.content}</p>
          <div className="section-meta">所属新闻：{item.newsTitle}</div>
        </article>
      ))}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="profile-stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function getProfileNewsEmptyText(tab: 'history' | 'favorites' | 'likes') {
  if (tab === 'history') {
    return '你还没有浏览记录，先去首页看看感兴趣的新闻吧。';
  }
  if (tab === 'favorites') {
    return '你还没有收藏新闻，可以在详情页点击收藏后再回来查看。';
  }
  return '你还没有点赞记录，可以在详情页给喜欢的内容点个赞。';
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

export default ProfilePage;
