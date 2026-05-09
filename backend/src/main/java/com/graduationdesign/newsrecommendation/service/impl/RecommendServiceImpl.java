package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.BehaviorActionType;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.Comment;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.entity.UserBehavior;
import com.graduationdesign.newsrecommendation.entity.UserInterest;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.CommentMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsTagMapper;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.mapper.UserBehaviorMapper;
import com.graduationdesign.newsrecommendation.mapper.UserInterestMapper;
import com.graduationdesign.newsrecommendation.service.RecommendService;
import com.graduationdesign.newsrecommendation.vo.RecommendNewsVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final double INTEREST_MATCH_SCORE = 50.0;

    private final NewsMapper newsMapper;
    private final NewsTagMapper newsTagMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final UserInterestMapper userInterestMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final CommentMapper commentMapper;

    public RecommendServiceImpl(
        NewsMapper newsMapper,
        NewsTagMapper newsTagMapper,
        TagMapper tagMapper,
        CategoryMapper categoryMapper,
        UserInterestMapper userInterestMapper,
        UserBehaviorMapper userBehaviorMapper,
        CommentMapper commentMapper
    ) {
        this.newsMapper = newsMapper;
        this.newsTagMapper = newsTagMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
        this.userInterestMapper = userInterestMapper;
        this.userBehaviorMapper = userBehaviorMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public PageResult<RecommendNewsVO> pageRecommendNews(User currentUser, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);

        List<News> activeNews = newsMapper.selectList(new LambdaQueryWrapper<News>()
            .eq(News::getStatus, 1)
            .orderByDesc(News::getPublishTime)
            .orderByDesc(News::getId));

        if (activeNews.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, safePage, safeSize);
        }

        RelatedNewsData relatedData = loadRelatedNewsData(activeNews);
        List<RecommendNewsVO> recommended = new ArrayList<>(currentUser == null
            ? buildAnonymousRecommendations(activeNews, relatedData)
            : buildPersonalizedRecommendations(activeNews, relatedData, currentUser.getId()));

        recommended.sort(Comparator
            .comparing(RecommendNewsVO::getRecommendScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(RecommendNewsVO::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(RecommendNewsVO::getId, Comparator.reverseOrder()));

        int fromIndex = (int) Math.min(recommended.size(), (safePage - 1) * safeSize);
        int toIndex = (int) Math.min(recommended.size(), fromIndex + safeSize);
        List<RecommendNewsVO> records = fromIndex >= recommended.size()
            ? Collections.emptyList()
            : recommended.subList(fromIndex, toIndex);

        return new PageResult<>(records, recommended.size(), safePage, safeSize);
    }

    private List<RecommendNewsVO> buildAnonymousRecommendations(List<News> activeNews, RelatedNewsData relatedData) {
        return activeNews.stream()
            .map(news -> {
                double heatScorePart = calculateHeatScore(news.getHeatScore());
                double freshnessScorePart = calculateFreshnessScore(news.getPublishTime());
                RecommendNewsVO vo = toRecommendNewsVO(news, relatedData);
                vo.setRecommendScore(roundScore(heatScorePart + freshnessScorePart));
                vo.setRecommendReason(heatScorePart >= freshnessScorePart ? "热门趋势内容" : "最新发布内容");
                return vo;
            })
            .sorted(Comparator
                .comparing(RecommendNewsVO::getRecommendScore, Comparator.reverseOrder())
                .thenComparing(RecommendNewsVO::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    private List<RecommendNewsVO> buildPersonalizedRecommendations(
        List<News> activeNews,
        RelatedNewsData relatedData,
        Long userId
    ) {
        Set<Long> interestTagIds = userInterestMapper.selectList(new LambdaQueryWrapper<UserInterest>()
                .eq(UserInterest::getUserId, userId))
            .stream()
            .map(UserInterest::getTagId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UserBehavior> behaviors = userBehaviorMapper.selectList(new LambdaQueryWrapper<UserBehavior>()
            .eq(UserBehavior::getUserId, userId)
            .orderByDesc(UserBehavior::getCreatedAt)
            .orderByDesc(UserBehavior::getId));

        Set<Long> dislikedNewsIds = behaviors.stream()
            .filter(behavior -> BehaviorActionType.DISLIKE.equals(behavior.getActionType()))
            .map(UserBehavior::getNewsId)
            .collect(Collectors.toSet());

        Map<Long, Double> behaviorTagWeights = buildBehaviorTagWeights(userId, behaviors, relatedData.newsTagMap());

        return activeNews.stream()
            .filter(news -> !dislikedNewsIds.contains(news.getId()))
            .map(news -> buildPersonalizedVO(news, relatedData, interestTagIds, behaviorTagWeights))
            .toList();
    }

    private RecommendNewsVO buildPersonalizedVO(
        News news,
        RelatedNewsData relatedData,
        Set<Long> interestTagIds,
        Map<Long, Double> behaviorTagWeights
    ) {
        List<Long> newsTagIds = relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList()).stream()
            .map(NewsTag::getTagId)
            .toList();

        boolean matchedInterest = newsTagIds.stream().anyMatch(interestTagIds::contains);
        double interestScorePart = calculateInterestScore(matchedInterest);
        double behaviorScorePart = calculateBehaviorScore(newsTagIds, behaviorTagWeights);
        double heatScorePart = calculateHeatScore(news.getHeatScore());
        double freshnessScorePart = calculateFreshnessScore(news.getPublishTime());

        RecommendNewsVO vo = toRecommendNewsVO(news, relatedData);
        vo.setRecommendScore(roundScore(interestScorePart + behaviorScorePart + heatScorePart + freshnessScorePart));
        vo.setRecommendReason(buildRecommendReason(matchedInterest, behaviorScorePart, heatScorePart, freshnessScorePart));
        return vo;
    }

    private Map<Long, Double> buildBehaviorTagWeights(Long userId, List<UserBehavior> behaviors, Map<Long, List<NewsTag>> newsTagMap) {
        Map<Long, Double> weights = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            if (BehaviorActionType.DISLIKE.equals(behavior.getActionType())) {
                continue;
            }
            double behaviorWeight = mapBehaviorWeight(behavior.getActionType());
            if (behaviorWeight <= 0) {
                continue;
            }
            List<NewsTag> newsTags = newsTagMap.getOrDefault(behavior.getNewsId(), Collections.emptyList());
            for (NewsTag newsTag : newsTags) {
                weights.merge(newsTag.getTagId(), behaviorWeight, Double::sum);
            }
        }

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getUserId, userId)
            .eq(Comment::getStatus, 1));
        for (Comment comment : comments) {
            List<NewsTag> newsTags = newsTagMap.getOrDefault(comment.getNewsId(), Collections.emptyList());
            for (NewsTag newsTag : newsTags) {
                weights.merge(newsTag.getTagId(), 20.0, Double::sum);
            }
        }

        return weights;
    }

    private double calculateInterestScore(boolean matchedInterest) {
        return matchedInterest ? INTEREST_MATCH_SCORE : 0.0;
    }

    private double calculateBehaviorScore(List<Long> newsTagIds, Map<Long, Double> behaviorTagWeights) {
        return newsTagIds.stream()
            .mapToDouble(tagId -> behaviorTagWeights.getOrDefault(tagId, 0.0))
            .sum();
    }

    private double calculateHeatScore(Double heatScore) {
        return safeDouble(heatScore) * 0.3;
    }

    private double mapBehaviorWeight(String actionType) {
        return switch (actionType) {
            case BehaviorActionType.LIKE -> 30.0;
            case BehaviorActionType.FAVORITE -> 40.0;
            case BehaviorActionType.VIEW -> 10.0;
            case BehaviorActionType.SHARE -> 15.0;
            default -> 0.0;
        };
    }

    private String buildRecommendReason(
        boolean matchedInterest,
        double behaviorScorePart,
        double heatScorePart,
        double freshnessScorePart
    ) {
        if (matchedInterest) {
            return "命中你的兴趣标签";
        }
        if (behaviorScorePart > 0) {
            return "根据你的浏览和点赞行为推荐";
        }
        if (heatScorePart >= freshnessScorePart) {
            return "热门趋势内容";
        }
        return "最新发布内容";
    }

    private double calculateFreshnessScore(LocalDateTime publishTime) {
        if (publishTime == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(publishTime, LocalDateTime.now()).toDays());
        if (days <= 3) {
            return 20.0;
        }
        if (days <= 7) {
            return 10.0;
        }
        if (days <= 30) {
            return 5.0;
        }
        return 0.0;
    }

    private RecommendNewsVO toRecommendNewsVO(News news, RelatedNewsData relatedData) {
        RecommendNewsVO vo = new RecommendNewsVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setCoverImage(news.getCoverImage());
        vo.setSourceName(news.getSourceName());
        vo.setCategoryId(news.getCategoryId());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setTagNames(relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList()).stream()
            .map(newsTag -> relatedData.tagMap().get(newsTag.getTagId()))
            .filter(Objects::nonNull)
            .map(Tag::getName)
            .toList());
        vo.setPublishTime(news.getPublishTime());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private RelatedNewsData loadRelatedNewsData(List<News> newsList) {
        List<Long> newsIds = newsList.stream().map(News::getId).toList();
        List<Long> categoryIds = newsList.stream().map(News::getCategoryId).filter(Objects::nonNull).distinct().toList();
        List<NewsTag> newsTags = newsIds.isEmpty()
            ? Collections.emptyList()
            : newsTagMapper.selectList(new LambdaQueryWrapper<NewsTag>().in(NewsTag::getNewsId, newsIds));

        Set<Long> tagIds = newsTags.stream().map(NewsTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = tagIds.isEmpty()
            ? Collections.emptyMap()
            : tagMapper.selectBatchIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, Function.identity()));
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
            ? Collections.emptyMap()
            : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, List<NewsTag>> newsTagMap = newsTags.stream().collect(Collectors.groupingBy(NewsTag::getNewsId));

        return new RelatedNewsData(categoryMap, tagMap, newsTagMap);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundScore(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record RelatedNewsData(
        Map<Long, Category> categoryMap,
        Map<Long, Tag> tagMap,
        Map<Long, List<NewsTag>> newsTagMap
    ) {
    }
}
