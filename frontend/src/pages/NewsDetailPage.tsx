import { useEffect, useState } from 'react';
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
import { fetchNewsDetail } from '../api/news';
import { useAuth } from '../store';
import type { CommentItem, NewsActionStatus, NewsDetail } from '../types';
import { openAuthDialog } from '../utils/authDialog';
import { getErrorMessage } from '../utils/request';

function NewsDetailPage() {
  const { id } = useParams();
  const { isAuthenticated, currentUser } = useAuth();
  const [news, setNews] = useState<NewsDetail | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCommentsLoading, setIsCommentsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [commentText, setCommentText] = useState('');
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [replyingCommentId, setReplyingCommentId] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionFeedback, setActionFeedback] = useState('');

  const newsId = id ? Number(id) : NaN;

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

  const loadComments = async () => {
    if (!newsId || Number.isNaN(newsId)) {
      setIsCommentsLoading(false);
      return;
    }

    setIsCommentsLoading(true);
    try {
      const result = await fetchNewsComments(newsId);
      setComments(result.data);
    } catch {
      setComments([]);
    } finally {
      setIsCommentsLoading(false);
    }
  };

  useEffect(() => {
    void Promise.all([loadNewsDetail(), loadComments()]);
  }, [id, isAuthenticated]);

  const requireLogin = () => {
    setActionFeedback('请先登录后再进行互动操作');
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
      setActionFeedback(news.liked ? '已取消点赞' : '点赞成功');
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '点赞操作失败'));
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
      setActionFeedback(news.favorited ? '已取消收藏' : '收藏成功');
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '收藏操作失败'));
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
      setActionFeedback('已记录为不感兴趣');
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '不感兴趣操作失败'));
    }
  };

  const handleShare = async () => {
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    try {
      await shareNews(news.id);
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(window.location.href);
      }
      setActionFeedback('已记录分享，并复制当前链接');
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '分享记录失败'));
    }
  };

  const refreshDetailAndComments = async () => {
    await Promise.all([loadNewsDetail(), loadComments()]);
  };

  const handleSubmitComment = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!isAuthenticated || !news) {
      requireLogin();
      return;
    }

    setIsSubmitting(true);
    try {
      await createNewsComment(news.id, commentText.trim());
      setCommentText('');
      setActionFeedback('评论发布成功');
      await refreshDetailAndComments();
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '评论发布失败'));
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
      setActionFeedback('回复内容不能为空');
      return;
    }

    setIsSubmitting(true);
    try {
      await replyComment(commentId, content);
      setReplyDrafts((current) => ({ ...current, [commentId]: '' }));
      setReplyingCommentId(null);
      setActionFeedback('回复成功');
      await refreshDetailAndComments();
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '回复失败'));
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

    try {
      await deleteComment(commentId);
      setActionFeedback('评论已删除');
      await refreshDetailAndComments();
    } catch (error) {
      setActionFeedback(getErrorMessage(error, '删除评论失败'));
    }
  };

  if (isLoading) {
    return <div className="page-card news-state-card">正在加载新闻详情...</div>;
  }

  if (!news) {
    return (
      <div className="page-card news-state-card">
        <p className="page-eyebrow">阶段 9</p>
        <h1>新闻暂不可访问</h1>
        <p className="page-description">{errorMessage || '新闻不存在，或该新闻已下架。'}</p>
        <Link to="/" className="ghost-button">
          返回首页
        </Link>
      </div>
    );
  }

  return (
    <section className="news-detail-page">
      <article className="page-card news-detail-card">
        <div className="news-detail-meta">
          <span className="news-category-chip">{news.categoryName}</span>
          <span>{formatDisplayDate(news.publishTime)}</span>
          <span>{news.sourceName}</span>
        </div>

        <h1>{news.title}</h1>
        <p className="news-detail-summary">{news.summary}</p>

        <div className="news-detail-stats">
          <span>热度 {news.heatScore}</span>
          <span>浏览 {news.viewCount}</span>
          <span>点赞 {news.likeCount}</span>
          <span>收藏 {news.favoriteCount}</span>
          <span>评论 {news.commentCount}</span>
        </div>

        <div className="news-tag-list">
          {news.tagNames.map((tagName) => (
            <span key={tagName} className="news-tag">
              {tagName}
            </span>
          ))}
        </div>

        <div className="detail-action-bar">
          <button type="button" className={news.liked ? 'primary-button' : 'ghost-button'} onClick={() => void handleLike()}>
            {news.liked ? '已点赞' : '点赞'}
          </button>
          <button
            type="button"
            className={news.favorited ? 'primary-button' : 'ghost-button'}
            onClick={() => void handleFavorite()}
          >
            {news.favorited ? '已收藏' : '收藏'}
          </button>
          <button
            type="button"
            className={news.disliked ? 'ghost-button active-soft' : 'ghost-button'}
            onClick={() => void handleDislike()}
          >
            {news.disliked ? '已标记不感兴趣' : '不感兴趣'}
          </button>
          <button type="button" className="ghost-button" onClick={() => void handleShare()}>
            分享
          </button>
        </div>

        {actionFeedback ? <p className="auth-feedback success">{actionFeedback}</p> : null}

        <NewsDetailCover imageUrl={news.coverImage} title={news.title} />

        <div className="news-detail-content">
          {news.content.split('\n').filter(Boolean).map((paragraph, index) => (
            <p key={`${news.id}-${index}`}>{paragraph}</p>
          ))}
        </div>

        <div className="news-detail-footer">
          <a href={news.sourceUrl} target="_blank" rel="noreferrer" className="primary-button">
            打开原文链接
          </a>
          <Link to="/" className="ghost-button">
            返回首页
          </Link>
        </div>
      </article>

      <section className="page-card comment-section">
        <div className="section-heading compact">
          <div>
            <p className="page-eyebrow">互动区</p>
            <h2>评论与回复</h2>
          </div>
          <span className="section-meta">共 {news.commentCount} 条评论</span>
        </div>

        <form className="comment-editor" onSubmit={handleSubmitComment}>
          <textarea
            value={commentText}
            onChange={(event) => setCommentText(event.target.value)}
            placeholder={isAuthenticated ? '写下你的评论（1-500字）' : '登录后可以发表评论'}
            rows={4}
          />
          <div className="comment-editor-actions">
            <span className="section-meta">
              {isAuthenticated ? `当前用户：${currentUser?.nickname || currentUser?.username}` : '未登录用户仅可查看评论'}
            </span>
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              发布评论
            </button>
          </div>
        </form>

        {isCommentsLoading ? (
          <div className="news-state-card">正在加载评论...</div>
        ) : comments.length === 0 ? (
          <div className="news-state-card">暂无评论，欢迎留下第一条看法。</div>
        ) : (
          <div className="comment-list">
            {comments.map((comment) => (
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
    </section>
  );
}

function NewsDetailCover({ imageUrl, title }: { imageUrl: string; title: string }) {
  const [hasError, setHasError] = useState(false);

  if (!imageUrl || hasError) {
    return (
      <div className="news-detail-cover news-cover-empty">
        <span>{title.slice(0, 24)}</span>
      </div>
    );
  }

  return (
    <div className="news-detail-cover">
      <img src={imageUrl} alt={title} onError={() => setHasError(true)} />
    </div>
  );
}

function formatDisplayDate(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

export default NewsDetailPage;
