package com.graduationdesign.newsrecommendation.service.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.graduationdesign.newsrecommendation.service.AppCacheService;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "false")
public class NoOpAppCacheService implements AppCacheService {

    @Override
    public <T> T getOrLoad(String key, JavaType javaType, Duration ttl, Supplier<T> loader) {
        return loader.get();
    }

    @Override
    public void evict(String key) {
        // no-op when cache is disabled
    }

    @Override
    public void evictByPrefix(String prefix) {
        // no-op when cache is disabled
    }
}
