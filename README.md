# TrendPulse

TrendPulse 是一个面向内容分发场景的个性化新闻推荐平台，覆盖新闻聚合、RSS 采集、用户互动、兴趣建模、推荐分发和后台运营管理。系统以“内容浏览 + 行为反馈 + 推荐优化 + 管理配置”为核心闭环，适用于资讯聚合、内容社区、垂直媒体和内部信息分发等业务场景。

![Java](https://img.shields.io/badge/Java-17-1f6feb?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-3fb950?style=flat-square)
![React](https://img.shields.io/badge/React-19-61dafb?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-6-3178c6?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8-005c84?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7-d73a49?style=flat-square)

## 项目概览

TrendPulse 将 RSS 内容采集、新闻内容管理、用户行为记录和推荐结果生成整合在同一套前后端系统中。用户可以浏览新闻、搜索内容、表达兴趣反馈；管理员可以维护内容、分类、标签、采集源和用户状态；推荐服务会结合用户兴趣标签、历史行为、内容热度和发布时间生成个性化新闻流。

系统内置五类内容方向：

- 前沿科技
- 成长学习
- 职业机会
- 数字生活
- 热点趋势

## 功能特性

### 用户端

- 新闻列表、热门新闻和个性化推荐流
- 分类筛选与关键词搜索
- 新闻详情、来源跳转、相关推荐
- 点赞、收藏、分享、不感兴趣反馈
- 评论与一级回复
- 个人中心：资料维护、兴趣标签、浏览历史、点赞、收藏、评论记录
- 空封面与远程图片加载失败的默认封面兜底

### 管理端

- 新闻管理：新增、编辑、删除、上下架、分类/状态/关键词筛选
- 用户管理：用户列表、角色状态展示、启用/禁用控制
- 分类与标签管理：维护内容组织结构和推荐标签体系
- RSS 采集管理：新增、编辑、启停采集源，查看上次采集状态、数量和错误信息
- 管理首页统计：新闻、用户、评论、行为、采集源和分类分布概览

### 推荐能力

- 登录用户基于兴趣标签和行为数据生成个性化推荐
- 未登录用户返回热门内容推荐
- 支持 `VIEW`、`LIKE`、`FAVORITE`、`SHARE`、`DISLIKE` 等行为信号
- `DISLIKE` 内容会从该用户推荐流中过滤
- 推荐结果包含可解释原因，便于前端展示和调试

## 技术栈

### 后端

| 技术 | 用途 |
| --- | --- |
| Java 17 | 后端运行环境 |
| Spring Boot 3.3 | Web 应用主框架 |
| Spring Security | 认证与权限控制 |
| JWT | 无状态登录鉴权 |
| MyBatis-Plus | ORM 与数据访问 |
| MySQL 8 | 主业务数据库 |
| Redis 7 | 热门内容与推荐结果缓存 |
| Spring Scheduling | RSS 自动采集调度 |
| Maven | 依赖管理与构建 |

### 前端

| 技术 | 用途 |
| --- | --- |
| React 19 | 前端 UI 框架 |
| TypeScript | 类型约束 |
| Vite | 开发与生产构建 |
| React Router | 前端路由 |
| Axios | HTTP 请求封装 |

## 系统架构

```text
User / Admin
    |
    v
React + TypeScript + Vite
    |
    | REST API / JWT
    v
Spring Boot Backend
    |
    +-- Auth / News / Recommend / Comment / Profile APIs
    +-- Admin APIs
    +-- RSS Crawl Scheduler
    |
    +-- MySQL
    +-- Redis
```

典型请求链路：

```text
前端交互
  -> Axios 携带 JWT
  -> Spring Security 校验身份与权限
  -> Controller 接收请求
  -> Service 执行业务逻辑
  -> Mapper 读写 MySQL
  -> Redis 缓存热点数据或推荐结果
  -> 前端渲染响应
```

## 数据模型

系统围绕 9 张核心业务表展开：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户账号、密码哈希、角色和状态 |
| `category` | 新闻分类 |
| `tag` | 内容标签和推荐标签 |
| `news` | 新闻正文、来源、封面、统计计数和上下架状态 |
| `news_tag` | 新闻与标签的多对多关系 |
| `user_interest` | 用户主动选择的兴趣标签 |
| `user_behavior` | 浏览、点赞、收藏、分享、不感兴趣等行为日志 |
| `comment` | 评论和一级回复 |
| `crawl_config` | RSS 采集源配置和采集状态 |

## RSS 采集

RSS 采集服务会读取采集源配置，按间隔自动抓取启用的 RSS 源。采集时优先解析 RSS item 中的标题、摘要、链接、发布时间、封面和正文内容；当 RSS 正文较短时，会尝试访问原文页面并从常见正文容器中提取正文。若原文页访问失败或结构不适合抽取，系统会回退使用 RSS 摘要，避免单条内容影响整体采集任务。

管理端支持手动触发采集，并展示最近一次采集时间、状态、采集数量和错误信息。

## 推荐策略

推荐分数由兴趣匹配、行为标签、内容热度和发布时间共同决定：

```text
recommendScore = interestScore + behaviorScore + heatScore + freshnessScore
```

行为信号权重：

| 行为 | 语义 |
| --- | --- |
| `VIEW` | 浏览，表示轻度兴趣 |
| `LIKE` | 点赞，表示明确正反馈 |
| `FAVORITE` | 收藏，表示强兴趣和复看意愿 |
| `SHARE` | 分享，表示高价值内容 |
| `DISLIKE` | 不感兴趣，用于推荐过滤 |

冷启动策略：

- 未登录用户：按热度和发布时间展示热门内容
- 新用户：优先匹配兴趣标签，同时混入热门内容
- 活跃用户：结合兴趣标签、行为标签、热度和新鲜度排序

## API 概览

```text
# 认证
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/logout

# 新闻与推荐
GET    /api/news
GET    /api/news/{id}
GET    /api/news/hot
GET    /api/news/{id}/related
GET    /api/recommend/news

# 用户行为
POST   /api/news/{id}/like
DELETE /api/news/{id}/like
POST   /api/news/{id}/favorite
DELETE /api/news/{id}/favorite
POST   /api/news/{id}/share
POST   /api/news/{id}/dislike

# 评论
GET    /api/news/{id}/comments
POST   /api/news/{id}/comments
POST   /api/comments/{id}/reply
DELETE /api/comments/{id}

# 个人中心
GET    /api/profile/summary
GET    /api/profile/history
GET    /api/profile/favorites
GET    /api/profile/likes
GET    /api/profile/comments
GET    /api/profile/interests
PUT    /api/profile/interests

# 管理端
GET    /api/admin/dashboard/summary
GET    /api/admin/news
POST   /api/admin/news
PUT    /api/admin/news/{id}
DELETE /api/admin/news/{id}
GET    /api/admin/users
GET    /api/admin/categories
GET    /api/admin/tags
GET    /api/admin/crawl-configs
POST   /api/admin/crawl-configs
PUT    /api/admin/crawl-configs/{id}
POST   /api/admin/crawl-configs/{id}/crawl
```

## 快速启动

### Docker Compose

仓库提供 Docker Compose 部署配置，包含 MySQL、Redis、后端服务和 Nginx 前端入口。首次启动时，后端会自动执行 `schema.sql` 和 `data.sql` 初始化表结构与演示数据。

```bash
docker compose up -d --build
```

重置本地数据并重新初始化：

```bash
docker compose down -v
docker compose up -d --build
```

查看服务状态：

```bash
docker compose ps
docker compose logs -f backend
```

默认访问地址：

- 前端入口：`http://localhost`
- 后端健康检查：`http://localhost/api/health`

### 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。如需指定后端地址，可配置：

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

## 构建与验证

后端测试：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
npm run build
```

## 目录结构

```text
trendpulse/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   └── resources/
│       │       ├── schema.sql
│       │       ├── data.sql
│       │       └── application.yml
│       └── test/
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── public/
│   └── src/
├── deploy/
│   └── nginx/
├── docker-compose.yml
└── README.md
```

## License

MIT
