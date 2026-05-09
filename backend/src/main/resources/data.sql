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
    (3, 4, '效率工具', 'EFFICIENCY_TOOL', 3, 1),
    (4, 2, '英语学习', 'ENGLISH_LEARNING', 4, 1),
    (5, 2, '自学方法', 'SELF_LEARNING_METHOD', 5, 1),
    (6, 3, '新职业', 'NEW_CAREER', 6, 1),
    (7, 3, '副业', 'SIDE_HUSTLE', 7, 1),
    (8, 4, '远程办公', 'REMOTE_WORK', 8, 1),
    (9, 1, '互联网产品', 'INTERNET_PRODUCT', 9, 1),
    (10, 5, '海外趋势', 'OVERSEAS_TREND', 10, 1)
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

INSERT INTO `news` (
    `id`, `title`, `summary`, `content`, `source_name`, `source_url`, `cover_image`,
    `category_id`, `publish_time`, `crawl_time`, `status`,
    `view_count`, `like_count`, `favorite_count`, `comment_count`, `heat_score`
)
VALUES
    (1, 'AI 助手正在成为大学生第二生产力工具', '从写作润色到课程总结，校园里的 AI 使用场景正在快速扩张。', '越来越多大学生把 AI 助手当成日常学习搭档，用来整理课堂录音、生成复习提纲和打磨表达。真正拉开差距的不是是否使用 AI，而是能否把它嵌入稳定的学习流程。', '未来科技周刊', 'https://news.example.com/frontier/ai-campus-tools', 'https://images.example.com/news/01.jpg', 1, '2026-05-06 09:20:00', '2026-05-06 09:35:00', 1, 186, 38, 26, 4, 88.5),
    (2, '大模型开源竞赛进入应用落地阶段', '模型能力趋同后，真正的差异开始体现在交付效率与产品整合能力上。', '过去一年开源大模型的讨论重点是参数规模和基准成绩，但最近的关注点逐渐转向推理成本、部署门槛和企业场景适配。对于创业团队来说，应用落地速度比单点指标更关键。', '模型观察室', 'https://news.example.com/frontier/llm-open-source-delivery', 'https://images.example.com/news/02.jpg', 1, '2026-05-05 14:10:00', '2026-05-05 14:25:00', 1, 152, 29, 18, 3, 81.3),
    (3, '年轻团队用 AI 做独立产品增长实验', '小团队正在用更轻的方式验证产品方向，AI 成为试错放大器。', '从自动生成落地页到快速整理用户反馈，一些两三人的团队已经可以在一周内完成过去需要一个月的小型实验。对于独立开发者而言，AI 正在降低产品冷启动的试错成本。', '互联网产品志', 'https://news.example.com/frontier/ai-indie-product-growth', 'https://images.example.com/news/03.jpg', 1, '2026-05-04 11:40:00', '2026-05-04 12:00:00', 1, 134, 24, 16, 2, 77.9),
    (4, '手机端 AI 搜索正在重塑信息入口', '搜索不再只是跳转网页，越来越多结果直接变成可操作答案。', '在手机端场景里，用户越来越偏好直接得到结论、步骤和推荐，而不是自己打开十几个页面拼装信息。AI 搜索的价值不只是更快找到内容，更是缩短从问题到行动之间的距离。', '前沿技术日报', 'https://news.example.com/frontier/mobile-ai-search-entry', 'https://images.example.com/news/04.jpg', 1, '2026-05-03 10:05:00', '2026-05-03 10:20:00', 0, 96, 12, 8, 1, 63.7),
    (5, '30 天英语输入计划为什么更适合忙碌学生', '对很多学习者而言，稳定输入比短期爆发更容易持续。', '相比复杂计划，很多大学生更适合把英语学习拆成每日可完成的输入任务，例如精读一段、复述一条观点、记录五个表达。关键不在一次学很久，而在于每天都能完成闭环。', '青年学习报', 'https://news.example.com/growth/30-day-english-input', 'https://images.example.com/news/05.jpg', 2, '2026-05-06 07:50:00', '2026-05-06 08:05:00', 1, 143, 31, 20, 5, 79.4),
    (6, '自学者最常见的低效循环与破局方法', '收藏很多内容并不等于真正吸收，输出不足才是常见瓶颈。', '不少自学者陷入“看了很多、记了很多、却很少真的会用”的循环。解决办法通常不是继续加资料，而是尽快转向练习、复盘和公开表达，让知识在使用中定型。', '成长研究所', 'https://news.example.com/growth/self-learning-trap', 'https://images.example.com/news/06.jpg', 2, '2026-05-05 20:00:00', '2026-05-05 20:15:00', 1, 167, 42, 24, 6, 84.8),
    (7, 'AI 时代大学生该如何构建学习闭环', '会提问只是起点，真正重要的是形成检索、验证和复盘的完整流程。', 'AI 工具让获取答案变得更快，但也容易让学习停留在“看懂了”的错觉层面。更有效的方法是把 AI 当成协作对象，用它生成提纲、辅助质疑，再由自己完成归纳和表达。', '未来课堂', 'https://news.example.com/growth/ai-study-loop', 'https://images.example.com/news/07.jpg', 2, '2026-05-04 16:35:00', '2026-05-04 16:45:00', 1, 121, 26, 17, 2, 73.6),
    (8, '读写笔记工作流让长期学习更稳定', '比起零散收藏，结构化笔记更容易沉淀长期价值。', '越来越多学生开始用“阅读摘录 + 自己重写 + 周回顾”的方式管理笔记。这样的工作流虽然慢一些，却能显著减少“学过但记不住”的情况。', '学习方法论', 'https://news.example.com/growth/reading-writing-notes-flow', 'https://images.example.com/news/08.jpg', 2, '2026-05-02 19:10:00', '2026-05-02 19:25:00', 1, 88, 14, 11, 1, 61.2),
    (9, '新职业岗位正在从提示词设计转向工作流搭建', '企业开始更看重把工具串起来的人，而不是只会写单条提示词的人。', '随着 AI 工具进入实际业务，岗位需求也从“会不会用模型”转向“能不能把模型接进流程”。工作流搭建、自动化编排和跨工具协作能力，正在成为新职业的核心竞争点。', '职业新观察', 'https://news.example.com/career/new-role-workflow-builder', 'https://images.example.com/news/09.jpg', 3, '2026-05-06 13:05:00', '2026-05-06 13:18:00', 1, 175, 36, 28, 3, 86.7),
    (10, '副业内容产品的启动门槛正在降低', '个人创作者借助模板化工具，正在更快完成选题到发布的第一轮验证。', '从知识卡片到付费电子手册，很多副业产品并不需要一开始就做到复杂。先用轻量方式验证是否有人愿意阅读、收藏和转发，往往比投入大量时间打磨更重要。', '副业雷达', 'https://news.example.com/career/side-hustle-content-product', 'https://images.example.com/news/10.jpg', 3, '2026-05-05 09:30:00', '2026-05-05 09:42:00', 1, 132, 21, 15, 2, 72.5),
    (11, '远程实习机会为什么越来越看重作品集', '相比笼统经历，能直接展示能力的实际产出更容易建立信任。', '在远程协作场景里，用人方更难通过短时间交流判断候选人的执行能力，因此作品集的重要性持续上升。清晰展示做过什么、如何拆解问题、最后产生了什么结果，会比泛泛描述更有效。', '职场观察局', 'https://news.example.com/career/remote-intern-portfolio', 'https://images.example.com/news/11.jpg', 3, '2026-05-04 08:50:00', '2026-05-04 09:02:00', 1, 118, 17, 14, 2, 68.9),
    (12, '年轻人职业转型先看能力迁移而不是头衔', '跨行业并不一定从零开始，关键在于识别可迁移能力。', '很多人把转型理解为完全重来，但现实中更高效的方式是先盘点已有能力，再寻找能复用这些能力的新岗位。沟通、写作、整理信息和项目推进能力，在很多新职业中都仍然有价值。', '职业成长社', 'https://news.example.com/career/transferable-skills-first', 'https://images.example.com/news/12.jpg', 3, '2026-05-03 18:15:00', '2026-05-03 18:30:00', 0, 79, 11, 9, 1, 57.1),
    (13, '数字极简桌面正在重新定义专注感', '更少的入口、更清晰的层级，往往比花哨布局更能提升执行效率。', '一些年轻用户开始重新整理自己的数字桌面：减少常驻图标、统一文件命名、压缩通知来源。数字极简并不是“什么都不用”，而是让每一次打开设备都更接近目标任务。', '数字生活实验室', 'https://news.example.com/digital/minimal-desktop-focus', 'https://images.example.com/news/13.jpg', 4, '2026-05-06 10:40:00', '2026-05-06 10:55:00', 1, 140, 27, 19, 3, 76.2),
    (14, '团队协作文档正在替代一部分碎片聊天', '信息沉淀能力开始比即时回复速度更重要。', '越来越多团队要求把讨论结果最终写回文档，而不是停留在聊天窗口里。这样做虽然看起来慢一些，但能显著减少重复沟通，也更方便新人快速接手上下文。', '远程协作周刊', 'https://news.example.com/digital/docs-over-chat', 'https://images.example.com/news/14.jpg', 4, '2026-05-05 15:00:00', '2026-05-05 15:15:00', 1, 126, 23, 18, 2, 71.8),
    (15, '轻量自动化工具成为个人效率新基建', '从待办同步到内容归档，自动化开始进入普通用户日常。', '过去自动化更像极客工具，但现在越来越多模板化服务让普通用户也能快速上手。自动化真正带来的价值不是炫技，而是减少重复步骤，把时间还给更重要的判断。', '效率工具箱', 'https://news.example.com/digital/light-automation-habit', 'https://images.example.com/news/15.jpg', 4, '2026-05-04 13:20:00', '2026-05-04 13:32:00', 1, 150, 33, 21, 4, 82.1),
    (16, '远程办公之后，下班仪式感如何重建', '工作和生活边界被打散后，主动切换场景比以前更重要。', '很多远程办公者发现，最难的不是开始工作，而是停止工作。固定收尾动作、明确下班清单和下班后不再处理低优先级消息，正在成为新的数字生活规则。', '生活方式编辑部', 'https://news.example.com/digital/remote-work-end-of-day', 'https://images.example.com/news/16.jpg', 4, '2026-05-02 21:00:00', '2026-05-02 21:15:00', 1, 84, 13, 10, 1, 58.6),
    (17, '海外社交平台短内容风向再次变化', '平台鼓励机制改变后，内容创作者开始重新寻找增长平衡点。', '近期多个海外平台重新调整推荐逻辑，短内容虽然仍然强势，但账号稳定增长越来越依赖清晰定位和持续更新。对关注趋势的人来说，重要的不只是看热闹，更要理解规则变化背后的平台目标。', '趋势编辑部', 'https://news.example.com/trend/overseas-short-content-shift', 'https://images.example.com/news/17.jpg', 5, '2026-05-06 12:30:00', '2026-05-06 12:45:00', 1, 171, 35, 23, 3, 87.1),
    (18, '热点趋势新闻为什么更需要慢一点解读', '速度能带来关注，但真正的判断往往来自二次整理和对比。', '热点内容最容易在第一时间占据注意力，但也最容易制造情绪噪音。对年轻用户来说，建立延迟判断习惯、等待更多信息出现，反而能提升信息消费质量。', '趋势编辑部', 'https://news.example.com/trend/slow-reading-hot-topics', 'https://images.example.com/news/18.jpg', 5, '2026-05-05 17:45:00', '2026-05-05 18:00:00', 0, 102, 16, 12, 2, 64.4),
    (19, '年轻人开始重新关注低成本高质量生活方式', '从合租空间到二手循环，新的生活趋势更强调可持续与可复制。', '和单纯追求“精致感”不同，越来越多年轻人开始讨论“长期过得舒服”这件事。更低压力的消费结构、更稳定的作息和更实用的工具选择，正在成为新的热点话题。', '城市青年观察', 'https://news.example.com/trend/low-cost-high-quality-living', 'https://images.example.com/news/19.jpg', 5, '2026-05-04 20:40:00', '2026-05-04 20:55:00', 1, 113, 19, 14, 2, 69.7),
    (20, '校园里的 AI 社交话题正在从热闹走向实用', '从“好不好玩”到“到底怎么用”，校园讨论开始进入第二阶段。', 'AI 在校园里已经不再只是新鲜话题，很多讨论开始转向具体应用：怎么写更有效的提示、哪些场景该用、哪些场景不该过度依赖。工具热度还在，但话题已经变得更务实。', '青年趋势观察', 'https://news.example.com/trend/campus-ai-social-practical', 'https://images.example.com/news/20.jpg', 5, '2026-05-03 11:25:00', '2026-05-03 11:40:00', 1, 147, 28, 19, 3, 78.3)
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `summary` = VALUES(`summary`),
    `content` = VALUES(`content`),
    `source_name` = VALUES(`source_name`),
    `source_url` = VALUES(`source_url`),
    `cover_image` = VALUES(`cover_image`),
    `category_id` = VALUES(`category_id`),
    `publish_time` = VALUES(`publish_time`),
    `crawl_time` = VALUES(`crawl_time`),
    `status` = VALUES(`status`),
    `view_count` = VALUES(`view_count`),
    `like_count` = VALUES(`like_count`),
    `favorite_count` = VALUES(`favorite_count`),
    `comment_count` = VALUES(`comment_count`),
    `heat_score` = VALUES(`heat_score`),
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `news_tag` (`id`, `news_id`, `tag_id`)
VALUES
    (1, 1, 1), (2, 1, 3),
    (3, 2, 1), (4, 2, 2),
    (5, 3, 1), (6, 3, 9),
    (7, 4, 1), (8, 4, 9),
    (9, 5, 4), (10, 5, 5),
    (11, 6, 5), (12, 6, 3),
    (13, 7, 1), (14, 7, 5),
    (15, 8, 3), (16, 8, 5),
    (17, 9, 6), (18, 9, 1),
    (19, 10, 7), (20, 10, 9),
    (21, 11, 8), (22, 11, 6),
    (23, 12, 6), (24, 12, 7),
    (25, 13, 3), (26, 13, 8),
    (27, 14, 8), (28, 14, 3), (29, 14, 9),
    (30, 15, 3), (31, 15, 1),
    (32, 16, 8), (33, 16, 5),
    (34, 17, 10), (35, 17, 9),
    (36, 18, 10), (37, 18, 5),
    (38, 19, 10), (39, 19, 3),
    (40, 20, 1), (41, 20, 10)
ON DUPLICATE KEY UPDATE
    `created_at` = CURRENT_TIMESTAMP;
