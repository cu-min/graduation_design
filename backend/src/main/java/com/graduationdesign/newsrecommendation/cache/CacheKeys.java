package com.graduationdesign.newsrecommendation.cache;

public final class CacheKeys {

    public static final String PUBLIC_CATEGORIES = "metadata:categories:public";
    public static final String PUBLIC_TAGS_PREFIX = "metadata:tags:public:";
    public static final String HOT_NEWS_PREFIX = "news:hot:";
    public static final String RECOMMEND_PREFIX = "recommend:user:";

    private CacheKeys() {
    }

    public static String publicTags(Long categoryId) {
        return PUBLIC_TAGS_PREFIX + (categoryId == null ? "all" : categoryId);
    }

    public static String hotNews(int limit) {
        return HOT_NEWS_PREFIX + limit;
    }

    public static String recommend(Long userId, long page, long size) {
        return RECOMMEND_PREFIX + (userId == null ? "anonymous" : userId) + ":" + page + ":" + size;
    }
}
