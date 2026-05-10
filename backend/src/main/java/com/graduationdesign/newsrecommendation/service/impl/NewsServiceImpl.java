package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.dto.NewsCreateRequest;
import com.graduationdesign.newsrecommendation.dto.NewsStatusRequest;
import com.graduationdesign.newsrecommendation.dto.NewsUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsTagMapper;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.service.NewsService;
import com.graduationdesign.newsrecommendation.service.UserBehaviorService;
import com.graduationdesign.newsrecommendation.vo.AdminNewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminNewsListVO;
import com.graduationdesign.newsrecommendation.vo.HotNewsVO;
import com.graduationdesign.newsrecommendation.vo.NewsActionStatusVO;
import com.graduationdesign.newsrecommendation.vo.NewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.NewsListVO;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private final NewsTagMapper newsTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserBehaviorService userBehaviorService;

    public NewsServiceImpl(
        NewsTagMapper newsTagMapper,
        CategoryMapper categoryMapper,
        TagMapper tagMapper,
        UserBehaviorService userBehaviorService
    ) {
        this.newsTagMapper = newsTagMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.userBehaviorService = userBehaviorService;
    }

    @Override
    public PageResult<AdminNewsListVO> pageAdminNews(long page, long size, String keyword, Long categoryId, Integer status) {
        Page<News> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<News>()
            .eq(categoryId != null, News::getCategoryId, categoryId)
            .eq(status != null, News::getStatus, status)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(News::getTitle, keyword)
                .or()
                .like(News::getSummary, keyword)
            )
            .orderByDesc(News::getPublishTime)
            .orderByDesc(News::getId);

        Page<News> pageResult = page(mpPage, queryWrapper);
        List<AdminNewsListVO> records = buildAdminNewsList(pageResult.getRecords());
        return new PageResult<>(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public AdminNewsDetailVO getAdminNewsDetail(Long id) {
        News news = getByIdOrThrow(id);
        RelatedData relatedData = loadRelatedData(List.of(news.getId()), List.of(news.getCategoryId()));
        return toAdminNewsDetailVO(news, relatedData);
    }

    @Override
    @Transactional
    public void createAdminNews(NewsCreateRequest request) {
        validateCategoryAndTags(request.getCategoryId(), request.getTagIds());
        ensureSourceUrlUnique(request.getSourceUrl().trim(), null);
        validateStatus(request.getStatus());

        News news = new News();
        fillNews(news, request);
        news.setViewCount(0);
        news.setLikeCount(0);
        news.setFavoriteCount(0);
        news.setCommentCount(0);
        save(news);

        replaceNewsTags(news.getId(), request.getTagIds());
    }

    @Override
    @Transactional
    public void updateAdminNews(Long id, NewsUpdateRequest request) {
        News news = getByIdOrThrow(id);
        validateCategoryAndTags(request.getCategoryId(), request.getTagIds());
        ensureSourceUrlUnique(request.getSourceUrl().trim(), id);
        validateStatus(request.getStatus());

        fillNews(news, request);
        updateById(news);
        replaceNewsTags(id, request.getTagIds());
    }

    @Override
    @Transactional
    public void deleteAdminNews(Long id) {
        getByIdOrThrow(id);
        newsTagMapper.delete(new LambdaQueryWrapper<NewsTag>().eq(NewsTag::getNewsId, id));
        removeById(id);
    }

    @Override
    @Transactional
    public void updateAdminNewsStatus(Long id, NewsStatusRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new IllegalArgumentException("Status must be 0 or 1");
        }
        News news = getByIdOrThrow(id);
        news.setStatus(request.getStatus());
        updateById(news);
    }

    @Override
    public PageResult<NewsListVO> pagePublicNews(long page, long size, String keyword, Long categoryId) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);

        Page<News> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<News>()
            .eq(News::getStatus, 1)
            .eq(categoryId != null, News::getCategoryId, categoryId)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(News::getTitle, keyword)
                .or()
                .like(News::getSummary, keyword)
            )
            .orderByDesc(News::getHeatScore)
            .orderByDesc(News::getPublishTime)
            .orderByDesc(News::getId);

        Page<News> pageResult = page(mpPage, queryWrapper);
        List<NewsListVO> records = buildPublicNewsList(pageResult.getRecords());
        return new PageResult<>(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public NewsDetailVO getPublicNewsDetail(Long id, User currentUser) {
        News news = getOne(
            new LambdaQueryWrapper<News>()
                .eq(News::getId, id)
                .eq(News::getStatus, 1)
                .last("LIMIT 1")
        );
        if (news == null) {
            throw new NotFoundException("News does not exist or has been taken offline");
        }

        if (currentUser != null) {
            userBehaviorService.recordView(currentUser.getId(), id);
        }

        RelatedData relatedData = loadRelatedData(List.of(news.getId()), List.of(news.getCategoryId()));
        NewsActionStatusVO actionStatus = currentUser != null
            ? userBehaviorService.getActionStatus(currentUser.getId(), id)
            : createAnonymousActionStatus(news);
        return toNewsDetailVO(news, relatedData, actionStatus);
    }

    @Override
    public List<NewsListVO> listRelatedNews(Long id, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 8);
        News currentNews = getOne(
            new LambdaQueryWrapper<News>()
                .eq(News::getId, id)
                .eq(News::getStatus, 1)
                .last("LIMIT 1")
        );
        if (currentNews == null) {
            throw new NotFoundException("News does not exist or has been taken offline");
        }

        Set<Long> currentTagIds = newsTagMapper.selectList(
            new LambdaQueryWrapper<NewsTag>().eq(NewsTag::getNewsId, id)
        ).stream().map(NewsTag::getTagId).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Long> overlapCountMap = Collections.emptyMap();
        Set<Long> tagMatchedNewsIds = new LinkedHashSet<>();
        if (!currentTagIds.isEmpty()) {
            List<NewsTag> matchedNewsTags = newsTagMapper.selectList(
                new LambdaQueryWrapper<NewsTag>()
                    .in(NewsTag::getTagId, currentTagIds)
                    .ne(NewsTag::getNewsId, id)
            );
            tagMatchedNewsIds = matchedNewsTags.stream()
                .map(NewsTag::getNewsId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            overlapCountMap = matchedNewsTags.stream()
                .collect(Collectors.groupingBy(NewsTag::getNewsId, Collectors.counting()));
        }

        List<News> candidates = queryRelatedCandidates(currentNews, tagMatchedNewsIds, safeLimit);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> finalOverlapCountMap = overlapCountMap;
        return buildPublicNewsList(
            candidates.stream()
                .sorted(
                    Comparator
                        .comparingLong((News news) -> finalOverlapCountMap.getOrDefault(news.getId(), 0L))
                        .reversed()
                        .thenComparing(
                            (News news) -> Objects.equals(news.getCategoryId(), currentNews.getCategoryId()) ? 1 : 0,
                            Comparator.reverseOrder()
                        )
                        .thenComparing(News::getHeatScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(News::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(News::getId, Comparator.reverseOrder())
                )
                .limit(safeLimit)
                .toList()
        );
    }

    @Override
    public List<HotNewsVO> listHotNews(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        Page<News> pageResult = page(
            new Page<>(1, safeLimit),
            new LambdaQueryWrapper<News>()
                .eq(News::getStatus, 1)
                .orderByDesc(News::getHeatScore)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getId)
        );

        if (pageResult.getRecords().isEmpty()) {
            return Collections.emptyList();
        }

        RelatedData relatedData = loadRelatedData(
            pageResult.getRecords().stream().map(News::getId).toList(),
            pageResult.getRecords().stream().map(News::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );

        return pageResult.getRecords().stream()
            .map(news -> toHotNewsVO(news, relatedData))
            .toList();
    }

    private List<News> queryRelatedCandidates(News currentNews, Set<Long> tagMatchedNewsIds, int safeLimit) {
        Page<News> candidatePage = new Page<>(1, Math.max(safeLimit * 4L, 12L));
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<News>()
            .eq(News::getStatus, 1)
            .ne(News::getId, currentNews.getId());

        if (!tagMatchedNewsIds.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .eq(News::getCategoryId, currentNews.getCategoryId())
                .or()
                .in(News::getId, tagMatchedNewsIds)
            );
        } else {
            queryWrapper.eq(News::getCategoryId, currentNews.getCategoryId());
        }

        queryWrapper
            .orderByDesc(News::getHeatScore)
            .orderByDesc(News::getPublishTime)
            .orderByDesc(News::getId);

        List<News> candidates = page(candidatePage, queryWrapper).getRecords();
        if (!candidates.isEmpty()) {
            return candidates;
        }

        return page(
            new Page<>(1, safeLimit),
            new LambdaQueryWrapper<News>()
                .eq(News::getStatus, 1)
                .ne(News::getId, currentNews.getId())
                .orderByDesc(News::getHeatScore)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getId)
        ).getRecords();
    }

    private News getByIdOrThrow(Long id) {
        News news = getById(id);
        if (news == null) {
            throw new NotFoundException("News does not exist");
        }
        return news;
    }

    private void validateCategoryAndTags(Long categoryId, List<Long> tagIds) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category does not exist");
        }
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("Tags cannot be empty");
        }

        Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
        List<Tag> tags = tagMapper.selectBatchIds(distinctTagIds);
        if (tags.size() != distinctTagIds.size()) {
            throw new IllegalArgumentException("Some tags do not exist");
        }
    }

    private void ensureSourceUrlUnique(String sourceUrl, Long excludeId) {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<News>().eq(News::getSourceUrl, sourceUrl);
        if (excludeId != null) {
            queryWrapper.ne(News::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new IllegalArgumentException("Source URL already exists");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("Status must be 0 or 1");
        }
    }

    private void fillNews(News news, NewsCreateRequest request) {
        news.setTitle(request.getTitle().trim());
        news.setSummary(request.getSummary().trim());
        news.setContent(request.getContent().trim());
        news.setSourceName(request.getSourceName().trim());
        news.setSourceUrl(request.getSourceUrl().trim());
        news.setCoverImage(request.getCoverImage().trim());
        news.setCategoryId(request.getCategoryId());
        news.setPublishTime(request.getPublishTime());
        news.setCrawlTime(news.getCrawlTime() == null ? LocalDateTime.now() : news.getCrawlTime());
        news.setStatus(request.getStatus());
        news.setHeatScore(request.getHeatScore());
    }

    private void replaceNewsTags(Long newsId, List<Long> tagIds) {
        newsTagMapper.delete(new LambdaQueryWrapper<NewsTag>().eq(NewsTag::getNewsId, newsId));
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            NewsTag newsTag = new NewsTag();
            newsTag.setNewsId(newsId);
            newsTag.setTagId(tagId);
            newsTagMapper.insert(newsTag);
        }
    }

    private List<AdminNewsListVO> buildAdminNewsList(List<News> newsList) {
        if (newsList.isEmpty()) {
            return Collections.emptyList();
        }

        RelatedData relatedData = loadRelatedData(
            newsList.stream().map(News::getId).toList(),
            newsList.stream().map(News::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );

        return newsList.stream()
            .map(news -> toAdminNewsListVO(news, relatedData))
            .toList();
    }

    private List<NewsListVO> buildPublicNewsList(List<News> newsList) {
        if (newsList.isEmpty()) {
            return Collections.emptyList();
        }

        RelatedData relatedData = loadRelatedData(
            newsList.stream().map(News::getId).toList(),
            newsList.stream().map(News::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );

        return newsList.stream()
            .map(news -> toNewsListVO(news, relatedData))
            .toList();
    }

    private RelatedData loadRelatedData(List<Long> newsIds, List<Long> categoryIds) {
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
        return new RelatedData(categoryMap, tagMap, newsTagMap);
    }

    private AdminNewsListVO toAdminNewsListVO(News news, RelatedData relatedData) {
        List<NewsTag> relatedNewsTags = relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList());
        List<Long> tagIds = relatedNewsTags.stream().map(NewsTag::getTagId).toList();
        List<String> tagNames = relatedNewsTags.stream()
            .map(newsTag -> relatedData.tagMap().get(newsTag.getTagId()))
            .filter(Objects::nonNull)
            .map(Tag::getName)
            .toList();

        AdminNewsListVO vo = new AdminNewsListVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setSourceName(news.getSourceName());
        vo.setCoverImage(news.getCoverImage());
        vo.setCategoryId(news.getCategoryId());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setTagIds(tagIds);
        vo.setTagNames(tagNames);
        vo.setPublishTime(news.getPublishTime());
        vo.setStatus(news.getStatus());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private AdminNewsDetailVO toAdminNewsDetailVO(News news, RelatedData relatedData) {
        List<NewsTag> relatedNewsTags = relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList());
        List<Long> tagIds = relatedNewsTags.stream().map(NewsTag::getTagId).toList();
        List<String> tagNames = relatedNewsTags.stream()
            .map(newsTag -> relatedData.tagMap().get(newsTag.getTagId()))
            .filter(Objects::nonNull)
            .map(Tag::getName)
            .toList();

        AdminNewsDetailVO vo = new AdminNewsDetailVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setContent(news.getContent());
        vo.setSourceName(news.getSourceName());
        vo.setSourceUrl(news.getSourceUrl());
        vo.setCoverImage(news.getCoverImage());
        vo.setCategoryId(news.getCategoryId());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setTagIds(tagIds);
        vo.setTagNames(tagNames);
        vo.setPublishTime(news.getPublishTime());
        vo.setCrawlTime(news.getCrawlTime());
        vo.setStatus(news.getStatus());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private NewsListVO toNewsListVO(News news, RelatedData relatedData) {
        List<String> tagNames = relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList()).stream()
            .map(newsTag -> relatedData.tagMap().get(newsTag.getTagId()))
            .filter(Objects::nonNull)
            .map(Tag::getName)
            .toList();

        NewsListVO vo = new NewsListVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setCoverImage(news.getCoverImage());
        vo.setSourceName(news.getSourceName());
        vo.setCategoryId(news.getCategoryId());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setTagNames(tagNames);
        vo.setPublishTime(news.getPublishTime());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private NewsDetailVO toNewsDetailVO(News news, RelatedData relatedData, NewsActionStatusVO actionStatus) {
        List<String> tagNames = relatedData.newsTagMap().getOrDefault(news.getId(), Collections.emptyList()).stream()
            .map(newsTag -> relatedData.tagMap().get(newsTag.getTagId()))
            .filter(Objects::nonNull)
            .map(Tag::getName)
            .toList();

        NewsDetailVO vo = new NewsDetailVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setSummary(news.getSummary());
        vo.setContent(news.getContent());
        vo.setCoverImage(news.getCoverImage());
        vo.setSourceName(news.getSourceName());
        vo.setSourceUrl(news.getSourceUrl());
        vo.setCategoryId(news.getCategoryId());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setTagNames(tagNames);
        vo.setPublishTime(news.getPublishTime());
        vo.setViewCount(news.getViewCount());
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        vo.setCommentCount(news.getCommentCount());
        vo.setHeatScore(news.getHeatScore());
        vo.setLiked(actionStatus.getLiked());
        vo.setFavorited(actionStatus.getFavorited());
        vo.setDisliked(actionStatus.getDisliked());
        return vo;
    }

    private NewsActionStatusVO createAnonymousActionStatus(News news) {
        NewsActionStatusVO vo = new NewsActionStatusVO();
        vo.setLiked(false);
        vo.setFavorited(false);
        vo.setDisliked(false);
        vo.setLikeCount(news.getLikeCount());
        vo.setFavoriteCount(news.getFavoriteCount());
        return vo;
    }

    private HotNewsVO toHotNewsVO(News news, RelatedData relatedData) {
        HotNewsVO vo = new HotNewsVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setCoverImage(news.getCoverImage());
        vo.setSourceName(news.getSourceName());
        vo.setCategoryName(relatedData.categoryMap().get(news.getCategoryId()) != null
            ? relatedData.categoryMap().get(news.getCategoryId()).getName()
            : null);
        vo.setPublishTime(news.getPublishTime());
        vo.setHeatScore(news.getHeatScore());
        return vo;
    }

    private record RelatedData(
        Map<Long, Category> categoryMap,
        Map<Long, Tag> tagMap,
        Map<Long, List<NewsTag>> newsTagMap
    ) {
    }
}
