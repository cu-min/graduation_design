package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationdesign.newsrecommendation.cache.CacheKeys;
import com.graduationdesign.newsrecommendation.dto.AdminCategoryRequest;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.CrawlConfigMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.service.AppCacheService;
import com.graduationdesign.newsrecommendation.service.CacheInvalidationService;
import com.graduationdesign.newsrecommendation.service.CategoryService;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private static final Duration PUBLIC_CATEGORY_CACHE_TTL = Duration.ofMinutes(30);

    private final TagMapper tagMapper;
    private final NewsMapper newsMapper;
    private final CrawlConfigMapper crawlConfigMapper;
    private final AppCacheService appCacheService;
    private final CacheInvalidationService cacheInvalidationService;
    private final ObjectMapper objectMapper;

    public CategoryServiceImpl(
        TagMapper tagMapper,
        NewsMapper newsMapper,
        CrawlConfigMapper crawlConfigMapper,
        AppCacheService appCacheService,
        CacheInvalidationService cacheInvalidationService,
        ObjectMapper objectMapper
    ) {
        this.tagMapper = tagMapper;
        this.newsMapper = newsMapper;
        this.crawlConfigMapper = crawlConfigMapper;
        this.appCacheService = appCacheService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Category> listPublicCategories() {
        return appCacheService.getOrLoad(
            CacheKeys.PUBLIC_CATEGORIES,
            objectMapper.getTypeFactory().constructCollectionType(List.class, Category.class),
            PUBLIC_CATEGORY_CACHE_TTL,
            () -> list(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId))
        );
    }

    @Override
    public List<Category> listAdminCategories() {
        return list(new LambdaQueryWrapper<Category>()
            .orderByAsc(Category::getSortOrder)
            .orderByAsc(Category::getId));
    }

    @Override
    @Transactional
    public void createAdminCategory(AdminCategoryRequest request) {
        validateStatus(request.getStatus());
        validateUniqueCode(request.getCode(), null);

        Category category = new Category();
        fillCategory(category, request);
        save(category);
        cacheInvalidationService.evictPublicContentCaches();
    }

    @Override
    @Transactional
    public void updateAdminCategory(Long id, AdminCategoryRequest request) {
        validateStatus(request.getStatus());
        Category category = getByIdOrThrow(id);
        validateUniqueCode(request.getCode(), id);

        fillCategory(category, request);
        updateById(category);
        cacheInvalidationService.evictPublicContentCaches();
    }

    @Override
    @Transactional
    public void deleteAdminCategory(Long id) {
        getByIdOrThrow(id);
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>().eq(Tag::getCategoryId, id)) > 0) {
            throw new IllegalArgumentException("Current category still has related tags and cannot be deleted");
        }
        if (newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getCategoryId, id)) > 0) {
            throw new IllegalArgumentException("Current category still has related news and cannot be deleted");
        }
        if (crawlConfigMapper.selectCount(new LambdaQueryWrapper<CrawlConfig>().eq(CrawlConfig::getCategoryId, id)) > 0) {
            throw new IllegalArgumentException("Current category is referenced by crawl configs and cannot be deleted");
        }
        removeById(id);
        cacheInvalidationService.evictPublicContentCaches();
    }

    @Override
    @Transactional
    public void updateAdminCategoryStatus(Long id, Integer status) {
        validateStatus(status);
        Category category = getByIdOrThrow(id);
        category.setStatus(status);
        updateById(category);
        cacheInvalidationService.evictPublicContentCaches();
    }

    private Category getByIdOrThrow(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new NotFoundException("Category does not exist");
        }
        return category;
    }

    private void fillCategory(Category category, AdminCategoryRequest request) {
        category.setName(request.getName().trim());
        category.setCode(request.getCode().trim());
        category.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        category.setSortOrder(request.getSortOrder());
        category.setStatus(request.getStatus());
    }

    private void validateUniqueCode(String code, Long excludeId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<Category>()
            .eq(Category::getCode, code.trim());
        if (excludeId != null) {
            queryWrapper.ne(Category::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new IllegalArgumentException("Category code already exists");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("Status must be 0 or 1");
        }
    }
}
