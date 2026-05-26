# CampusHub

CampusHub 是一个轻量级校园论坛后端项目，使用 Spring Boot + JDBC 实现。项目目标不是堆功能，而是把一个个人项目常见的后端主链路做完整：认证、用户、分类、帖子、评论、点赞、通知、管理、缓存、事件和测试。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 认证模块 | 用户注册、登录、退出，支持内存 token 和 Redis token 两种存储方式 |
| 用户模块 | 当前用户资料、公开主页、资料修改 |
| 分类模块 | 查询启用分类 |
| 帖子模块 | 发帖、列表、详情、编辑、删除、用户帖子、热门帖子 |
| 评论模块 | 发表评论、评论列表、删除评论、我的评论 |
| 点赞模块 | 点赞、取消点赞、查询点赞状态 |
| 通知模块 | 评论通知、点赞通知、未读数、标记已读 |
| 管理模块 | 隐藏/恢复帖子，禁用/启用用户 |
| 缓存模块 | Redis 热门帖子缓存，可选启用 |
| 事件模块 | 评论和点赞领域事件，默认同步处理，可选 RocketMQ 异步处理 |
| 浏览量模块 | 内存计数 + 定时批量刷新数据库 |
| 测试模块 | H2 测试库 + 核心接口集成测试 + 边界场景测试 |

## 技术栈

- Java 17
- Spring Boot 4
- Spring Web
- Spring Validation
- Spring JDBC
- MySQL
- H2，测试环境使用
- Redis，可选
- RocketMQ，可选
- JUnit 6 / Spring Boot Test

## 项目结构

```text
src/main/java/com/tyj/campushub
├── admin       管理接口
├── auth        注册、登录、token、当前用户识别
├── category    帖子分类
├── comment     评论
├── common      通用响应、分页响应、错误码
├── event       领域事件、RocketMQ 发布和消费
├── exception   业务异常、全局异常处理
├── like        点赞
├── notice      站内通知
├── post        帖子、热门缓存、浏览量批量更新
└── user        用户资料
```

## 接口文档

完整 REST API 文档见：

```text
docs/api/REST_API.md
```

文档包含统一响应结构、错误码、请求参数、响应字段和完整接口清单。

## 环境要求

最小启动只需要：

- JDK 17+
- Maven，项目已带 Maven Wrapper
- MySQL 8.x

可选增强能力：

- Redis，用于 token 和热门帖子缓存
- RocketMQ，用于评论/点赞事件异步通知

## 数据库初始化

1. 创建数据库和表：

```sql
source src/main/resources/db/schema.sql;
```

2. 初始化分类数据：

```sql
source src/main/resources/db/data.sql;
```

如果在命令行中执行，可以先进入 MySQL：

```bash
mysql -u root -p
```

然后执行：

```sql
source D:/GitCode/CampusHub/src/main/resources/db/schema.sql;
source D:/GitCode/CampusHub/src/main/resources/db/data.sql;
```

## 本地启动

默认配置读取 `src/main/resources/application.yaml`。

常用环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 服务端口 |
| `CAMPUSHUB_DB_URL` | `jdbc:mysql://localhost:3306/campushub?...` | MySQL 连接地址 |
| `CAMPUSHUB_DB_USERNAME` | `root` | MySQL 用户名 |
| `CAMPUSHUB_DB_PASSWORD` | 空 | MySQL 密码 |
| `CAMPUSHUB_VIEW_COUNT_FLUSH_INTERVAL_SECONDS` | `10` | 浏览量批量刷库间隔 |

启动：

```bash
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

启动后默认访问：

```text
http://localhost:8080
```

## 启用 Redis

默认 profile 不依赖 Redis：

- token 使用 `InMemoryTokenStore`
- 热门帖子缓存使用 `NoOpHotPostCache`

启用 Redis profile 后：

- token 使用 `RedisTokenStore`
- 热门帖子缓存使用 `RedisHotPostCache`

Redis 配置：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `CAMPUSHUB_REDIS_HOST` | `localhost` | Redis 主机 |
| `CAMPUSHUB_REDIS_PORT` | `6379` | Redis 端口 |
| `CAMPUSHUB_REDIS_PASSWORD` | 空 | Redis 密码 |
| `CAMPUSHUB_REDIS_DATABASE` | `0` | Redis database |
| `CAMPUSHUB_HOT_POST_TTL_SECONDS` | `300` | 热门帖子缓存时间 |
| `CAMPUSHUB_HOT_POST_TTL_JITTER_SECONDS` | `60` | 热门帖子缓存随机过期抖动时间 |

启动：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis"
```

## 启用 RocketMQ

默认 profile 不依赖 RocketMQ，评论和点赞事件会同步处理，便于本地开发。

启用 `rocketmq` profile 后：

- `RocketMqDomainEventPublisher` 负责发送事件
- `RocketMqEventConsumer` 负责消费事件并生成通知

RocketMQ 配置在：

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

启动：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=rocketmq
```

同时启用 Redis 和 RocketMQ：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rocketmq
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis,rocketmq"
```

## 运行测试

测试环境使用 H2 内存数据库，不需要本地 MySQL、Redis、RocketMQ。

```bash
./mvnw test
```

Windows PowerShell：

```powershell
.\mvnw.cmd test
```

当前测试覆盖：

- Spring Boot 上下文启动
- 注册、登录、分类、发帖、评论、点赞、通知主流程
- 未登录发帖
- 重复注册
- 非法分页参数
- 普通用户访问管理接口
- 重复点赞和重复取消点赞

## 典型请求示例

注册：

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "小艾"
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

发帖：

```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "categoryId": 1,
  "title": "高数复习资料怎么整理？",
  "content": "想问问大家期末复习有什么方法。"
}
```

## 已完成状态

当前项目已经具备一个轻量论坛后端的完整闭环：

- 用户可以注册、登录、发帖、评论、点赞
- 帖子作者可以收到评论和点赞通知
- 管理员可以处理帖子和用户
- 热门帖子支持缓存扩展
- 评论/点赞支持事件化扩展
- 浏览量采用批量刷库降低写压力
- 有接口文档和集成测试兜底

## Redis 缓存优化说明

热门帖子接口使用 Cache Aside 模式：

1. 先查 Redis 热门帖子缓存。
2. 命中则直接返回。
3. 未命中时尝试获取短期互斥锁。
4. 获取锁的请求回源 MySQL 查询热门帖子，并写入 Redis。
5. 未获取锁的请求短暂等待后再查一次缓存，仍未命中则直接回源返回。

对应解决的问题：

| 问题 | 当前做法 | 说明 |
| --- | --- | --- |
| 缓存雪崩 | TTL 增加随机抖动 | 避免大量热门缓存同一时间过期 |
| 缓存击穿 | Redis 短期互斥锁 | 避免同一个热点 key 失效时大量请求同时打到 MySQL |
| 缓存穿透 | 空列表也写入缓存，并在业务层校验分类是否存在 | 避免无数据的合法查询反复回源；非法分类直接返回 404 |

这是一版适合个人项目的轻量实现，没有引入复杂中间件或 Lua 脚本。面试时可以说明：生产级释放锁更推荐使用 Lua 保证原子性，或者使用 Redisson。

## 后续优化方向

- 细化管理员权限，例如禁止管理员封禁其他管理员
- 补充更多边界测试，例如删除不存在资源、禁用用户登录、隐藏帖子不可访问
- 引入 OpenAPI/Swagger 自动生成接口文档
- 使用 Lua 或 Redisson 进一步增强 Redis 锁释放的原子性
- 增强 RocketMQ 消息可靠性，例如重试、幂等、死信队列
- 增加 Docker Compose，统一启动 MySQL、Redis、RocketMQ
