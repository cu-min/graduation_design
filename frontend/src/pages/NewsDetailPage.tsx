import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  dislikeNews,
  favoriteNews,
  likeNews,
  shareNews,
  unfavoriteNews,
  unlikeNews,
} from '../api/behavior';
import {
  createNewsComment,
  deleteComment,
  fetchNewsComments,
  replyComment,
} from '../api/comments';
import {
  DislikeIcon,
  ShareOneIcon,
  StarIcon,
  ThumbsUpIcon,
} from '../components/icons/NewsDetailIcons';
import { fetchNewsDetail, fetchRelatedNews } from '../api/news';
import { useAuth } from '../store';
import type {
  CommentItem,
  NewsActionStatus,
  NewsDetail,
  RelatedNewsItem,
} from '../types';
import { openAuthDialog } from '../utils/authDialog';
import { getDisplayNewsCover, getNewsCoverFallback } from '../utils/newsCover';
import { getErrorMessage } from '../utils/request';

type CommentSortMode = 'time' | 'hot';

function NewsDetailPage() {
  const { id } = useParams();
  const { isAuthenticated, currentUser } = useAuth();
  const [news, setNews] = useState<NewsDetail | null>(null);
  const [relatedNews, setRelatedNews] = useState<RelatedNewsItem[]>([]);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRelatedLoading, setIsRelatedLoading] = useState(true);
  const [isCommentsLoading, setIsCommentsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [commentText, setCommentText] = useState('');
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [replyingCommentId, setReplyingCommentId] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionFeedback, setActionFeedback] = useState('');
  const [actionFeedbackType, setActionFeedbackType] = useState<'success' | 'error'>('success');
  const [isShareCopied, setIsShareCopied] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [commentSortMode, setCommentSortMode] = useState<CommentSortMode>('time');

  const newsId = id ? Number(id) : NaN;
  const displayCommentCount = comments.length > 0 || !isCommentsLoading ? countComments(comments) : news?.commentCount ?? 0;
  const sortedComments = useMemo(() => sortComments(comments, commentSortMode), [comments, commentSortMode]);

  const showActionFeedback = (message: string, type: 'success' | 'error') => {
    setActionFeedback(message);
    setActionFeedbackType(type);
  };

  const showToast = (message: string) => {
    setToastMessage(message);
    window.setTimeout(() => setToastMessage(''), 2200);
  };

  const syncCommentCount = (nextComments: CommentItem[]) => {
    const nextCommentCount = countComments(nextComments);
    setNews((current) => (current ? { ...current, commentCount: nextCommentCount } : current));
  };

  const loadNewsDetail = async () => {
    if (!newsId || Number.isNaN(newsId)) {
      setErrorMessage('新闻 ID 无效');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const result = await fetchNewsDetail(newsId);
      setNews(result.data);
    } catch (error) {
      setNews(null);
      setErrorMessage(getErrorMessage(error, '新闻详情加载失败，请稍后重试'));
    } finally {
      setIsLoading(false);
    }
  };

  const loadRelatedNews = async () => {
    if (!newsId || Number.isNaN(newsId)) {
      setRelatedNews([]);
      setIsRelatedLoading(false);
      return;
    }

    setIsRelatedLoading(true);
    try {
      const result = await fetchRelatedNews(newsId, 4);
      setRelatedNews(result.data);
    } catch {
      setRelatedNews([]);
    } finally {
      setIsRelatedLoading(false);
    }
  };

  const loadComments = async () => {
    if (!newsId || Number.isNaN(newsId)) {
      setIsCommentsLoading(false);
      return [];
    }

    setIsCommentsLoading(true);
    try {
      const result = await fetchNewsComments(newsId);
      setComments(result.data);
      return result.data;
    } catch {
      setComments([]);
      return [];
    } finally {
      setIsCommentsLoading(false);
    }
  };

  useEffect(() => {
    void Promise.all([loadNewsDetail(), loadRelatedNews(), loadComments()]);
  }, [id, isAuthenticated]);

  const requireLogin = () => {
    showActionFeedback('请先登录后再进行互动操作', 'error');
    openAuthDialog('login');
  };

  const applyActionStatus = (status: NewsActionStatus) => {
    setNews((current) =>
      current
        ? {
            ...current,
            liked: status.liked,
            favorited: status.favorited,
            disliked: status.disliked,
            likeCount: status.likeCount,
            favoriteCount: status.favoriteCount,
          }
        : current,
    );
  };

  const handleLike = async () => {
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    try {
      const result = news.liked ? await unlikeNews(news.id) : await likeNews(news.id);
      applyActionStatus(result.data);
      setActionFeedback('');
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '点赞操作失败'), 'error');
    }
  };

  const handleFavorite = async () => {
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    try {
      const result = news.favorited ? await unfavoriteNews(news.id) : await favoriteNews(news.id);
      applyActionStatus(result.data);
      setActionFeedback('');
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '收藏操作失败'), 'error');
    }
  };

  const handleDislike = async () => {
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    try {
      const result = await dislikeNews(news.id);
      applyActionStatus(result.data);
      showActionFeedback('已减少此类内容推荐', 'success');
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '不感兴趣操作失败'), 'error');
    }
  };

  const handleShare = async () => {
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    try {
      const shareUrl = window.location.href;
      await shareNews(news.id);
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareUrl);
      } else {
        copyTextWithFallback(shareUrl);
      }
      setIsShareCopied(true);
      window.setTimeout(() => setIsShareCopied(false), 1800);
      setActionFeedback('');
      showToast('分享链接已复制，可发送给好友打开');
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '分享操作失败'), 'error');
    }
  };

  const handleSubmitComment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    const currentScrollY = window.scrollY;
    setIsSubmitting(true);
    try {
      await createNewsComment(news.id, commentText.trim());
      setCommentText('');
      setActionFeedback('');
      showToast('评论发布成功');
      const nextComments = await loadComments();
      syncCommentCount(nextComments);
      restoreScrollPosition(currentScrollY);
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '评论发布失败'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReply = async (commentId: number) => {
    if (!isAuthenticated) {
      requireLogin();
      return;
    }

    const content = replyDrafts[commentId]?.trim();
    if (!content) {
      showActionFeedback('回复内容不能为空', 'error');
      return;
    }

    setIsSubmitting(true);
    const currentScrollY = window.scrollY;
    try {
      await replyComment(commentId, content);
      setReplyDrafts((current) => ({ ...current, [commentId]: '' }));
      setReplyingCommentId(null);
      setActionFeedback('');
      showToast('回复成功');
      const nextComments = await loadComments();
      syncCommentCount(nextComments);
      restoreScrollPosition(currentScrollY);
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '回复失败'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!isAuthenticated) {
      requireLogin();
      return;
    }

    if (!window.confirm('确认删除这条评论吗？')) {
      return;
    }

    const currentScrollY = window.scrollY;
    try {
      await deleteComment(commentId);
      setActionFeedback('');
      showToast('评论已删除');
      const nextComments = await loadComments();
      syncCommentCount(nextComments);
      restoreScrollPosition(currentScrollY);
    } catch (error) {
      showActionFeedback(getErrorMessage(error, '删除评论失败'), 'error');
    }
  };

  if (isLoading) {
    return <div className="page-card news-state-card">正在加载新闻详情...</div>;
  }

  if (!news) {
    return (
      <div className="page-card news-state-card">
        <h1>新闻暂不可访问</h1>
        <p className="page-description">{errorMessage || '新闻不存在，或该新闻已下线。'}</p>
      </div>
    );
  }

  return (
    <section className="news-detail-page">
      {toastMessage ? <div className="news-detail-toast" role="status">{toastMessage}</div> : null}
      <article className="page-card news-detail-card news-detail-header-card">
        <div className="news-detail-meta">
          <span className="detail-meta-line" aria-hidden="true" />
          <span className="detail-meta-category">{news.categoryName}</span>
          <span>{formatHeaderDate(news.publishTime)}</span>
        </div>

        <h1>{news.title}</h1>
        {shouldShowSummary(news.summary, news.content) ? (
          <div className="article-summary">
            <p>
              {news.summary}
              {news.sourceUrl ? (
                <>
                  {' '}
                  <a href={news.sourceUrl} target="_blank" rel="noreferrer">查看全文</a>
                </>
              ) : null}
            </p>
          </div>
        ) : null}

        <div className="detail-header-divider" />

        <div className="detail-header-bottom">
          <div className="detail-source-tags">
            <span className="detail-source-name">{news.sourceName} RSS</span>
            <span className="detail-source-separator">·</span>
            <div className="news-tag-list">
              {news.tagNames.map((tagName) => (
                <span key={tagName} className="news-tag">
                  {tagName}
                </span>
              ))}
            </div>
          </div>

          <div className="news-detail-stats">
            <span className="stat-item heat"><span className="stat-symbol">热</span><strong>{news.heatScore}</strong></span>
            <span className="stat-item"><span className="stat-symbol">览</span><strong>{news.viewCount}</strong></span>
            <span className="stat-item"><span className="stat-symbol">赞</span><strong>{news.likeCount}</strong></span>
            <span className="stat-item"><span className="stat-symbol">藏</span><strong>{news.favoriteCount}</strong></span>
            <span className="stat-item"><span className="stat-symbol">评</span><strong>{displayCommentCount}</strong></span>
          </div>
        </div>
      </article>

      <section className="page-card news-cover-card">
        {news.coverImage ? (
          <NewsDetailCover imageUrl={news.coverImage} title={news.title} categoryName={news.categoryName} />
        ) : (
          <div className="news-detail-cover news-detail-cover-placeholder">
            <span>封面图</span>
          </div>
        )}
      </section>

      <article className="page-card news-detail-card news-article-card">
        <article className="news-detail-content article-content">
          {renderArticleContent(news.content)}
        </article>

        <div className="detail-actions-panel">
          <div className="detail-action-bar">
            <div className="detail-action-group">
              <button
                type="button"
                className={`detail-action-button detail-action-like ${news.liked ? 'is-active' : ''}`.trim()}
                onClick={() => void handleLike()}
              >
                <ThumbsUpIcon className="detail-action-icon" />
                <span>{news.liked ? '已点赞' : '点赞'} {news.likeCount}</span>
              </button>
              <button
                type="button"
                className={`detail-action-button detail-action-favorite ${news.favorited ? 'is-active' : ''}`.trim()}
                onClick={() => void handleFavorite()}
              >
                <StarIcon className="detail-action-icon" />
                <span>收藏 {news.favoriteCount}</span>
              </button>
              <button
                type="button"
                className={`detail-action-button detail-action-dislike ${news.disliked ? 'is-active' : ''}`.trim()}
                onClick={() => void handleDislike()}
              >
                <DislikeIcon className="detail-action-icon" />
                <span>{news.disliked ? '已减少推荐' : '不感兴趣'}</span>
              </button>
              <button
                type="button"
                className="detail-action-button detail-action-share"
                onClick={() => void handleShare()}
              >
                <ShareOneIcon className="detail-action-icon" />
                <span>{isShareCopied ? '已复制' : '分享'}</span>
              </button>
            </div>

            {news.sourceUrl ? (
              <a href={news.sourceUrl} target="_blank" rel="noreferrer" className="article-source-link">
                <span className="external-link-mark" aria-hidden="true">↗</span>
                打开原文 · {getSourceDisplayName(news.sourceUrl, news.sourceName)}
              </a>
            ) : null}
          </div>

          {actionFeedback ? <p className={`auth-feedback ${actionFeedbackType}`}>{actionFeedback}</p> : null}
        </div>
      </article>

      <section className="page-card comment-section">
        <div className="section-heading compact">
          <div className="comment-title-row">
            <h2>评论与回复</h2>
            <span className="comment-count-badge">
              {displayCommentCount > 0 ? `${displayCommentCount} 条评论` : '还没有人评论'}
            </span>
          </div>
          <label className="comment-sort-control">
            <span className="sr-only">评论排序</span>
            <select value={commentSortMode} onChange={(event) => setCommentSortMode(event.target.value as CommentSortMode)}>
              <option value="time">按时间</option>
              <option value="hot">按热度</option>
            </select>
          </label>
        </div>

        <form className="comment-editor" onSubmit={handleSubmitComment}>
          <div className="comment-editor-row">
            <div className="comment-avatar" aria-hidden="true">
              {(currentUser?.nickname || currentUser?.username || '访').slice(0, 1)}
            </div>
            <div className="comment-input-shell">
              <textarea
                value={commentText}
                onChange={(event) => setCommentText(event.target.value)}
                placeholder={isAuthenticated ? '说点什么……第一个评论是你的了' : '登录后可以发表评论'}
                rows={4}
              />
              <div className="comment-input-footer">
                <span className="section-meta">
                  {isAuthenticated ? `当前用户：${currentUser?.nickname || currentUser?.username}` : '未登录用户仅可查看评论'}
                </span>
                <span className="comment-length">{commentText.length} / 500</span>
              </div>
            </div>
          </div>
          <div className="comment-editor-actions">
            <button
              type="submit"
              className="primary-button comment-submit-button"
              disabled={isSubmitting || !commentText.trim()}
            >
              {isSubmitting ? '发布中...' : '发布评论'}
            </button>
          </div>
        </form>

        {isCommentsLoading ? (
          <div className="news-state-card">正在加载评论...</div>
        ) : comments.length === 0 ? (
          <div className="news-state-card comment-empty-state">
            <p>暂无评论，欢迎留下第一条看法。</p>
            <span>你的评论会用于个性化推荐</span>
          </div>
        ) : (
          <div className="comment-list">
            {sortedComments.map((comment) => (
              <article key={comment.id} className="comment-card">
                <div className="comment-header">
                  <div>
                    <strong>{comment.nickname}</strong>
                    <span>{formatDisplayDate(comment.createdAt)}</span>
                  </div>
                  <div className="comment-actions">
                    <button
                      type="button"
                      className="ghost-button"
                      onClick={() => {
                        if (!isAuthenticated) {
                          requireLogin();
                          return;
                        }
                        setReplyingCommentId((current) => (current === comment.id ? null : comment.id));
                      }}
                    >
                      回复
                    </button>
                    {comment.canDelete ? (
                      <button
                        type="button"
                        className="ghost-button danger"
                        onClick={() => void handleDeleteComment(comment.id)}
                      >
                        删除
                      </button>
                    ) : null}
                  </div>
                </div>
                <p>{comment.content}</p>

                {replyingCommentId === comment.id ? (
                  <div className="reply-editor">
                    <textarea
                      value={replyDrafts[comment.id] ?? ''}
                      onChange={(event) =>
                        setReplyDrafts((current) => ({ ...current, [comment.id]: event.target.value }))
                      }
                      rows={3}
                      placeholder="回复这条评论"
                    />
                    <div className="comment-actions">
                      <button type="button" className="ghost-button" onClick={() => setReplyingCommentId(null)}>
                        取消
                      </button>
                      <button
                        type="button"
                        className="primary-button"
                        disabled={isSubmitting}
                        onClick={() => void handleReply(comment.id)}
                      >
                        发送回复
                      </button>
                    </div>
                  </div>
                ) : null}

                {comment.replies.length > 0 ? (
                  <div className="reply-list">
                    {comment.replies.map((reply) => (
                      <div key={reply.id} className="reply-card">
                        <div className="comment-header">
                          <div>
                            <strong>{reply.nickname}</strong>
                            <span>{formatDisplayDate(reply.createdAt)}</span>
                          </div>
                          {reply.canDelete ? (
                            <button
                              type="button"
                              className="ghost-button danger"
                              onClick={() => void handleDeleteComment(reply.id)}
                            >
                              删除
                            </button>
                          ) : null}
                        </div>
                        <p>{reply.content}</p>
                      </div>
                    ))}
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="page-card related-news-section">
        <div className="section-heading compact">
          <div>
            <h2>相关推荐</h2>
          </div>
        </div>

        {isRelatedLoading ? (
          <div className="news-state-card compact-empty-state">正在加载相关推荐...</div>
        ) : relatedNews.length === 0 ? (
          <div className="news-state-card compact-empty-state">暂时没有可展示的相关推荐。</div>
        ) : (
          <div className="related-news-grid">
            {relatedNews.map((item) => (
              <Link key={item.id} to={`/news/${item.id}`} className="related-news-card">
                <NewsDetailCover imageUrl={item.coverImage} title={item.title} categoryName={item.categoryName} compact />
                <div className="related-news-body">
                  <div className="news-card-topline">
                    <span className="news-category-chip">{item.categoryName}</span>
                    <span className="news-time">{formatDisplayDate(item.publishTime)}</span>
                  </div>
                  <h3>{item.title}</h3>
                  <p>{item.summary}</p>
                  <div className="news-metrics">
                    <span>热度 {item.heatScore}</span>
                    <span>浏览 {item.viewCount}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}

function NewsDetailCover({
  imageUrl,
  title,
  categoryName,
  compact = false,
}: {
  imageUrl?: string | null;
  title: string;
  categoryName?: string;
  compact?: boolean;
}) {
  const [hasError, setHasError] = useState(false);
  const className = compact ? 'news-detail-cover related-news-cover' : 'news-detail-cover';
  const coverUrl = getDisplayNewsCover(imageUrl, categoryName, hasError);
  const fallbackUrl = getNewsCoverFallback(categoryName);
  const isFallbackCover = coverUrl === fallbackUrl;

  useEffect(() => {
    setHasError(false);
  }, [imageUrl, categoryName]);

  return (
    <div className={className}>
      <img
        key={coverUrl}
        src={coverUrl}
        alt={title}
        loading={compact ? 'lazy' : 'eager'}
        referrerPolicy="no-referrer"
        decoding={compact ? 'async' : 'auto'}
        onError={() => {
          if (!isFallbackCover) {
            setHasError(true);
          }
        }}
      />
    </div>
  );
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

function formatHeaderDate(value: string) {
  return formatDisplayDate(value).replace(' ', ' · ');
}

function renderArticleContent(content?: string) {
  const paragraphs = formatArticleContent(content);
  if (paragraphs.length === 0) {
    return <p className="empty-content">暂无正文内容</p>;
  }

  return paragraphs.map((paragraph, index) => {
    const isEndMarker = /^[-—\s]*END[-—\s]*$/i.test(paragraph);
    const isListLine = /^([-•]\s|\d+[.、]\s)/.test(paragraph);
    const classNames = [
      isEndMarker ? 'article-end-marker' : '',
      isListLine ? 'article-list-line' : '',
    ].filter(Boolean).join(' ') || undefined;
    return (
      <p key={`${index}-${paragraph.slice(0, 12)}`} className={classNames}>
        {paragraph}
      </p>
    );
  });
}

function formatArticleContent(content?: string) {
  if (!content || !content.trim()) {
    return [];
  }

  const normalized = stripSimpleHtml(content)
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n[ \t]+/g, '\n')
    .trim();

  let paragraphs = normalized
    .split(/\n{2,}/)
    .flatMap((block) => block.split(/\n(?=(?:[-•]\s|\d+[.、]\s))/))
    .map((item) => item.replace(/[ \t]{2,}/g, ' ').trim())
    .filter(Boolean);

  if (paragraphs.length <= 1 && normalized.length > 300) {
    paragraphs = splitLongPlainText(normalized);
  }

  return paragraphs;
}

function stripSimpleHtml(value: string) {
  return value
    .replace(/<\s*br\s*\/?>/gi, '\n')
    .replace(/<\/\s*(p|div|section|article|li|h[1-6]|blockquote)\s*>/gi, '\n\n')
    .replace(/<[^>]+>/g, '');
}

function splitLongPlainText(value: string) {
  const sentences = value
    .replace(/([。！？!?])\s*/g, '$1\n')
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean);

  const paragraphs: string[] = [];
  let buffer = '';
  for (const sentence of sentences) {
    if (!buffer) {
      buffer = sentence;
      continue;
    }
    if (buffer.length + sentence.length > 180) {
      paragraphs.push(buffer);
      buffer = sentence;
    } else {
      buffer += sentence;
    }
  }
  if (buffer) {
    paragraphs.push(buffer);
  }

  return paragraphs.length > 0 ? paragraphs : [value];
}

function shouldShowSummary(summary?: string, content?: string) {
  if (!summary || !summary.trim()) {
    return false;
  }
  const summaryText = normalizeComparableText(summary);
  const contentText = normalizeComparableText(content);
  return Boolean(summaryText) && (!contentText || !contentText.startsWith(summaryText));
}

function normalizeComparableText(value?: string) {
  return stripSimpleHtml(value ?? '').replace(/\s+/g, '').slice(0, 220);
}

function getSourceDisplayName(sourceUrl: string, sourceName: string) {
  try {
    return new URL(sourceUrl).hostname.replace(/^www\./, '') || sourceName;
  } catch {
    return sourceName;
  }
}

function countComments(comments: CommentItem[]) {
  return comments.reduce((total, comment) => total + 1 + comment.replies.length, 0);
}

function sortComments(comments: CommentItem[], mode: CommentSortMode) {
  return [...comments].sort((left, right) => {
    if (mode === 'hot') {
      const heatDiff = right.replies.length - left.replies.length;
      if (heatDiff !== 0) {
        return heatDiff;
      }
    }
    return Date.parse(right.createdAt) - Date.parse(left.createdAt);
  });
}

function restoreScrollPosition(scrollY: number) {
  requestAnimationFrame(() => {
    window.scrollTo({ top: scrollY, behavior: 'auto' });
  });
}

function copyTextWithFallback(value: string) {
  const textarea = document.createElement('textarea');
  textarea.value = value;
  textarea.setAttribute('readonly', 'readonly');
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

export default NewsDetailPage;
