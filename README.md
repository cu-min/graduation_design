# 个性化新闻推荐系统

[![Java](https://img.shields.io/badge/Java-17-1f6feb?style=for-the-badge)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-3fb950?style=for-the-badge)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61dafb?style=for-the-badge)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178c6?style=for-the-badge)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8-005c84?style=for-the-badge)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-Cache-d73a49?style=for-the-badge)](https://redis.io/)

一个面向内容分发场景的个性化新闻推荐系统，聚焦新闻浏览、用户互动、兴趣建模、推荐分发与后台运营管理。系统提供完整的前后端业务闭环，适用于新闻资讯、内容社区、信息聚合平台等需要“内容推荐 + 内容运营”能力的项目场景。

## 项目定位

这个系统解决的是两个核心问题：

- 用户端如何更快看到与自己兴趣更匹配的新闻内容
- 运营端如何更高效地管理新闻、分类、标签、采集源和用户状态

项目当前已经具备“内容展示 + 用户互动 + 推荐链路 + 后台管理”的基础产品能力，既可以作为内容推荐平台的原型，也可以继续向更完整的线上系统演进。

## 核心能力

### 用户端

- 首页新闻流展示
- 推荐流与最新资讯切换
- 分类筛选与关键词搜索
- 新闻详情阅读
- 点赞、收藏、分享、不感兴趣
- 评论与回复
- 个人中心
- 个人资料编辑
- 头像展示与头像地址编辑
- 兴趣标签维护
- 浏览记录、点赞记录、收藏记录、评论记录

### 推荐能力

- 基于兴趣标签的内容偏好建模
- 基于浏览、点赞、收藏、评论等行为的推荐调整
- 首页推荐流
- 新闻详情页相关推荐
- 热门内容排序与展示

### 后台管理

- 后台新闻管理
- 分类管理
- 标签管理
- 用户管理
- 采集源管理
- 内容概览与行为统计

## 业务需求

### 用户需求

- 快速浏览新闻内容，不需要复杂学习成本
- 能根据个人兴趣看到更相关的内容
- 能保存、反馈、筛选和沉淀自己的阅读偏好
- 能统一管理个人资料、账号信息和互动记录

### 运营需求

- 快速新增、编辑、删除和上下架新闻
- 灵活维护分类、标签和内容组织结构
- 维护采集源并掌握采集执行情况
- 查看用户规模、内容规模和基础行为数据

### 系统需求

- 保持用户端和后台端权限边界清晰
- 推荐链路具备基础可解释性和可扩展性
- 测试环境可独立运行，不依赖本地手工数据库状态
- 前后端可独立构建并通过基础校验

## 当前实现范围

### 已完成

- 前后端分离架构
- 用户认证与权限控制
- 用户端核心内容浏览链路
- 用户互动行为采集
- 推荐流与相关推荐
- 个人中心资料与账号设置
- 后台分类、标签、用户、新闻、采集源管理
- 后端测试环境独立化
- GitHub Actions 基础 CI

### 持续演进方向

- 注册后的兴趣冷启动引导
- 更强的协同过滤与混合推荐
- Redis 缓存进一步落地
- 更细粒度的后台权限体系
- 更完整的数据统计与推荐分析能力

## 主要页面

| 页面 | 说明 |
| --- | --- |
| 首页 | 展示推荐流、最新资讯、搜索筛选和热门内容 |
| 新闻详情页 | 展示新闻正文、互动操作、评论回复和相关推荐 |
| 个人中心 | 管理头像、昵称、邮箱、手机号、兴趣标签与个人记录 |
| 管理后台 | 管理新闻、分类、标签、用户、采集源与基础统计 |

## 技术栈

### 前端

- React 19
- TypeScript
- Vite
- React Router
- Axios

### 后端

- Java 17
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- JWT

### 数据与基础设施

- MySQL
- Redis
- H2（测试环境）
- GitHub Actions

## 项目结构

```text
graduation_design/
├─ backend/    Spring Boot 后端
├─ frontend/   React 前端
└─ README.md
```

## 后端核心模块

- `AuthController`：登录、注册与身份认证
- `NewsController`：新闻列表、详情、热门内容、相关推荐
- `RecommendController`：推荐流接口
- `CommentController`：评论与回复
- `ProfileController`：个人资料、密码与个人中心数据
- `UserBehaviorController`：用户互动行为
- `AdminNewsController`：后台新闻管理
- `AdminCategoryController`：后台分类管理
- `AdminTagController`：后台标签管理
- `AdminUserController`：后台用户管理
- `AdminDashboardController`：后台统计面板
- `AdminCrawlConfigController`：采集源管理

## RSS 采集策略

当前系统优先从 RSS item 中获取新闻标题、摘要、链接、发布时间和图片。正文内容会按 `content:encoded`、`encodedContent`、`description`、`summary`、`title` 的顺序读取，并通过 Jsoup 去除 `script`、`style`、`iframe` 等标签后保存为普通正文文本。

当 RSS 正文较短时，系统会尝试访问新闻原文页面，使用常见正文容器（如 `article`、`main`、`.article-content`、`.post-content`、`.entry-content`、`.content`、`#content`）进行轻量级正文抽取；封面图优先使用 RSS 中的 `media:content` / `enclosure`，其次尝试原文页 `og:image` 和正文首张图片。若目标网站限制访问、响应异常或页面结构复杂导致抽取失败，采集任务会回退使用 RSS 摘要，不会因此中断。

## 数据模型

当前系统围绕以下核心数据表展开：

- `user`
- `category`
- `tag`
- `news`
- `news_tag`
- `user_interest`
- `user_behavior`
- `comment`
- `crawl_config`

这些数据共同支撑了内容组织、用户画像、推荐分发、互动记录和后台运营管理。

## 工程状态

当前仓库已经具备以下基础工程能力：

- 前端可以独立完成生产构建
- 后端可以独立完成编译
- 后端测试使用独立测试配置与内存数据库
- GitHub 工作流会自动执行前端构建与后端测试

## 适用场景

- 新闻资讯平台
- 兴趣内容推荐产品
- 内容聚合与分发系统
- 具备后台运营能力的内容管理项目

## Docker Deployment

本项目支持 Docker Compose 一键启动。正式部署包含 `mysql`、`redis`、`backend`、`nginx` 四个服务，其中 Nginx 作为统一入口，负责托管前端静态资源，并将 `/api` 请求反向代理到后端服务。

首次启动时，MySQL 容器会创建 `.env` 中配置的数据库，后端启动后会通过 Spring Boot 的 `schema.sql` 和 `data.sql` 自动初始化表结构与初始数据。容器部署会读取仓库根目录的 `.env`，如需重置本地变量，可参考 `.env.example`。

常用命令：

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
docker compose down
docker compose up -d --build
```

本机已完成正式部署验证：`docker compose build` 成功，`docker compose up -d` 成功，`mysql`、`redis`、`backend` 均为 healthy，`nginx` 正常启动，访问 `http://localhost` 可打开页面，页面功能与接口访问正常。

如果需要清空 Docker 数据库并重新初始化，可以执行 `docker compose down -v` 后再启动；该命令会删除 MySQL 数据卷，请确认数据不需要保留后再使用。

这套部署方式不会替代原有本地开发方式，前端 `npm run dev` 和后端本地启动仍然可继续使用。

## License

当前仓库未单独声明开源许可证；如需对外开放或商用，请先补充明确的许可证文件与使用规则。
