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

-- Demo accounts use the plaintext test password: 123456
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `phone`, `role`, `status`)
VALUES
    (1, 'admin', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', '系统管理员', 'https://api.dicebear.com/9.x/initials/svg?seed=ADMIN', 'admin@news.local', '13800000001', 'ADMIN', 1),
    (101, 'ai_learner', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', 'AI学习者', 'https://api.dicebear.com/9.x/initials/svg?seed=AI', 'ai_learner@news.local', '13800000101', 'USER', 1),
    (102, 'career_user', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', '求职观察员', 'https://api.dicebear.com/9.x/initials/svg?seed=JOB', 'career_user@news.local', '13800000102', 'USER', 1),
    (103, 'digital_user', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', '数码生活家', 'https://api.dicebear.com/9.x/initials/svg?seed=APP', 'digital_user@news.local', '13800000103', 'USER', 1),
    (104, 'trend_user', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', '热点追踪者', 'https://api.dicebear.com/9.x/initials/svg?seed=TREND', 'trend_user@news.local', '13800000104', 'USER', 1),
    (105, 'disabled_user', '$2a$10$P7MjDfN.VtlQ8Gk4GQ0FdO.pv6RnpFmupedopCJAPN0BPnuLAKHqS', '测试禁用用户', 'https://api.dicebear.com/9.x/initials/svg?seed=OFF', 'disabled_user@news.local', '13800000105', 'USER', 0)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `email` = VALUES(`email`),
    `phone` = VALUES(`phone`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `crawl_config` (
    `id`, `source_name`, `source_url`, `source_type`, `category_id`, `enabled`,
    `crawl_interval`, `last_crawl_time`, `last_crawl_count`, `last_status`, `last_error`
)
VALUES
    (101, '少数派 RSS', 'https://sspai.com/feed', 'RSS', 4, 1, 60, CURRENT_TIMESTAMP - INTERVAL 3 HOUR, 8, 'SUCCESS', NULL),
    (102, 'IT之家科技资讯', 'https://www.ithome.com/rss/', 'RSS', 1, 1, 45, CURRENT_TIMESTAMP - INTERVAL 2 HOUR, 12, 'SUCCESS', NULL),
    (103, '中新网即时新闻', 'https://www.chinanews.com.cn/rss/scroll-news.xml', 'RSS', 5, 0, 90, NULL, 0, 'READY', '备用综合新闻源，演示时可按需启用'),
    (104, '36氪快讯 RSSHub', 'https://rsshub.app/36kr/newsflashes', 'RSS', 5, 0, 60, CURRENT_TIMESTAMP - INTERVAL 1 DAY, 0, 'FAILED', '公共 RSSHub 节点连接超时，已作为备用源保留'),
    (105, '爱范儿 RSS', 'https://www.ifanr.com/feed', 'RSS', 4, 1, 60, CURRENT_TIMESTAMP - INTERVAL 5 HOUR, 6, 'SUCCESS', NULL)
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
    (101, 1, 5.0), (101, 2, 4.8), (101, 3, 3.6), (101, 13, 3.2), (101, 25, 2.8),
    (102, 18, 5.0), (102, 20, 4.7), (102, 8, 4.1), (102, 19, 3.6), (102, 6, 3.2),
    (103, 21, 4.8), (103, 22, 4.6), (103, 24, 4.2), (103, 25, 3.8), (103, 26, 3.1),
    (104, 27, 4.7), (104, 28, 3.8), (104, 29, 4.5), (104, 30, 4.3), (104, 10, 3.2),
    (105, 4, 2.0), (105, 10, 1.5), (105, 16, 1.0)
ON DUPLICATE KEY UPDATE
    `weight` = VALUES(`weight`),
    `updated_at` = CURRENT_TIMESTAMP;

DELETE FROM `comment`
WHERE `user_id` IN (101, 102, 103, 104, 105)
   OR `news_id` IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015);

DELETE FROM `user_behavior`
WHERE `user_id` IN (101, 102, 103, 104, 105)
   OR `news_id` IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015);

DELETE FROM `news_tag`
WHERE `news_id` IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015);

DELETE FROM `news`
WHERE `id` IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015)
   OR `source_url` LIKE 'https://news.example.com/%';

INSERT INTO `news` (
    `id`, `title`, `summary`, `content`, `source_name`, `source_url`, `cover_image`,
    `category_id`, `publish_time`, `crawl_time`, `status`, `view_count`, `like_count`,
    `favorite_count`, `comment_count`, `heat_score`
)
VALUES
    (1001, '多模态助手进入校园学习场景', '多模态模型开始覆盖资料整理、课堂复盘和论文阅读等学习流程。', '多模态助手正在从单点问答走向完整学习流程：识别课件、总结课堂录音、提炼论文结构，并把结果沉淀到个人知识库。对学生而言，关键不只是提速，而是建立可复查、可追踪的学习记录。', '演示科技周报', 'https://news.example.com/frontier/ai-campus-assistant', '/news-covers/tech.svg', 1, CURRENT_TIMESTAMP - INTERVAL 6 HOUR, CURRENT_TIMESTAMP - INTERVAL 5 HOUR, 1, 326, 4, 2, 0, 93.6),
    (1002, '开源大模型部署转向轻量化', '越来越多团队把注意力放在小参数模型、端侧推理和私有化知识库。', '开源大模型生态继续降本，企业更关注可控部署、数据隔离和响应速度。轻量化模型配合检索增强，正在成为中小团队落地智能客服、内部助手和内容分析的务实路径。', '模型观察', 'https://news.example.com/frontier/lightweight-llm', '/news-covers/tech.svg', 1, CURRENT_TIMESTAMP - INTERVAL 1 DAY, CURRENT_TIMESTAMP - INTERVAL 23 HOUR, 1, 288, 3, 2, 0, 88.4),
    (1003, 'AI 编程工具开始重塑个人开发流程', '从补全代码到生成测试，AI 编程工具正在改变小团队交付节奏。', 'AI 编程工具的价值正在从“写得更快”转向“验证得更稳”。在个人项目和毕业设计中，自动生成测试、解释报错和整理变更记录，能显著降低调试成本。', '开发者日报', 'https://news.example.com/frontier/ai-coding-workflow', '/news-covers/tech.svg', 1, CURRENT_TIMESTAMP - INTERVAL 2 DAY, CURRENT_TIMESTAMP - INTERVAL 47 HOUR, 1, 241, 2, 1, 0, 84.9),
    (1004, '费曼笔记法在碎片学习中重新流行', '学习类社区重新讨论如何把短内容转化为可复用知识。', '碎片化阅读不必然导致低效，关键在于输出机制。把复杂概念改写成自己的解释，再用例子补足漏洞，可以让短视频、文章和课程片段变成长期知识资产。', '成长研究室', 'https://news.example.com/growth/feynman-note-loop', '/news-covers/growth.svg', 2, CURRENT_TIMESTAMP - INTERVAL 8 HOUR, CURRENT_TIMESTAMP - INTERVAL 7 HOUR, 1, 198, 2, 1, 0, 78.2),
    (1005, '大学生效率工具清单更新', '任务管理、稍后读和知识库工具形成更清晰的组合方式。', '新的效率工具组合更强调低摩擦记录：任务进入清单，资料进入稍后读，长期内容进入知识库。相比追求复杂系统，稳定执行和定期清理更适合学生群体。', '效率工具箱', 'https://news.example.com/growth/student-productivity-stack', '/news-covers/growth.svg', 2, CURRENT_TIMESTAMP - INTERVAL 1 DAY, CURRENT_TIMESTAMP - INTERVAL 22 HOUR, 1, 214, 2, 2, 0, 80.5),
    (1006, '英语学习 App 增加 AI 口语陪练', 'AI 陪练降低了开口门槛，但反馈质量仍需要结合真实语境判断。', 'AI 口语陪练可以提供即时纠错和情景对话，适合日常热身和表达练习。学习者仍需要结合真实材料输入，避免只在模板化对话里循环。', '学习前线', 'https://news.example.com/growth/ai-english-practice', '/news-covers/growth.svg', 2, CURRENT_TIMESTAMP - INTERVAL 3 DAY, CURRENT_TIMESTAMP - INTERVAL 71 HOUR, 1, 167, 1, 1, 0, 70.4),
    (1007, '春招实习岗位更看重项目闭环能力', '企业在筛选实习生时更关注项目目标、实现过程和结果复盘。', '近期实习招聘中，完整项目经历比单点技能描述更容易被追问。候选人可以用背景、行动、结果和反思来组织作品集，突出自己如何发现问题并完成交付。', '职业机会观察', 'https://news.example.com/career/internship-project-loop', '/news-covers/career.svg', 3, CURRENT_TIMESTAMP - INTERVAL 4 HOUR, CURRENT_TIMESTAMP - INTERVAL 3 HOUR, 1, 305, 3, 3, 0, 91.1),
    (1008, '远程办公岗位要求更清晰的异步协作能力', '招聘信息中开始频繁出现文档协作、时区沟通和自驱管理。', '远程办公并不只是地点自由，它要求候选人能用文档同步进展、拆解任务并主动暴露风险。对求职者来说，展示异步沟通样例和项目管理习惯会更有说服力。', '远程工作简报', 'https://news.example.com/career/remote-async-work', '/news-covers/career.svg', 3, CURRENT_TIMESTAMP - INTERVAL 1 DAY, CURRENT_TIMESTAMP - INTERVAL 21 HOUR, 1, 246, 2, 2, 0, 83.7),
    (1009, '简历中的 AI 项目如何避免同质化', '面试官更希望看到真实场景、数据来源和可验证效果。', '随着 AI 项目大量出现在简历中，单纯写“接入大模型”已经不够。更好的写法是说明问题场景、技术取舍、评估方式和上线后的限制。', '求职方法论', 'https://news.example.com/career/ai-project-resume', '/news-covers/career.svg', 3, CURRENT_TIMESTAMP - INTERVAL 2 DAY, CURRENT_TIMESTAMP - INTERVAL 46 HOUR, 1, 221, 2, 1, 0, 79.6),
    (1010, '国产笔记应用加强跨端同步体验', '数字生活类产品继续围绕低延迟同步和离线可用做优化。', '笔记应用竞争正在回到基础体验：打开速度、跨端同步、附件管理和搜索准确性。对重度用户来说，稳定比花哨模板更重要。', '数字生活编辑部', 'https://news.example.com/digital/note-app-sync', '/news-covers/digital.svg', 4, CURRENT_TIMESTAMP - INTERVAL 7 HOUR, CURRENT_TIMESTAMP - INTERVAL 6 HOUR, 1, 274, 3, 3, 0, 87.8),
    (1011, 'App 推荐社区流行“低打扰”工具', '用户更偏好通知少、权限清晰、专注单一任务的小工具。', '数字生活社区的讨论从全能套件转向低打扰工具。轻量截图、剪贴板管理、专注计时和阅读稍后处理，成为提升日常效率的常见选择。', 'App 观察', 'https://news.example.com/digital/quiet-tools', '/news-covers/digital.svg', 4, CURRENT_TIMESTAMP - INTERVAL 1 DAY, CURRENT_TIMESTAMP - INTERVAL 20 HOUR, 1, 232, 2, 2, 0, 82.1),
    (1012, '智能硬件新品强调健康数据整合', '可穿戴设备开始把睡眠、运动和压力指标放进统一仪表盘。', '新一代智能硬件更关注数据解释而不是单纯采集。设备厂商尝试把睡眠、运动、心率和压力趋势整合为可执行建议，帮助用户建立长期生活习惯。', '消费科技日报', 'https://news.example.com/digital/smart-health-device', '/news-covers/digital.svg', 4, CURRENT_TIMESTAMP - INTERVAL 3 DAY, CURRENT_TIMESTAMP - INTERVAL 70 HOUR, 1, 176, 1, 1, 0, 73.9),
    (1013, '内容平台调整推荐规则，鼓励深度互动', '平台开始把收藏、长阅读和评论质量纳入更重要的分发信号。', '热点平台的推荐规则继续演化，单纯点击不再代表满意度。收藏、停留、分享和高质量评论成为判断内容价值的重要指标。', '平台动态追踪', 'https://news.example.com/trend/platform-ranking-signal', '/news-covers/trend.svg', 5, CURRENT_TIMESTAMP - INTERVAL 5 HOUR, CURRENT_TIMESTAMP - INTERVAL 4 HOUR, 1, 352, 4, 3, 0, 95.2),
    (1014, '年轻用户重新关注个人品牌建设', '从作品集主页到公开学习记录，青年群体更重视长期展示。', '个人品牌不再只属于创作者。学生和职场新人通过作品集、技术博客和公开学习记录展示成长轨迹，也让机会发现更具连续性。', '青年趋势观察', 'https://news.example.com/trend/personal-brand-youth', '/news-covers/trend.svg', 5, CURRENT_TIMESTAMP - INTERVAL 1 DAY, CURRENT_TIMESTAMP - INTERVAL 19 HOUR, 1, 267, 2, 2, 0, 85.6),
    (1015, 'AI 订阅服务进入精细化竞争', '工具厂商开始用场景包、团队套餐和教育优惠区分用户。', 'AI 订阅服务的竞争从模型能力扩展到套餐设计。面向学生、团队和创作者的差异化权益，正在影响用户如何选择长期工具。', '商业趋势简报', 'https://news.example.com/trend/ai-subscription-market', '/news-covers/trend.svg', 5, CURRENT_TIMESTAMP - INTERVAL 2 DAY, CURRENT_TIMESTAMP - INTERVAL 44 HOUR, 1, 238, 2, 1, 0, 81.7);

INSERT INTO `news_tag` (`news_id`, `tag_id`)
VALUES
    (1001, 1), (1001, 2), (1001, 3),
    (1002, 2), (1002, 13), (1002, 14),
    (1003, 1), (1003, 3), (1003, 25),
    (1004, 5), (1004, 16), (1004, 17),
    (1005, 3), (1005, 17), (1005, 25),
    (1006, 4), (1006, 5), (1006, 22),
    (1007, 18), (1007, 19), (1007, 20),
    (1008, 8), (1008, 19), (1008, 24),
    (1009, 1), (1009, 18), (1009, 20),
    (1010, 21), (1010, 24), (1010, 25),
    (1011, 22), (1011, 24), (1011, 25),
    (1012, 21), (1012, 23), (1012, 26),
    (1013, 27), (1013, 29), (1013, 30),
    (1014, 28), (1014, 29), (1014, 30),
    (1015, 1), (1015, 10), (1015, 30)
ON DUPLICATE KEY UPDATE
    `tag_id` = VALUES(`tag_id`);

INSERT INTO `user_behavior` (`user_id`, `news_id`, `action_type`, `action_weight`, `duration`, `created_at`)
VALUES
    (101, 1001, 'VIEW', 1.0, 180, CURRENT_TIMESTAMP - INTERVAL 5 HOUR),
    (101, 1002, 'VIEW', 1.0, 160, CURRENT_TIMESTAMP - INTERVAL 1 DAY),
    (101, 1003, 'VIEW', 1.0, 210, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (101, 1005, 'VIEW', 1.0, 96, CURRENT_TIMESTAMP - INTERVAL 3 DAY),
    (101, 1001, 'LIKE', 3.0, 0, CURRENT_TIMESTAMP - INTERVAL 4 HOUR),
    (101, 1003, 'FAVORITE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (101, 1002, 'SHARE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 22 HOUR),
    (101, 1014, 'DISLIKE', -5.0, 0, CURRENT_TIMESTAMP - INTERVAL 1 DAY),
    (102, 1007, 'VIEW', 1.0, 230, CURRENT_TIMESTAMP - INTERVAL 3 HOUR),
    (102, 1008, 'VIEW', 1.0, 175, CURRENT_TIMESTAMP - INTERVAL 23 HOUR),
    (102, 1009, 'VIEW', 1.0, 205, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (102, 1015, 'VIEW', 1.0, 82, CURRENT_TIMESTAMP - INTERVAL 3 DAY),
    (102, 1007, 'LIKE', 3.0, 0, CURRENT_TIMESTAMP - INTERVAL 2 HOUR),
    (102, 1008, 'FAVORITE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 21 HOUR),
    (102, 1009, 'SHARE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 1 DAY),
    (102, 1012, 'DISLIKE', -5.0, 0, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (103, 1010, 'VIEW', 1.0, 190, CURRENT_TIMESTAMP - INTERVAL 6 HOUR),
    (103, 1011, 'VIEW', 1.0, 155, CURRENT_TIMESTAMP - INTERVAL 20 HOUR),
    (103, 1012, 'VIEW', 1.0, 130, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (103, 1005, 'VIEW', 1.0, 120, CURRENT_TIMESTAMP - INTERVAL 3 DAY),
    (103, 1010, 'LIKE', 3.0, 0, CURRENT_TIMESTAMP - INTERVAL 5 HOUR),
    (103, 1011, 'FAVORITE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 18 HOUR),
    (103, 1011, 'SHARE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 17 HOUR),
    (104, 1013, 'VIEW', 1.0, 220, CURRENT_TIMESTAMP - INTERVAL 4 HOUR),
    (104, 1014, 'VIEW', 1.0, 170, CURRENT_TIMESTAMP - INTERVAL 18 HOUR),
    (104, 1015, 'VIEW', 1.0, 165, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (104, 1002, 'VIEW', 1.0, 105, CURRENT_TIMESTAMP - INTERVAL 3 DAY),
    (104, 1013, 'LIKE', 3.0, 0, CURRENT_TIMESTAMP - INTERVAL 3 HOUR),
    (104, 1013, 'FAVORITE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 3 HOUR),
    (104, 1014, 'SHARE', 5.0, 0, CURRENT_TIMESTAMP - INTERVAL 16 HOUR),
    (104, 1006, 'DISLIKE', -5.0, 0, CURRENT_TIMESTAMP - INTERVAL 1 DAY);

INSERT INTO `comment` (`news_id`, `user_id`, `parent_id`, `content`, `status`, `created_at`)
VALUES
    (1001, 101, NULL, '多模态学习助手这个方向很适合做课堂资料整理，如果能和个人知识库打通会更完整。', 1, CURRENT_TIMESTAMP - INTERVAL 4 HOUR),
    (1003, 101, NULL, 'AI 编程工具真正有价值的是帮忙补测试和解释报错，不只是生成代码。', 1, CURRENT_TIMESTAMP - INTERVAL 2 DAY),
    (1007, 102, NULL, '项目闭环确实比堆技术栈更能打动面试官，答辩项目也可以按这个思路讲。', 1, CURRENT_TIMESTAMP - INTERVAL 3 HOUR),
    (1008, 102, NULL, '远程岗位对文档能力要求很高，简历里可以放一段协作说明。', 1, CURRENT_TIMESTAMP - INTERVAL 20 HOUR),
    (1010, 103, NULL, '笔记应用同步稳定性太重要了，跨设备延迟会直接影响使用习惯。', 1, CURRENT_TIMESTAMP - INTERVAL 5 HOUR),
    (1011, 103, NULL, '低打扰工具反而更容易长期留下来，通知少是很大的优点。', 1, CURRENT_TIMESTAMP - INTERVAL 18 HOUR),
    (1013, 104, NULL, '把收藏和长阅读纳入推荐信号很合理，比单纯点击更能代表真实兴趣。', 1, CURRENT_TIMESTAMP - INTERVAL 3 HOUR),
    (1015, 104, NULL, 'AI 订阅如果能给学生更明确的场景包，转化率应该会更好。', 1, CURRENT_TIMESTAMP - INTERVAL 1 DAY);

UPDATE `news` n
SET `comment_count` = (
    SELECT COUNT(*)
    FROM `comment` c
    WHERE c.`news_id` = n.`id`
      AND c.`status` = 1
)
WHERE n.`id` IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015);

UPDATE `news`
SET `cover_image` = CASE `category_id`
    WHEN 1 THEN '/news-covers/tech.svg'
    WHEN 2 THEN '/news-covers/growth.svg'
    WHEN 3 THEN '/news-covers/career.svg'
    WHEN 4 THEN '/news-covers/digital.svg'
    ELSE '/news-covers/trend.svg'
END
WHERE `cover_image` IS NULL OR TRIM(`cover_image`) = '';
