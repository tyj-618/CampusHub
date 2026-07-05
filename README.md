# CampusHub

CampusHub 是一个轻量级校园论坛纯后端项目，基于 Spring Boot 开发，围绕校园交流场景实现用户认证、用户资料、帖子、评论、点赞、站内通知、后台管理等核心功能。

项目重点不是堆叠页面或前端效果，而是完整体现一个后端项目从业务建模、接口设计、分层架构、数据库访问、缓存/排行榜、事件解耦、并发优化到集成测试的工程化过程。

## 技术栈

- Java 17
- Spring Boot 4
- Spring Web
- Spring Validation
- MyBatis-Plus
- MySQL
- Redis，可选启用
- RocketMQ，可选启用
- H2，测试环境使用
- JUnit / Spring Boot Test

## 项目架构

项目采用清晰的后端分层结构：

```text
Controller -> Service -> Mapper(MyBatis-Plus) -> Database
```

- Controller：负责 REST API 接入，接收请求参数、请求体和请求头。
- Service：负责业务逻辑、登录校验、权限校验、资源状态判断、事件发布和排行榜协调。
- Mapper：基于 MyBatis-Plus 负责单表 CRUD、复杂查询 SQL 和数据库结果映射。
- Common / Exception：负责统一响应结构、分页响应、错误码、业务异常和全局异常处理。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| auth | 用户注册、登录、退出、Token 管理、当前用户识别 |
| user | 当前用户资料、公开主页、资料修改 |
| category | 帖子分类查询 |
| post | 发帖、帖子列表、帖子详情、编辑、删除、用户帖子、热门帖子 |
| comment | 发表评论、评论列表、删除评论、我的评论 |
| like | 点赞、取消点赞、查询点赞状态 |
| notice | 评论通知、点赞通知、未读数、标记已读 |
| admin | 隐藏/恢复帖子、禁用/启用用户 |
| event | 领域事件抽象、同步事件处理、RocketMQ 扩展 |
| common | 统一响应、分页响应、错误码 |
| exception | 业务异常、全局异常处理 |

## 项目亮点

- 使用 `Controller-Service-Mapper` 分层架构，明确接口层、业务层和数据访问层职责。
- 使用 MyBatis-Plus 重构数据访问层，基于实体映射处理核心表 CRUD，并通过注解 SQL 保留复杂列表、详情和统计查询的可读性。
- 使用 BCrypt 对用户密码进行哈希存储，避免明文密码落库。
- 抽象 `TokenStore`，默认使用内存版 Token 存储，启用 Redis 后可切换为 Redis Token 存储，适合多实例扩展。
- 使用 Redis ZSet 实现热门帖子排行榜：
  - `member = postId`
  - `score = hot_score`
  - MySQL `post_stat.hot_score` 作为持久化热度分来源
  - Redis 排行榜为空或数据不一致时，可从 MySQL 回源重建
- 使用 RocketMQ 事件模型解耦评论、点赞和站内通知，默认环境下也提供同步事件实现，便于本地开发和测试。
- 使用 `ConcurrentHashMap + LongAdder + ScheduledExecutorService` 对帖子浏览量进行内存累计和定时批量刷库，降低高频浏览场景下的数据库写压力。
- 使用统一响应结构、错误码枚举、业务异常和全局异常处理，保证接口返回格式一致。
- 使用 H2 编写核心接口集成测试，覆盖认证、发帖、评论、点赞、通知和权限边界场景。

## 项目结构

```text
src/main/java/com/tyj/campushub
├── admin       后台管理
├── auth        注册、登录、Token、当前用户识别
├── category    帖子分类
├── comment     评论
├── common      通用响应、分页响应、错误码
├── event       领域事件、RocketMQ 发布和消费
├── exception   业务异常、全局异常处理
├── like        点赞
├── notice      站内通知
├── post        帖子、热门排行榜、浏览量批量刷新
└── user        用户资料
```

重要资源文件：

```text
src/main/resources/application.yaml
src/main/resources/application-rocketmq.yaml
src/main/resources/db/schema.sql
src/main/resources/db/data.sql
src/test/resources/application-test.yaml
src/test/resources/schema.sql
src/test/resources/data.sql
```

## 接口文档

核心接口清单见 [docs/API.md](docs/API.md)，覆盖认证、用户资料、帖子、评论、点赞、通知和后台管理接口。

## 数据库初始化

创建数据库和表：

```sql
source src/main/resources/db/schema.sql;
```

初始化分类数据：

```sql
source src/main/resources/db/data.sql;
```

## 本地启动

默认启动只依赖 MySQL。

可以参考环境变量示例配置本机 MySQL、Redis 和 RocketMQ：

```bash
cp .env.example .env
```

`.env` 用于记录本地配置，不提交到仓库；启动前可将其中变量配置到系统环境变量、IDE 运行配置或命令行会话中。

常用环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `CAMPUSHUB_DB_URL` | `jdbc:mysql://localhost:3306/campushub?...` | MySQL 连接地址 |
| `CAMPUSHUB_DB_USERNAME` | `root` | MySQL 用户名 |
| `CAMPUSHUB_DB_PASSWORD` | 空 | MySQL 密码 |
| `CAMPUSHUB_VIEW_COUNT_FLUSH_INTERVAL_SECONDS` | `10` | 浏览量批量刷库间隔 |
| `CAMPUSHUB_HOT_POST_CACHE_TTL_SECONDS` | `300` | 热门帖子缓存 TTL |
| `CAMPUSHUB_HOT_POST_CACHE_JITTER_SECONDS` | `60` | 热门帖子缓存随机抖动 |
| `CAMPUSHUB_HOT_POST_EMPTY_CACHE_TTL_SECONDS` | `30` | 空结果缓存 TTL |
| `CAMPUSHUB_HOT_POST_REBUILD_LOCK_TTL_SECONDS` | `10` | 热榜回源重建锁 TTL |

启动项目：

```bash
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

## Redis 模式

默认 profile 不依赖 Redis：

- `InMemoryTokenStore` 存储登录 Token。
- `NoOpHotPostRankStore` 直接从 MySQL 查询热门帖子。

启用 `redis` profile 后：

- `RedisTokenStore` 使用 Redis 存储 Token，并设置过期时间。
- `RedisHotPostRankStore` 使用 Redis ZSet 维护热门帖子排行榜。

Redis 环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `CAMPUSHUB_REDIS_HOST` | `localhost` | Redis 主机 |
| `CAMPUSHUB_REDIS_PORT` | `6379` | Redis 端口 |
| `CAMPUSHUB_REDIS_PASSWORD` | 空 | Redis 密码 |
| `CAMPUSHUB_REDIS_DATABASE` | `0` | Redis 数据库编号 |

启用 Redis：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis"
```

## RocketMQ 模式

默认 profile 下，评论和点赞事件同步处理，不依赖 RocketMQ。

启用 `rocketmq` profile 后：

- `RocketMqDomainEventPublisher` 负责发布事件。
- `RocketMqEventConsumer` 负责消费事件并生成通知。

RocketMQ 配置文件：

```text
src/main/resources/application-rocketmq.yaml
```

常用环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `CAMPUSHUB_ROCKETMQ_NAME_SERVER` | `localhost:9876` | RocketMQ NameServer |
| `CAMPUSHUB_ROCKETMQ_PRODUCER_GROUP` | `campushub-producer-group` | 生产者组 |
| `CAMPUSHUB_COMMENT_TOPIC` | `campushub-comment-event` | 评论事件 Topic |
| `CAMPUSHUB_LIKE_TOPIC` | `campushub-like-event` | 点赞事件 Topic |

启用 RocketMQ：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=rocketmq
```

同时启用 Redis 和 RocketMQ：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rocketmq
```

## 运行测试

测试环境使用 H2 内存数据库，不依赖本地 MySQL、Redis 或 RocketMQ。

```bash
./mvnw test
```

Windows PowerShell：

```powershell
.\mvnw.cmd test
```

当前集成测试覆盖：

- Spring Boot 上下文启动
- 用户注册与登录
- 分类查询
- 发帖
- 评论创建与查询
- 点赞与取消点赞
- 通知查询与未读数
- 未登录、重复注册、非法分页、普通用户访问管理接口等边界场景

## 常见请求示例

注册：

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "Alice"
}
```

登录：

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456"
}
```

发布帖子：

```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "categoryId": 1,
  "title": "期末复习资料怎么整理？",
  "content": "想问问大家期末复习有什么方法。"
}
```

查询热门帖子：

```http
GET /api/posts/hot?limit=10
```

## 简历描述

CampusHub 是一个基于 Spring Boot 的校园论坛纯后端项目，实现了用户认证、帖子发布、评论互动、点赞、站内通知、后台管理等核心业务。项目采用 `Controller-Service-Mapper` 分层架构，使用统一响应、全局异常处理和参数校验保证接口规范性；使用 Redis ZSet 实现热门帖子排行榜，结合 MySQL `hot_score` 完成持久化兜底和回源重建；通过 RocketMQ 事件模型解耦互动行为与通知生成；使用 JUC 对浏览量进行内存累计和定时批量刷库；并基于 H2 编写集成测试覆盖核心流程和边界场景。
