import { useEffect, useMemo, useState, type Dispatch, type FormEvent, type SetStateAction } from 'react';
import { Link } from 'react-router-dom';
import { fetchTags } from '../api/metadata';
import {
  fetchProfileComments,
  fetchProfileFavorites,
  fetchProfileHistory,
  fetchProfileInterests,
  fetchProfileLikes,
  fetchProfileSummary,
  updateProfileBasic,
  updateProfileInterests,
  updateProfilePassword,
  uploadProfileAvatar,
} from '../api/profile';
import { useAuth } from '../store';
import type {
  PageResult,
  PasswordUpdateRequest,
  ProfileCommentItem,
  ProfileNewsItem,
  ProfileSummary,
  ProfileUpdateRequest,
  TagOption,
} from '../types';
import { openAuthDialog } from '../utils/authDialog';
import { getErrorMessage } from '../utils/request';

type ProfileTab = 'summary' | 'interests' | 'history' | 'favorites' | 'likes' | 'comments' | 'settings';

const tabItems: Array<{ key: ProfileTab; label: string }> = [
  { key: 'summary', label: '个人资料' },
  { key: 'interests', label: '兴趣标签' },
  { key: 'history', label: '浏览记录' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'likes', label: '我的点赞' },
  { key: 'comments', label: '我的评论' },
  { key: 'settings', label: '账号设置' },
];

const initialProfileForm: ProfileUpdateRequest = {
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
};

const initialPasswordForm: PasswordUpdateRequest = {
  currentPassword: '',
  newPassword: '',
};

function ProfilePage() {
  const { currentUser, isAuthenticated, isBootstrapping, refreshCurrentUser, signOut } = useAuth();
  const [activeTab, setActiveTab] = useState<ProfileTab>('summary');
  const [summary, setSummary] = useState<ProfileSummary | null>(null);
  const [profileForm, setProfileForm] = useState<ProfileUpdateRequest>(initialProfileForm);
  const [passwordForm, setPasswordForm] = useState<PasswordUpdateRequest>(initialPasswordForm);
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
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isSavingInterests, setIsSavingInterests] = useState(false);
  const [isSavingPassword, setIsSavingPassword] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const totalPages = useMemo(() => {
    const target = activeTab === 'comments' ? commentPageData : newsPageData;
    return Math.max(1, Math.ceil(target.total / target.size));
  }, [activeTab, commentPageData, newsPageData]);

  const groupedTags = useMemo(() => {
    const tagMap = new Map<number, TagOption[]>();
    for (const tag of allTags) {
      const current = tagMap.get(tag.categoryId) ?? [];
      current.push(tag);
      tagMap.set(tag.categoryId, current);
    }
    return Array.from(tagMap.entries());
  }, [allTags]);

  const selectedInterestTags = useMemo(
    () => allTags.filter((tag) => selectedInterestIds.includes(tag.id)),
    [allTags, selectedInterestIds],
  );

  const applySummary = (nextSummary: ProfileSummary) => {
    setSummary(nextSummary);
    setProfileForm({
      nickname: nextSummary.nickname || '',
      email: nextSummary.email || '',
      phone: nextSummary.phone || '',
      avatar: nextSummary.avatar || '',
    });
  };

  const loadSummary = async () => {
    const result = await fetchProfileSummary();
    applySummary(result.data);
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
        await loadSummary();
        return;
      }

      if (tab === 'interests') {
        await loadInterests();
        return;
      }

      if (tab === 'settings') {
        if (!summary) {
          await loadSummary();
        }
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
      window.dispatchEvent(new Event('app:interests-updated'));
      setSuccessMessage('兴趣标签已保存');
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '兴趣标签保存失败'));
    } finally {
      setIsSavingInterests(false);
    }
  };

  const handleAvatarFileChange = async (file: File | null) => {
    if (!file) {
      return;
    }

    setIsUploadingAvatar(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const result = await uploadProfileAvatar(file);
      setProfileForm((current) => ({ ...current, avatar: result.data }));
      setSuccessMessage('头像已上传，记得保存个人资料');
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '头像上传失败'));
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleSaveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSavingProfile(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await updateProfileBasic({
        ...profileForm,
        phone: profileForm.phone?.trim() || undefined,
        avatar: profileForm.avatar?.trim() || undefined,
      });
      await refreshCurrentUser();
      await loadSummary();
      setSuccessMessage('个人资料已更新');
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '个人资料更新失败'));
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleUpdatePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSavingPassword(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await updateProfilePassword(passwordForm);
      setPasswordForm(initialPasswordForm);
      setSuccessMessage('密码修改成功');
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '密码修改失败'));
    } finally {
      setIsSavingPassword(false);
    }
  };

  if (isBootstrapping) {
    return <div className="page-card news-state-card">正在恢复登录状态...</div>;
  }

  if (!isAuthenticated || !currentUser) {
    return (
      <div className="page-card news-state-card">
        <h1>请先登录后查看个人中心</h1>
        <p className="page-description">登录后可以查看浏览记录、收藏、点赞、评论，以及管理自己的资料和兴趣标签。</p>
        <button type="button" className="primary-button" onClick={() => openAuthDialog('login')}>
          去登录
        </button>
      </div>
    );
  }

  const displayName = summary?.nickname || currentUser.nickname || currentUser.username;
  const avatarValue = profileForm.avatar || summary?.avatar || currentUser.avatar || '';

  return (
    <section className="profile-page">
      {errorMessage ? <p className="auth-feedback error">{errorMessage}</p> : null}
      {successMessage ? <p className="auth-feedback success">{successMessage}</p> : null}

      <div className="page-card profile-hero-card">
        <div className="profile-hero-main">
          <div className="profile-avatar-stack">
            <AvatarPreview name={displayName} avatar={avatarValue} large />
            <button type="button" className="ghost-button" onClick={() => setActiveTab('summary')}>
              编辑头像和资料
            </button>
          </div>

          <div className="profile-hero-copy">
            <h1>{displayName}</h1>
            <p>{summary?.email || currentUser.email || '暂未填写邮箱'}</p>
            <div className="profile-hero-meta">
              <span className="news-category-chip">{summary?.role || currentUser.role}</span>
              <span>用户名 {summary?.username || currentUser.username}</span>
              <span>{summary?.phone || currentUser.phone || '暂未填写手机号'}</span>
            </div>
          </div>

          <div className="profile-hero-actions">
            <button type="button" className="primary-button" onClick={() => setActiveTab('summary')}>
              修改资料
            </button>
            <button type="button" className="ghost-button" onClick={() => setActiveTab('settings')}>
              账号设置
            </button>
          </div>
        </div>

        <div className="profile-stats-grid profile-hero-stats">
          <StatCard label="浏览" value={summary?.historyCount ?? 0} />
          <StatCard label="点赞" value={summary?.likeCount ?? 0} />
          <StatCard label="收藏" value={summary?.favoriteCount ?? 0} />
          <StatCard label="评论" value={summary?.commentCount ?? 0} />
        </div>
      </div>

      <div className="page-card profile-shell">
        <aside className="profile-sidebar">
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
          {activeTab === 'summary' ? (
            <ProfileSummaryPanel
              summary={summary}
              profileForm={profileForm}
              isLoading={isLoading}
              isSavingProfile={isSavingProfile}
              isUploadingAvatar={isUploadingAvatar}
              onProfileFormChange={setProfileForm}
              onAvatarFileChange={handleAvatarFileChange}
              onSaveProfile={handleSaveProfile}
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
          ) : activeTab === 'settings' ? (
            <ProfileSettingsPanel
              passwordForm={passwordForm}
              isSavingPassword={isSavingPassword}
              onPasswordFormChange={setPasswordForm}
              onUpdatePassword={handleUpdatePassword}
              onSignOut={() => void signOut()}
            />
          ) : (
            <ProfileNewsPanel pageData={newsPageData} isLoading={isLoading} emptyLabel={activeTab} />
          )}

          {activeTab !== 'summary' && activeTab !== 'interests' && activeTab !== 'settings' ? (
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
  profileForm,
  isLoading,
  isSavingProfile,
  isUploadingAvatar,
  onProfileFormChange,
  onAvatarFileChange,
  onSaveProfile,
}: {
  summary: ProfileSummary | null;
  profileForm: ProfileUpdateRequest;
  isLoading: boolean;
  isSavingProfile: boolean;
  isUploadingAvatar: boolean;
  onProfileFormChange: Dispatch<SetStateAction<ProfileUpdateRequest>>;
  onAvatarFileChange: (file: File | null) => Promise<void>;
  onSaveProfile: (event: FormEvent<HTMLFormElement>) => void;
}) {
  if (isLoading && !summary) {
    return <div className="news-state-card">正在加载个人资料...</div>;
  }

  if (!summary) {
    return <div className="news-state-card">暂时无法获取个人资料。</div>;
  }

  return (
    <div className="profile-summary-panel">
      <div className="profile-panel-header">
        <h2>个人资料</h2>
        <p>头像、昵称、邮箱和手机号都可以在这里统一修改。</p>
      </div>

      <div className="profile-edit-layout">
        <div className="profile-avatar-editor">
          <AvatarPreview name={summary.nickname || summary.username} avatar={profileForm.avatar || summary.avatar || ''} />
          <div className="profile-avatar-editor-copy">
            <strong>头像预览</strong>
            <span>支持直接上传头像图片，也可以继续填写图片地址。</span>
          </div>
          <label className="avatar-upload-field">
            <span>{isUploadingAvatar ? '正在上传头像...' : '选择本地图片'}</span>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              disabled={isUploadingAvatar}
              onChange={(event) => void onAvatarFileChange(event.target.files?.[0] ?? null)}
            />
          </label>
        </div>

        <form className="auth-form admin-news-form" onSubmit={onSaveProfile}>
          <label>
            昵称
            <input
              value={profileForm.nickname}
              onChange={(event) => onProfileFormChange((current) => ({ ...current, nickname: event.target.value }))}
              required
            />
          </label>
          <div className="admin-form-grid">
            <label>
              邮箱
              <input
                type="email"
                value={profileForm.email}
                onChange={(event) => onProfileFormChange((current) => ({ ...current, email: event.target.value }))}
                required
              />
            </label>
            <label>
              手机号
              <input
                value={profileForm.phone ?? ''}
                onChange={(event) => onProfileFormChange((current) => ({ ...current, phone: event.target.value }))}
                placeholder="选填"
              />
            </label>
          </div>
          <label>
            头像地址
            <input
              value={profileForm.avatar ?? ''}
              onChange={(event) => onProfileFormChange((current) => ({ ...current, avatar: event.target.value }))}
              placeholder="https://example.com/avatar.png"
            />
          </label>
          <button type="submit" className="primary-button" disabled={isSavingProfile || isUploadingAvatar}>
            {isSavingProfile ? '保存中...' : '保存个人资料'}
          </button>
        </form>
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
      <div className="profile-panel-header">
        <h2>兴趣标签</h2>
        <p>选择你感兴趣的内容方向，推荐流会优先匹配这些标签。</p>
      </div>

      {selectedInterestTags.length > 0 ? (
        <div className="profile-interest-preview-card">
          <strong>已选择的标签</strong>
          <div className="news-tag-list">
            {selectedInterestTags.map((tag) => (
              <span key={tag.id} className="news-tag">
                {tag.name}
              </span>
            ))}
          </div>
        </div>
      ) : (
        <div className="news-state-card compact-empty-state">你还没有选择兴趣标签。</div>
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

function ProfileSettingsPanel({
  passwordForm,
  isSavingPassword,
  onPasswordFormChange,
  onUpdatePassword,
  onSignOut,
}: {
  passwordForm: PasswordUpdateRequest;
  isSavingPassword: boolean;
  onPasswordFormChange: Dispatch<SetStateAction<PasswordUpdateRequest>>;
  onUpdatePassword: (event: FormEvent<HTMLFormElement>) => void;
  onSignOut: () => void;
}) {
  return (
    <div className="profile-summary-panel">
      <div className="profile-panel-header">
        <h2>账号设置</h2>
        <p>这里用于修改密码和退出当前账号。</p>
      </div>

      <form className="auth-form admin-news-form" onSubmit={onUpdatePassword}>
        <label>
          当前密码
          <input
            type="password"
            value={passwordForm.currentPassword}
            onChange={(event) =>
              onPasswordFormChange((current) => ({ ...current, currentPassword: event.target.value }))
            }
            required
          />
        </label>
        <label>
          新密码
          <input
            type="password"
            value={passwordForm.newPassword}
            onChange={(event) =>
              onPasswordFormChange((current) => ({ ...current, newPassword: event.target.value }))
            }
            required
          />
        </label>
        <button type="submit" className="primary-button" disabled={isSavingPassword}>
          {isSavingPassword ? '提交中...' : '修改密码'}
        </button>
      </form>

      <div className="profile-signout-card">
        <div>
          <strong>退出登录</strong>
          <p>如果你需要切换账号，可以在这里安全退出当前登录状态。</p>
        </div>
        <button type="button" className="ghost-button" onClick={onSignOut}>
          退出登录
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
  emptyLabel: Exclude<ProfileTab, 'summary' | 'interests' | 'comments' | 'settings'>;
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
    return <div className="news-state-card">你还没有留下评论。</div>;
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

function AvatarPreview({
  name,
  avatar,
  large = false,
}: {
  name: string;
  avatar: string;
  large?: boolean;
}) {
  const initials = getInitials(name);
  const className = large ? 'profile-avatar-preview large' : 'profile-avatar-preview';

  return <div className={className}>{avatar ? <img src={avatar} alt={name} /> : <span>{initials}</span>}</div>;
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
    return '你还没有浏览记录。';
  }
  if (tab === 'favorites') {
    return '你还没有收藏新闻。';
  }
  return '你还没有点赞记录。';
}

function getInitials(value: string) {
  const normalized = value.trim();
  if (!normalized) {
    return 'U';
  }
  if (/^[A-Za-z0-9 ]+$/.test(normalized)) {
    const initials = normalized
      .split(/\s+/)
      .slice(0, 2)
      .map((segment) => segment[0]?.toUpperCase() ?? '')
      .join('');
    return initials || normalized.slice(0, 2).toUpperCase();
  }
  return normalized.slice(0, 2).toUpperCase();
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

export default ProfilePage;
