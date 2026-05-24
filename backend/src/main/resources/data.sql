INSERT INTO `category` (`id`, `name`, `code`, `description`, `sort_order`, `status`)
VALUES
    (1, '前沿科技', 'FRONTIER_TECH', '聚焦 AI、大模型、互联网产品等前沿科技资讯', 1, 1),
    (2, '成长学习', 'GROWTH_LEARNING', '聚焦英语学习、自学方法等成长型内容', 2, 1),
    (3, '职业机会', 'CAREER_OPPORTUNITY', '聚焦新职业、副业与职业发展机会', 3, 1),
    (4, '数字生活', 'DIGITAL_LIFE', '聚焦效率工具、远程办公等数字生活内容', 4, 1),
    (5, '热点趋势', 'HOT_TREND', '聚焦海外趋势与面向年轻人的热门趋势资讯', 5, 1)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `sort_order` = VALUES(`sort_order`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `tag` (`id`, `category_id`, `name`, `code`, `sort_order`, `status`)
VALUES
    (1, 1, 'AI', 'AI', 1, 1),
    (2, 1, '大模型', 'LLM', 2, 1),
    (3, 2, '效率工具', 'EFFICIENCY_TOOL', 3, 1),
    (4, 2, '英语学习', 'ENGLISH_LEARNING', 1, 1),
    (5, 2, '自学方法', 'SELF_LEARNING_METHOD', 2, 1),
    (6, 3, '新职业', 'NEW_CAREER', 2, 1),
    (7, 3, '副业', 'SIDE_HUSTLE', 3, 1),
    (8, 3, '远程办公', 'REMOTE_WORK', 4, 1),
    (9, 1, '互联网产品', 'INTERNET_PRODUCT', 7, 1),
    (10, 5, '海外趋势', 'OVERSEAS_TREND', 5, 1),
    (11, 1, '机器人', 'ROBOT', 3, 1),
    (12, 1, '芯片', 'CHIP', 4, 1),
    (13, 1, '开源项目', 'OPEN_SOURCE_PROJECT', 5, 1),
    (14, 1, '科技公司', 'TECH_COMPANY', 6, 1),
    (15, 2, '考研考证', 'EXAM_CERTIFICATE', 4, 1),
    (16, 2, '阅读写作', 'READING_WRITING', 5, 1),
    (17, 2, '知识管理', 'KNOWLEDGE_MANAGEMENT', 6, 1),
    (18, 3, '实习就业', 'INTERNSHIP_EMPLOYMENT', 1, 1),
    (19, 3, '职场成长', 'CAREER_GROWTH', 5, 1),
    (20, 3, '简历面试', 'RESUME_INTERVIEW', 6, 1),
    (21, 4, '数码产品', 'DIGITAL_PRODUCT', 1, 1),
    (22, 4, 'App推荐', 'APP_RECOMMENDATION', 2, 1),
    (23, 4, '智能硬件', 'SMART_HARDWARE', 3, 1),
    (24, 4, '软件工具', 'SOFTWARE_TOOL', 4, 1),
    (25, 4, '生产力工具', 'PRODUCTIVITY_TOOL', 5, 1),
    (26, 4, '消费科技', 'CONSUMER_TECH', 6, 1),
    (27, 5, '社会热点', 'SOCIAL_HOT_TOPIC', 1, 1),
    (28, 5, '青年话题', 'YOUTH_TOPIC', 2, 1),
    (29, 5, '平台动态', 'PLATFORM_DYNAMIC', 3, 1),
    (30, 5, '商业趋势', 'BUSINESS_TREND', 4, 1)
ON DUPLICATE KEY UPDATE
    `category_id` = VALUES(`category_id`),
    `name` = VALUES(`name`),
    `sort_order` = VALUES(`sort_order`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `phone`, `role`, `status`)
VALUES
    (1, 'admin', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', '系统管理员', NULL, 'admin@news.local', NULL, 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `email` = VALUES(`email`),
    `phone` = VALUES(`phone`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `phone`, `role`, `status`)
VALUES
    (101, 'ai_learner', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', 'AI学习者', NULL, 'ai_learner@news.local', NULL, 'USER', 1),
    (102, 'career_user', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', '求职观察员', NULL, 'career_user@news.local', NULL, 'USER', 1),
    (103, 'digital_user', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', '数码生活家', NULL, 'digital_user@news.local', NULL, 'USER', 1),
    (104, 'trend_user', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', '热点追踪者', NULL, 'trend_user@news.local', NULL, 'USER', 1),
    (105, 'disabled_user', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', '测试禁用用户', NULL, 'disabled_user@news.local', NULL, 'USER', 0)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `email` = VALUES(`email`),
    `phone` = VALUES(`phone`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

DELETE FROM `comment`
WHERE `news_id` IN (
    SELECT `id` FROM `news` WHERE `source_url` LIKE 'https://news.example.com/%'
);

DELETE FROM `user_behavior`
WHERE `news_id` IN (
    SELECT `id` FROM `news` WHERE `source_url` LIKE 'https://news.example.com/%'
);

DELETE FROM `news_tag`
WHERE `news_id` IN (
    SELECT `id` FROM `news` WHERE `source_url` LIKE 'https://news.example.com/%'
);

DELETE FROM `news`
WHERE `source_url` LIKE 'https://news.example.com/%';

UPDATE `news`
SET `cover_image` = CASE `category_id`
    WHEN 1 THEN '/news-covers/frontier-tech.svg'
    WHEN 2 THEN '/news-covers/growth-learning.svg'
    WHEN 3 THEN '/news-covers/career-opportunity.svg'
    WHEN 4 THEN '/news-covers/digital-life.svg'
    ELSE '/news-covers/hot-trend.svg'
END
WHERE `cover_image` IS NULL OR TRIM(`cover_image`) = '';

INSERT INTO `crawl_config` (
    `id`, `source_name`, `source_url`, `source_type`, `category_id`, `enabled`,
    `crawl_interval`, `last_crawl_time`, `last_crawl_count`, `last_status`, `last_error`
)
VALUES
    (101, '少数派 RSS', 'https://sspai.com/feed', 'RSS', 4, 1, 60, NULL, 0, 'READY', NULL),
    (102, 'IT之家科技资讯', 'https://www.ithome.com/rss/', 'RSS', 1, 1, 45, NULL, 0, 'READY', NULL),
    (103, '中新网即时新闻', 'https://www.chinanews.com.cn/rss/scroll-news.xml', 'RSS', 5, 0, 90, NULL, 0, 'READY', '备用综合新闻源，演示时可按需启用'),
    (104, '36氪快讯 RSSHub', 'https://rsshub.app/36kr/newsflashes', 'RSS', 1, 0, 60, '2026-05-24 17:56:00', 0, 'FAILED', '公共 RSSHub 节点连接超时，已作为备用源保留')
ON DUPLICATE KEY UPDATE
    `source_name` = VALUES(`source_name`),
    `source_url` = VALUES(`source_url`),
    `source_type` = VALUES(`source_type`),
    `category_id` = VALUES(`category_id`),
    `enabled` = VALUES(`enabled`),
    `crawl_interval` = VALUES(`crawl_interval`),
    `last_crawl_time` = VALUES(`last_crawl_time`),
    `last_crawl_count` = VALUES(`last_crawl_count`),
    `last_status` = VALUES(`last_status`),
    `last_error` = VALUES(`last_error`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `user_interest` (`user_id`, `tag_id`, `weight`)
VALUES
    (101, 1, 1.6), (101, 2, 1.4), (101, 5, 1.0), (101, 13, 1.0),
    (102, 18, 1.6), (102, 20, 1.4), (102, 8, 1.2), (102, 19, 1.0),
    (103, 21, 1.5), (103, 22, 1.4), (103, 24, 1.3), (103, 25, 1.0),
    (104, 27, 1.5), (104, 28, 1.3), (104, 29, 1.2), (104, 30, 1.0),
    (105, 4, 1.0), (105, 10, 1.0)
ON DUPLICATE KEY UPDATE
    `weight` = VALUES(`weight`),
    `updated_at` = CURRENT_TIMESTAMP;
