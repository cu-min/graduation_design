package com.graduationdesign.newsrecommendation.service;

import com.graduationdesign.newsrecommendation.cache.CacheKeys;
import org.springframework.stereotype.Service;

@Service
public class CacheInvalidationService {

    private final AppCacheService appCacheService;

    public CacheInvalidationService(AppCacheService appCacheService) {
        this.appCacheService = appCacheService;
    }

    public void evictMetadataCaches() {
        appCacheService.evict(CacheKeys.PUBLIC_CATEGORIES);
        appCacheService.evictByPrefix(CacheKeys.PUBLIC_TAGS_PREFIX);
    }

    public void evictHotNewsCaches() {
        appCacheService.evictByPrefix(CacheKeys.HOT_NEWS_PREFIX);
    }

    public void evictRecommendCaches() {
        appCacheService.evictByPrefix(CacheKeys.RECOMMEND_PREFIX);
    }

    public void evictDiscoveryCaches() {
        evictHotNewsCaches();
        evictRecommendCaches();
    }

    public void evictPublicContentCaches() {
        evictMetadataCaches();
        evictDiscoveryCaches();
    }
}
