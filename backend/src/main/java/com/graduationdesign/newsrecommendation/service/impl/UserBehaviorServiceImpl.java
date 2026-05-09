package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.common.BehaviorActionType;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.UserBehavior;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.UserBehaviorMapper;
import com.graduationdesign.newsrecommendation.service.UserBehaviorService;
import com.graduationdesign.newsrecommendation.vo.NewsActionStatusVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper, UserBehavior> implements UserBehaviorService {

    private final NewsMapper newsMapper;

    public UserBehaviorServiceImpl(NewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    @Override
    @Transactional
    public void recordView(Long userId, Long newsId) {
        News news = getActiveNews(newsId);
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setNewsId(newsId);
        behavior.setActionType(BehaviorActionType.VIEW);
        behavior.setActionWeight(1.0);
        behavior.setDuration(0);
        save(behavior);
    }

    @Override
    @Transactional
    public NewsActionStatusVO likeNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        if (countBehavior(userId, newsId, BehaviorActionType.LIKE) == 0) {
            saveBehavior(userId, newsId, BehaviorActionType.LIKE);
            newsMapper.update(
                null,
                new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("like_count = like_count + 1")
            );
        }
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    @Transactional
    public NewsActionStatusVO unlikeNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        long removedCount = removeBehavior(userId, newsId, BehaviorActionType.LIKE);
        if (removedCount > 0) {
            newsMapper.update(
                null,
                new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("like_count = GREATEST(like_count - " + removedCount + ", 0)")
            );
        }
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    @Transactional
    public NewsActionStatusVO favoriteNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        if (countBehavior(userId, newsId, BehaviorActionType.FAVORITE) == 0) {
            saveBehavior(userId, newsId, BehaviorActionType.FAVORITE);
            newsMapper.update(
                null,
                new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("favorite_count = favorite_count + 1")
            );
        }
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    @Transactional
    public NewsActionStatusVO unfavoriteNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        long removedCount = removeBehavior(userId, newsId, BehaviorActionType.FAVORITE);
        if (removedCount > 0) {
            newsMapper.update(
                null,
                new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("favorite_count = GREATEST(favorite_count - " + removedCount + ", 0)")
            );
        }
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    @Transactional
    public NewsActionStatusVO dislikeNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        if (countBehavior(userId, newsId, BehaviorActionType.DISLIKE) == 0) {
            saveBehavior(userId, newsId, BehaviorActionType.DISLIKE);
        }
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    @Transactional
    public NewsActionStatusVO shareNews(Long userId, Long newsId) {
        getActiveNews(newsId);
        saveBehavior(userId, newsId, BehaviorActionType.SHARE);
        return buildStatus(userId, getActiveNews(newsId));
    }

    @Override
    public NewsActionStatusVO getActionStatus(Long userId, Long newsId) {
        News news = getActiveNews(newsId);
        return buildStatus(userId, news);
    }

    private NewsActionStatusVO buildStatus(Long userId, News news) {
        NewsActionStatusVO vo = new NewsActionStatusVO();
        vo.setLiked(countBehavior(userId, news.getId(), BehaviorActionType.LIKE) > 0);
        vo.setFavorited(countBehavior(userId, news.getId(), BehaviorActionType.FAVORITE) > 0);
        vo.setDisliked(countBehavior(userId, news.getId(), BehaviorActionType.DISLIKE) > 0);
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        return vo;
    }

    private News getActiveNews(Long newsId) {
        News news = newsMapper.selectById(newsId);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new NotFoundException("News does not exist or has been taken offline");
        }
        return news;
    }

    private void saveBehavior(Long userId, Long newsId, String actionType) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setNewsId(newsId);
        behavior.setActionType(actionType);
        behavior.setActionWeight(1.0);
        behavior.setDuration(0);
        save(behavior);
    }

    private long removeBehavior(Long userId, Long newsId, String actionType) {
        long count = countBehavior(userId, newsId, actionType);
        if (count > 0) {
            remove(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getNewsId, newsId)
                .eq(UserBehavior::getActionType, actionType));
        }
        return count;
    }

    private long countBehavior(Long userId, Long newsId, String actionType) {
        return count(new LambdaQueryWrapper<UserBehavior>()
            .eq(UserBehavior::getUserId, userId)
            .eq(UserBehavior::getNewsId, newsId)
            .eq(UserBehavior::getActionType, actionType));
    }
}
