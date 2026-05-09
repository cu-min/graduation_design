package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.BehaviorActionType;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.Comment;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.entity.UserBehavior;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.CommentMapper;
import com.graduationdesign.newsrecommendation.mapper.CrawlConfigMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.UserBehaviorMapper;
import com.graduationdesign.newsrecommendation.mapper.UserMapper;
import com.graduationdesign.newsrecommendation.service.DashboardService;
import com.graduationdesign.newsrecommendation.vo.AdminCategoryStatVO;
import com.graduationdesign.newsrecommendation.vo.AdminDashboardSummaryVO;
import com.graduationdesign.newsrecommendation.vo.AdminHotNewsVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final NewsMapper newsMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final CrawlConfigMapper crawlConfigMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final CategoryMapper categoryMapper;

    public DashboardServiceImpl(
        NewsMapper newsMapper,
        UserMapper userMapper,
        CommentMapper commentMapper,
        CrawlConfigMapper crawlConfigMapper,
        UserBehaviorMapper userBehaviorMapper,
        CategoryMapper categoryMapper
    ) {
        this.newsMapper = newsMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.crawlConfigMapper = crawlConfigMapper;
        this.userBehaviorMapper = userBehaviorMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public AdminDashboardSummaryVO getAdminDashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        AdminDashboardSummaryVO vo = new AdminDashboardSummaryVO();
        vo.setNewsTotal(newsMapper.selectCount(null));
        vo.setOnlineNewsTotal(newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getStatus, 1)));
        vo.setOfflineNewsTotal(newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getStatus, 0)));
        vo.setUserTotal(userMapper.selectCount(null));
        vo.setCommentTotal(commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1)));
        vo.setCrawlConfigTotal(crawlConfigMapper.selectCount(null));
        vo.setEnabledCrawlConfigTotal(crawlConfigMapper.selectCount(new LambdaQueryWrapper<CrawlConfig>().eq(CrawlConfig::getEnabled, 1)));
        vo.setViewBehaviorTotal(countBehaviorByType(BehaviorActionType.VIEW));
        vo.setLikeBehaviorTotal(countBehaviorByType(BehaviorActionType.LIKE));
        vo.setFavoriteBehaviorTotal(countBehaviorByType(BehaviorActionType.FAVORITE));
        vo.setDislikeBehaviorTotal(countBehaviorByType(BehaviorActionType.DISLIKE));
        vo.setShareBehaviorTotal(countBehaviorByType(BehaviorActionType.SHARE));
        vo.setTodayCrawledNewsTotal(newsMapper.selectCount(
            new LambdaQueryWrapper<News>()
                .ge(News::getCrawlTime, todayStart)
                .lt(News::getCrawlTime, tomorrowStart)
        ));
        vo.setTodayCommentTotal(commentMapper.selectCount(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1)
                .ge(Comment::getCreatedAt, todayStart)
                .lt(Comment::getCreatedAt, tomorrowStart)
        ));

        CrawlConfig latestCrawlConfig = crawlConfigMapper.selectOne(
            new LambdaQueryWrapper<CrawlConfig>()
                .isNotNull(CrawlConfig::getLastCrawlTime)
                .orderByDesc(CrawlConfig::getLastCrawlTime)
                .last("LIMIT 1")
        );
        if (latestCrawlConfig != null) {
            vo.setLatestCrawlTime(latestCrawlConfig.getLastCrawlTime());
            vo.setLatestCrawlStatus(latestCrawlConfig.getLastStatus());
        }

        vo.setHotNews(buildHotNews());
        vo.setCategoryStats(buildCategoryStats());

        return vo;
    }

    private long countBehaviorByType(String actionType) {
        return userBehaviorMapper.selectCount(new LambdaQueryWrapper<UserBehavior>().eq(UserBehavior::getActionType, actionType));
    }

    private List<AdminHotNewsVO> buildHotNews() {
        List<News> hotNews = newsMapper.selectList(
            new LambdaQueryWrapper<News>()
                .eq(News::getStatus, 1)
                .orderByDesc(News::getHeatScore)
                .orderByDesc(News::getLikeCount)
                .orderByDesc(News::getFavoriteCount)
                .last("LIMIT 5")
        );

        if (hotNews.isEmpty()) {
            return Collections.emptyList();
        }

        return hotNews.stream().map(news -> {
            AdminHotNewsVO vo = new AdminHotNewsVO();
            vo.setId(news.getId());
            vo.setTitle(news.getTitle());
            vo.setHeatScore(news.getHeatScore());
            vo.setLikeCount(news.getLikeCount());
            vo.setFavoriteCount(news.getFavoriteCount());
            return vo;
        }).toList();
    }

    private List<AdminCategoryStatVO> buildCategoryStats() {
        List<News> allNews = newsMapper.selectList(new LambdaQueryWrapper<News>().select(News::getCategoryId));
        if (allNews.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> categoryCountMap = allNews.stream()
            .map(News::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (categoryCountMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryCountMap.keySet()).stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

        return categoryCountMap.entrySet().stream()
            .map(entry -> {
                AdminCategoryStatVO vo = new AdminCategoryStatVO();
                vo.setCategoryId(entry.getKey());
                vo.setCategoryName(categoryMap.get(entry.getKey()) != null ? categoryMap.get(entry.getKey()).getName() : "未知分类");
                vo.setNewsCount(entry.getValue());
                return vo;
            })
            .sorted((left, right) -> Long.compare(right.getNewsCount(), left.getNewsCount()))
            .toList();
    }
}
