package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.BehaviorActionType;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.Comment;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.entity.UserBehavior;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.CommentMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.UserBehaviorMapper;
import com.graduationdesign.newsrecommendation.service.ProfileService;
import com.graduationdesign.newsrecommendation.vo.ProfileCommentVO;
import com.graduationdesign.newsrecommendation.vo.ProfileNewsItemVO;
import com.graduationdesign.newsrecommendation.vo.ProfileSummaryVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final CommentMapper commentMapper;
    private final NewsMapper newsMapper;
    private final CategoryMapper categoryMapper;

    public ProfileServiceImpl(
        UserBehaviorMapper userBehaviorMapper,
        CommentMapper commentMapper,
        NewsMapper newsMapper,
        CategoryMapper categoryMapper
    ) {
        this.userBehaviorMapper = userBehaviorMapper;
        this.commentMapper = commentMapper;
        this.newsMapper = newsMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ProfileSummaryVO getProfileSummary(User currentUser) {
        ProfileSummaryVO vo = new ProfileSummaryVO();
        vo.setId(currentUser.getId());
        vo.setUsername(currentUser.getUsername());
        vo.setNickname(currentUser.getNickname());
        vo.setEmail(currentUser.getEmail());
        vo.setPhone(currentUser.getPhone());
        vo.setAvatar(currentUser.getAvatar());
        vo.setRole(currentUser.getRole());
        vo.setFavoriteCount(userBehaviorMapper.selectCount(behaviorWrapper(currentUser.getId(), BehaviorActionType.FAVORITE)).intValue());
        vo.setLikeCount(userBehaviorMapper.selectCount(behaviorWrapper(currentUser.getId(), BehaviorActionType.LIKE)).intValue());
        vo.setCommentCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getUserId, currentUser.getId())
            .eq(Comment::getStatus, 1)).intValue());
        vo.setHistoryCount(countDistinctHistory(currentUser.getId()));
        return vo;
    }

    @Override
    public PageResult<ProfileNewsItemVO> pageFavorites(Long userId, long page, long size) {
        return pageBehaviorNews(userId, BehaviorActionType.FAVORITE, page, size, false);
    }

    @Override
    public PageResult<ProfileNewsItemVO> pageLikes(Long userId, long page, long size) {
        return pageBehaviorNews(userId, BehaviorActionType.LIKE, page, size, false);
    }

    @Override
    public PageResult<ProfileNewsItemVO> pageHistory(Long userId, long page, long size) {
        return pageBehaviorNews(userId, BehaviorActionType.VIEW, page, size, true);
    }

    @Override
    public PageResult<ProfileCommentVO> pageComments(Long userId, long page, long size) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getUserId, userId)
            .eq(Comment::getStatus, 1)
            .orderByDesc(Comment::getCreatedAt)
            .orderByDesc(Comment::getId));

        if (comments.isEmpty()) {
            return emptyPage(page, size);
        }

        Set<Long> newsIds = comments.stream().map(Comment::getNewsId).collect(Collectors.toSet());
        Map<Long, News> newsMap = loadActiveNewsMap(newsIds);

        List<ProfileCommentVO> items = comments.stream()
            .filter(comment -> newsMap.containsKey(comment.getNewsId()))
            .map(comment -> {
                News news = newsMap.get(comment.getNewsId());
                ProfileCommentVO vo = new ProfileCommentVO();
                vo.setId(comment.getId());
                vo.setNewsId(comment.getNewsId());
                vo.setNewsTitle(news.getTitle());
                vo.setParentId(comment.getParentId());
                vo.setContent(comment.getContent());
                vo.setCreatedAt(comment.getCreatedAt());
                return vo;
            })
            .toList();

        return paginate(items, page, size);
    }

    private PageResult<ProfileNewsItemVO> pageBehaviorNews(Long userId, String actionType, long page, long size, boolean deduplicateByNews) {
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(new LambdaQueryWrapper<UserBehavior>()
            .eq(UserBehavior::getUserId, userId)
            .eq(UserBehavior::getActionType, actionType)
            .orderByDesc(UserBehavior::getCreatedAt)
            .orderByDesc(UserBehavior::getId));

        if (behaviors.isEmpty()) {
            return emptyPage(page, size);
        }

        List<UserBehavior> filteredBehaviors = deduplicateByNews ? deduplicateByNews(behaviors) : behaviors;
        Set<Long> newsIds = filteredBehaviors.stream().map(UserBehavior::getNewsId).collect(Collectors.toSet());
        Map<Long, News> newsMap = loadActiveNewsMap(newsIds);
        Map<Long, Category> categoryMap = loadCategoryMap(newsMap.values().stream().map(News::getCategoryId).collect(Collectors.toSet()));

        List<ProfileNewsItemVO> items = filteredBehaviors.stream()
            .filter(behavior -> newsMap.containsKey(behavior.getNewsId()))
            .map(behavior -> toProfileNewsItemVO(behavior, newsMap.get(behavior.getNewsId()), categoryMap))
            .toList();

        return paginate(items, page, size);
    }

    private List<UserBehavior> deduplicateByNews(List<UserBehavior> behaviors) {
        Map<Long, UserBehavior> latestByNewsId = new LinkedHashMap<>();
        for (UserBehavior behavior : behaviors) {
            latestByNewsId.putIfAbsent(behavior.getNewsId(), behavior);
        }
        return new ArrayList<>(latestByNewsId.values());
    }

    private Map<Long, News> loadActiveNewsMap(Set<Long> newsIds) {
        if (newsIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return newsMapper.selectBatchIds(newsIds).stream()
            .filter(news -> news.getStatus() != null && news.getStatus() == 1)
            .collect(Collectors.toMap(News::getId, Function.identity()));
    }

    private Map<Long, Category> loadCategoryMap(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private ProfileNewsItemVO toProfileNewsItemVO(UserBehavior behavior, News news, Map<Long, Category> categoryMap) {
        ProfileNewsItemVO vo = new ProfileNewsItemVO();
        vo.setNewsId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setCoverImage(news.getCoverImage());
        vo.setSourceName(news.getSourceName());
        vo.setCategoryName(categoryMap.get(news.getCategoryId()) != null ? categoryMap.get(news.getCategoryId()).getName() : null);
        vo.setPublishTime(news.getPublishTime());
        vo.setBehaviorTime(behavior.getCreatedAt());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private int countDistinctHistory(Long userId) {
        List<UserBehavior> views = userBehaviorMapper.selectList(behaviorWrapper(userId, BehaviorActionType.VIEW));
        return (int) views.stream().map(UserBehavior::getNewsId).filter(Objects::nonNull).distinct().count();
    }

    private LambdaQueryWrapper<UserBehavior> behaviorWrapper(Long userId, String actionType) {
        return new LambdaQueryWrapper<UserBehavior>()
            .eq(UserBehavior::getUserId, userId)
            .eq(UserBehavior::getActionType, actionType);
    }

    private <T> PageResult<T> emptyPage(long page, long size) {
        return new PageResult<>(Collections.emptyList(), 0, Math.max(page, 1), Math.max(size, 1));
    }

    private <T> PageResult<T> paginate(List<T> source, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        int fromIndex = (int) Math.min(source.size(), (safePage - 1) * safeSize);
        int toIndex = (int) Math.min(source.size(), fromIndex + safeSize);
        List<T> records = fromIndex >= source.size() ? Collections.emptyList() : source.subList(fromIndex, toIndex);
        return new PageResult<>(records, source.size(), safePage, safeSize);
    }
}
