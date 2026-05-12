package com.graduationdesign.newsrecommendation.service.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationdesign.newsrecommendation.service.AppCacheService;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisAppCacheService implements AppCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisAppCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAppCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T getOrLoad(String key, JavaType javaType, Duration ttl, Supplier<T> loader) {
        try {
            String cachedValue = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cachedValue)) {
                return objectMapper.readValue(cachedValue, javaType);
            }
        } catch (Exception exception) {
            log.warn("Failed to read Redis cache for key {}", key, exception);
        }

        T loadedValue = loader.get();
        if (loadedValue == null) {
            return null;
        }

        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(loadedValue), ttl);
        } catch (Exception exception) {
            log.warn("Failed to write Redis cache for key {}", key, exception);
        }

        return loadedValue;
    }

    @Override
    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("Failed to evict Redis cache for key {}", key, exception);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            log.warn("Failed to evict Redis cache for prefix {}", prefix, exception);
        }
    }
}
