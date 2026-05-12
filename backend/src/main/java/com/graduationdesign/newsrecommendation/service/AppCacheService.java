package com.graduationdesign.newsrecommendation.service;

import com.fasterxml.jackson.databind.JavaType;
import java.time.Duration;
import java.util.function.Supplier;

public interface AppCacheService {

    <T> T getOrLoad(String key, JavaType javaType, Duration ttl, Supplier<T> loader);

    void evict(String key);

    void evictByPrefix(String prefix);
}
