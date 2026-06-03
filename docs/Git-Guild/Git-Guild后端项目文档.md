# Git Guild 后端项目文档

## 一、可写入简历的项目简介

**Git Guild：游戏化开源任务协作与代码托管平台**

Git Guild 是一个面向开源新手和项目维护者的游戏化代码协作平台，融合 GitHub/Gitee 风格的仓库、Issue、PR、代码审核流程，以及任务悬赏、推荐匹配、XP 成长等机制，帮助初学者通过真实工程任务完成贡献闭环。项目采用 Vue 3 + Vite 前端、Spring Boot 3 + Java 17 后端、MySQL 8 数据库、Redis 缓存、Docker Compose 部署，并集成 Gitea 作为平台内代码托管底座。

我主要负责项目后端核心功能开发，包括用户注册登录、JWT 认证、角色权限控制、任务发布、任务列表筛选、任务详情、任务接取等核心接口；基于 Spring Security 实现无状态鉴权，使用 BCrypt 进行密码哈希存储，并通过 tokenVersion 机制支持改密和登出后的令牌失效；基于 Spring Data JPA 设计用户、仓库、Issue、任务、分类、标签、接取记录等领域模型，并结合 MySQL 建表脚本维护数据库结构一致性。项目亮点在于采用模块化单体架构清晰划分用户认证、代码托管适配、任务悬赏、提交审核、推荐匹配、通知和成长激励模块，通过 REST API、统一响应体、业务异常码、后端角色校验和任务状态机保证业务闭环的可维护性与可扩展性。

可拆成简历 bullet：

- 负责 Git Guild 后端核心模块开发，基于 Spring Boot 3、Spring Security、JWT、Spring Data JPA、MySQL 实现用户认证、角色权限、任务发布、任务筛选、任务详情与任务接取接口。
- 设计并实现 access token / refresh token 鉴权机制，使用 BCrypt 存储密码哈希，通过 tokenVersion 支持用户登出、修改密码后的旧令牌失效，提升认证安全性。
- 按 P3/P4 设计文档落地用户、仓库、Issue、任务、分类、标签、接取记录等领域模型，维护 MySQL 初始化脚本并与 JPA 实体保持一致。
- 采用统一响应体、业务异常码和全局异常处理，规范接口成功/失败返回结构，便于前端联调和后续模块复用。
- 围绕任务协作核心流程设计任务状态流转，支持维护者发布任务、用户浏览筛选任务、查看任务详情、初学者接取任务等关键业务闭环。
- 编写认证模块与任务模块单元测试，覆盖注册登录、JWT 解析、任务发布权限、Issue 可用性校验、任务接取冲突等核心分支。

---

## 二、项目背景与目标

Git Guild 的核心目标不是简单实现一个任务列表系统，而是围绕真实代码贡献流程构建一个适合新手成长的平台。传统开源项目对新手而言有几个门槛：不知道从哪里开始、看不懂项目结构、不清楚任务难度、提交后得不到结构化反馈。Git Guild 通过“任务 / 悬赏任务”的方式，将真实仓库中的 Issue 转化为可被浏览、筛选、推荐、接取、提交和审核的任务。

系统面向三类核心角色：

| 角色 | 主要目标 | 后端关注点 |
| --- | --- | --- |
| 初学者 BEGINNER | 浏览任务、接取任务、提交成果、获得反馈和成长记录 | 身份认证、任务筛选、接取规则、提交记录、成长数据 |
| 维护者 MAINTAINER | 导入仓库、发布任务、审核成果 | 发布权限、仓库/Issue 关联、任务状态流转、审核接口 |
| 管理员 ADMIN | 审核任务发布、治理异常任务和平台数据 | 管理员权限、任务上架/下架、审核记录、平台治理 |

后端目标包括：

1. 提供稳定的 REST API，支撑前端任务大厅、工作台、提交柜台、管理员审核等页面。
2. 建立平台自有账号体系，不依赖 GitHub OAuth，便于后续对接 Gitea/GitHub 但保持平台身份独立。
3. 将外部代码托管能力封装到适配模块，避免业务模块直接依赖 Gitea/GitHub API。
4. 以任务协作闭环为主线，逐步实现任务发布、审核、接取、提交、反馈、成长激励等流程。
5. 在课程项目规模内采用模块化单体，降低部署和协作复杂度，同时保留后续演进空间。

---

## 三、整体技术栈

| 层级 | 技术 | 作用 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3 | 快速构建 REST API、配置管理、依赖注入、自动装配 |
| 语言 | Java 17 | 后端主要开发语言 |
| 安全 | Spring Security | 认证、授权、过滤器链、安全异常处理 |
| 令牌 | JWT / JJWT | 无状态 access token / refresh token 签发与解析 |
| 密码安全 | BCryptPasswordEncoder | 密码哈希存储，避免明文密码进入数据库 |
| ORM | Spring Data JPA / Hibernate | 实体映射、Repository 抽象、事务内持久化 |
| 数据库 | MySQL 8 | 存储用户、任务、审核、推荐、成长、通知等核心业务数据 |
| 缓存规划 | Redis | 后续用于推荐缓存、热点任务、通知计数、排行榜等 |
| API 文档 | springdoc-openapi | 生成 OpenAPI / Swagger 文档，当前存在版本兼容风险，接口测试主要使用 Postman |
| 部署环境 | Docker Compose | 本地启动 MySQL、Redis、Gitea 等基础设施 |
| 测试 | JUnit 5、Mockito、Spring Boot Test、H2 | 单元测试、上下文测试、测试环境内存数据库 |

---

## 四、后端模块划分

后端采用模块化单体结构，核心包划分如下：

```text
com.gitguild.backend
├── common              通用响应、业务异常、全局异常处理
├── security            JWT、安全过滤器、当前用户上下文、Spring Security 配置
├── user                用户、角色、注册登录、改密、登出
├── codehost            仓库、Issue、PR、Gitea/GitHub 适配边界
├── quest               任务、分类、标签、筛选、接取、状态流转
├── review              成果提交、维护者审核、管理员审核、逐项反馈
├── recommendation      任务推荐、贡献者推荐、推荐理由
├── guide               新手引导、项目结构、运行说明、贡献流程
├── notification        站内通知、邮件通知、通知偏好
└── growth              XP、等级、贡献记录、徽章、排行榜
```

模块化单体的优势在于：

- 部署简单，适合课程项目和小团队协作。
- 模块边界清晰，避免 controller/service 混杂。
- 可以在一个事务内完成任务状态和接取记录的一致性更新。
- 后续如果某些模块复杂度上升，例如推荐或通知，可以再拆成独立服务。

---

## 五、已实现的后端功能

### 5.1 统一响应与异常处理

所有 API 返回统一结构：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "timestamp": "2026-05-24T20:00:00+08:00",
  "traceId": "req-xxxx"
}
```

失败响应包含：

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "email 格式不正确",
  "timestamp": "...",
  "traceId": "req-xxxx"
}
```

对应实现：

- `ApiResponse<T>`：统一响应体。
- `BusinessException`：携带业务错误码、HTTP 状态码、错误详情。
- `GlobalExceptionHandler`：捕获参数校验异常、业务异常和兜底异常。

底层原理：

Spring MVC 在 controller 方法执行前会通过 `@Valid` 触发 Bean Validation，如果参数不合法，会抛出 `MethodArgumentNotValidException`。`@RestControllerAdvice` 可以统一拦截这些异常，并将异常转换为 JSON 响应。这样 controller 不需要在每个接口里重复写 try-catch，前端也能稳定解析错误码。

---

### 5.2 用户认证与角色权限

已实现接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | 用户注册 |
| POST | `/api/v1/auth/login` | 用户登录，返回 accessToken 和 refreshToken |
| POST | `/api/v1/auth/refresh` | 使用 refreshToken 换取新的 accessToken |
| POST | `/api/v1/auth/logout` | 登出，使旧 refreshToken 失效 |
| GET | `/api/v1/users/me` | 获取当前登录用户 |
| PATCH | `/api/v1/users/me/password` | 修改密码 |

核心角色：

```text
BEGINNER     初学者
MAINTAINER   项目维护者
ADMIN        管理员
```

认证流程：

1. 用户注册时提交用户名、邮箱、密码和角色。
2. 后端校验邮箱和用户名唯一性。
3. 密码通过 BCrypt 哈希后写入 `password_hash`。
4. 用户登录时使用邮箱查询用户，通过 BCrypt 校验密码。
5. 登录成功后签发 accessToken 和 refreshToken。
6. 业务接口通过 `Authorization: Bearer <token>` 传递身份。
7. JWT Filter 解析 token，将当前用户写入 Spring Security 上下文。
8. Controller / Service 从认证上下文中获取当前用户 ID，避免前端传入可信 userId。

JWT 中包含：

```text
sub             用户 ID
tokenType       ACCESS / REFRESH
roles           ROLE_BEGINNER / ROLE_MAINTAINER / ROLE_ADMIN
tokenVersion    当前用户令牌版本
exp             过期时间
```

底层原理：

JWT 是一种自包含令牌。服务端用密钥对 header 和 payload 签名，客户端后续请求携带 token，服务端只需校验签名和过期时间即可恢复用户身份。优点是无状态、扩展简单；缺点是签发后在过期前天然难以撤销。项目通过 `tokenVersion` 缓解这一问题：用户改密或登出时递增数据库中的 tokenVersion，refreshToken 中旧版本与数据库不一致时会被拒绝。

BCrypt 原理：

BCrypt 是专门用于密码存储的慢哈希算法。它会自动生成 salt，并通过计算成本参数增加暴力破解难度。数据库中保存的是类似 `$2a$10$...` 的哈希字符串，而不是明文密码。登录时不是解密密码，而是用同样算法校验用户输入是否匹配哈希。

Spring Security 原理：

Spring Security 的核心是过滤器链。请求进入应用后，会先经过安全过滤器。自定义 `JwtAuthenticationFilter` 从请求头读取 Bearer token，校验成功后创建 `Authentication` 对象并写入 `SecurityContextHolder`。后续 controller 或 service 就可以通过认证上下文获取当前用户。

---

### 5.3 任务与悬赏核心接口

已实现接口：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/quests` | MAINTAINER / ADMIN | 发布任务，初始状态为 `PENDING_ADMIN_REVIEW` |
| GET | `/api/v1/quests` | 公开 | 浏览任务列表，支持筛选和分页 |
| GET | `/api/v1/quests/{questId}` | 已发布公开；未发布仅发布者/管理员 | 查看任务详情 |
| POST | `/api/v1/quests/{questId}/assignments` | 登录用户，非 ADMIN | 接取任务 |

任务状态：

```text
DRAFT
PENDING_ADMIN_REVIEW
PUBLISHED
IN_PROGRESS
IN_REVIEW
COMPLETED
CLOSED
```

发布任务流程：

1. 维护者或管理员登录。
2. 传入仓库 ID、Issue ID、标题、描述、完成标准、难度、技术栈、分类、标签、奖励 XP 等。
3. 后端校验发布者角色。
4. 后端校验仓库存在。
5. 后端校验 Issue 属于该仓库，且 Issue 状态为 `OPEN`。
6. 后端校验该 Issue 未被其他未完成任务占用。
7. 创建任务，状态为 `PENDING_ADMIN_REVIEW`。
8. 后续由管理员审核通过后进入 `PUBLISHED`。

任务列表筛选支持：

```text
keyword
categoryId
tagIds
difficulty
techStack
status
sortBy
sortOrder
page
size
```

接取任务流程：

1. 用户登录并请求接取任务。
2. 后端从 JWT 获取用户 ID，不信任前端传入 userId。
3. 校验用户不是 ADMIN。
4. 校验任务状态必须为 `PUBLISHED`。
5. 校验当前用户未重复接取。
6. 校验任务未被其他用户接取。
7. 创建 `quest_assignments` 记录。
8. 将任务状态更新为 `IN_PROGRESS`。

底层原理：

任务接取属于典型的状态流转场景。它不是简单插入一条记录，而是需要同时维护任务状态和接取记录。后端通过事务保证这两个动作要么同时成功，要么同时失败。生产级场景下还应增加数据库唯一约束、乐观锁或悲观锁，避免两个用户并发接取同一个任务。

JPA 查询原理：

任务列表使用 `JpaSpecificationExecutor` 动态构建查询条件。Specification 本质上是 JPA Criteria API 的封装，可以按请求参数动态拼接 `Predicate`。例如 keyword 会拼接 title/description 的 LIKE 条件，difficulty 会拼接枚举等值条件，tagIds 会 join 标签关联表并做分组过滤。

---

### 5.4 代码托管支撑模型

为了支撑任务和 Issue 关联，后端已经建立了代码托管相关基础实体：

- `CodeRepository` 映射 `repositories` 表。
- `CodeIssue` 映射 `issues` 表。

当前它们主要用于任务模块：任务必须绑定仓库和 Issue。后续代码托管模块会继续扩展：

- GitHub 仓库导入。
- Gitea 仓库接入。
- Issue 同步。
- PR 创建和状态查询。
- Webhook 接收和内部事件转换。

底层原理：

项目没有自研 Git 存储，而是复用 Gitea 作为代码托管底座。Git Guild 后端只保存业务需要的元数据，例如 repositoryId、externalIssueId、syncStatus、externalUrl 等。这样能降低实现成本，也让平台把精力集中在任务协作、推荐、审核和成长激励上。

---

### 5.5 数据库脚本与 JPA 实体

项目维护了 P4 数据库初始化脚本：

```text
docs/P4/database/init.sql
```

脚本包含用户、仓库、Issue、PR、任务、分类、标签、接取记录、提交、审核、推荐、成长、通知等核心表。

当前脚本已做开发适配：

- `CREATE TABLE` 调整为 `CREATE TABLE IF NOT EXISTS`，方便开发环境重复执行。
- `users` 表增加 `token_version` 字段，与认证模块一致。
- 已在 Docker MySQL 临时库中验证建表语法和外键顺序。

开发阶段配置：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

这表示开发环境中 Hibernate 可以根据实体自动更新表结构。但生产级项目不应依赖 `ddl-auto=update`，而应使用版本化迁移工具。

底层原理：

JPA 通过实体类和注解描述对象与表之间的映射关系。Hibernate 根据实体元数据生成 SQL，完成插入、查询、更新和关联加载。`ddl-auto=update` 会在启动时检查实体和数据库结构差异并尝试更新表结构，适合开发但不适合生产，因为它不易审计，也难以回滚。

---

## 六、待实现后端功能规划

### 6.1 代码托管适配模块

后续需要实现：

- 仓库导入：`POST /api/v1/repositories/import`
- 仓库详情：`GET /api/v1/repositories/{repositoryId}`
- 手动同步仓库：`POST /api/v1/repositories/{repositoryId}/sync`
- 查询 Issue 列表：`GET /api/v1/repositories/{repositoryId}/issues`
- 创建分支、上传 commit、创建 PR、查询 PR。
- Webhook 接收：`POST /api/v1/code-host/webhooks/{hostType}`

生产级注意点：

- 外部 API 超时和重试。
- Webhook 签名校验。
- Webhook 幂等处理。
- 外部事件乱序处理。
- 外部 token 不进入普通业务表明文字段。

### 6.2 管理员审核模块

后续需要实现：

- 管理员审核任务发布。
- 任务退回。
- 任务下架。
- 管理员审核记录。

核心状态流转：

```text
PENDING_ADMIN_REVIEW -> PUBLISHED
PENDING_ADMIN_REVIEW -> DRAFT
PUBLISHED -> CLOSED
```

### 6.3 成果提交与审核模块

后续需要实现：

- 用户提交成果。
- 成果关联 PR。
- 维护者审核提交。
- 逐项 ReviewItem 反馈。
- 审核通过后触发任务完成、XP 和贡献记录。

核心状态：

```text
PENDING_REVIEW
APPROVED
CHANGES_REQUESTED
REJECTED
```

### 6.4 推荐匹配模块

规划中的推荐策略：

- 技术栈匹配。
- 成长阶段匹配。
- 历史贡献匹配。
- 新手友好标签加权。

设计上应采用策略模式：

```text
RecommendationStrategy
├── TechStackMatchStrategy
├── GrowthStageStrategy
└── ContributionHistoryStrategy
```

这样新增推荐规则时不需要频繁修改推荐服务主流程。

### 6.5 通知与成长模块

通知模块：

- 任务接取通知。
- 审核结果通知。
- 邮件通知。
- 每日汇总。

成长模块：

- XP 流水。
- 等级。
- 贡献记录。
- 后续徽章和排行榜。

---

## 七、生产级改进预设

### 7.1 数据库迁移

当前开发环境可以使用 JPA 自动更新，但生产级应切换到 Flyway 或 Liquibase。

建议结构：

```text
backend/src/main/resources/db/migration
├── V1__init_schema.sql
├── V2__add_token_version.sql
├── V3__add_quest_assignment_constraints.sql
└── V4__add_review_tables.sql
```

生产环境配置：

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

这样应用启动时只校验实体和数据库是否一致，不自动修改生产库。

### 7.2 Refresh Token 管理

当前 refreshToken 通过 tokenVersion 实现批量失效。生产级可以进一步增强：

- 将 refreshToken 的 jti 存入 Redis 或数据库。
- 支持单设备登出。
- 支持多设备会话管理。
- 记录登录 IP、设备、过期时间。
- 检测异常刷新行为。

### 7.3 权限模型细化

当前主要是角色权限。生产级需要资源级权限：

- 维护者只能管理自己仓库或自己发布的任务。
- 用户只能修改自己的提交。
- 管理员拥有平台级审核权限。
- 私有仓库需要额外访问控制。

可以考虑：

- 方法级权限：`@PreAuthorize`
- 领域权限服务：`PermissionService.canReviewSubmission(user, submission)`
- 数据库协作者表：repository_collaborators

### 7.4 并发控制

任务接取存在并发风险。生产级需要：

- `quest_assignments` 增加唯一约束或有效接取唯一索引。
- `quests` 增加 version 字段做乐观锁。
- 接取接口使用事务。
- 必要时对任务行加悲观锁。

### 7.5 API 文档与契约

当前 Swagger 因 springdoc 版本兼容可能存在 `/api-docs` 500 风险。生产级应：

- 升级兼容当前 Spring Boot 版本的 springdoc。
- 在 CI 中校验 OpenAPI 文档生成。
- 为前端提供稳定接口契约。
- 对错误码和 DTO 做文档化。

### 7.6 测试体系

生产级测试应分层：

| 测试类型 | 工具 | 目标 |
| --- | --- | --- |
| 单元测试 | JUnit + Mockito | 验证 Service 业务规则 |
| Controller 测试 | MockMvc | 验证路由、参数校验、权限、响应结构 |
| Repository 测试 | DataJpaTest | 验证 JPA 查询和映射 |
| 集成测试 | Testcontainers MySQL | 使用真实 MySQL 验证 SQL 兼容性 |
| API 回归测试 | Postman/Newman | 验证接口流程 |

### 7.7 可观测性

生产级后端应增加：

- 结构化日志。
- traceId 全链路传递。
- 关键接口耗时统计。
- 登录失败日志。
- Webhook 处理日志。
- 慢查询监控。
- 任务状态变更审计日志。

---

## 八、后端核心流程示意

### 8.1 登录流程

```text
用户提交邮箱和密码
        ↓
AuthController.login
        ↓
AuthService 查询用户
        ↓
BCrypt 校验密码
        ↓
JwtTokenProvider 签发 accessToken / refreshToken
        ↓
返回统一 ApiResponse
```

### 8.2 发布任务流程

```text
维护者携带 JWT 请求发布任务
        ↓
JwtAuthenticationFilter 解析身份
        ↓
QuestController.createQuest
        ↓
QuestService 校验角色、仓库、Issue、分类、标签
        ↓
创建 Quest，状态为 PENDING_ADMIN_REVIEW
        ↓
返回任务创建结果
```

### 8.3 接取任务流程

```text
初学者携带 JWT 请求接取任务
        ↓
后端解析当前用户
        ↓
校验任务存在且状态为 PUBLISHED
        ↓
校验任务未被接取
        ↓
创建 QuestAssignment
        ↓
任务状态变更为 IN_PROGRESS
        ↓
返回接取结果
```

---

## 九、模拟面试 Q&A

### Q1：这个项目是做什么的？

Git Guild 是一个游戏化代码托管与任务协作平台，目标是帮助开源新手通过真实 Issue 和 PR 完成贡献。平台把仓库中的 Issue 转化为任务，初学者可以筛选、接取、提交成果，维护者进行审核，系统后续还会提供推荐、通知和成长激励。

### Q2：你在项目中负责什么？

我主要负责后端核心功能开发，包括用户注册登录、JWT 鉴权、角色权限控制、任务发布、任务列表筛选、任务详情和任务接取接口。同时我也参与了数据库脚本维护、JPA 实体设计、统一响应和业务异常处理，并编写了认证与任务模块的测试。

### Q3：为什么采用模块化单体而不是微服务？

这个项目是课程团队项目，团队规模和周期有限。模块化单体可以保持部署简单、事务处理简单、开发协作成本低，同时通过包结构和接口边界保持模块清晰。等推荐、通知或代码托管模块复杂度上升后，再拆分服务会更合理。

### Q4：JWT 登录是怎么实现的？

用户登录成功后，后端签发 accessToken 和 refreshToken。JWT 中包含用户 ID、角色、token 类型、tokenVersion 和过期时间。请求业务接口时，客户端通过 Authorization 请求头携带 accessToken。后端 JWT Filter 校验签名和过期时间，并将用户信息写入 Spring Security 上下文。

### Q5：为什么还要设计 tokenVersion？

JWT 是无状态的，签发后在过期前不容易撤销。tokenVersion 存在数据库用户表中，refreshToken 中也保存签发时的 tokenVersion。当用户登出或修改密码时，数据库中的 tokenVersion 递增，旧 refreshToken 再来刷新时版本不匹配，就会被拒绝。

### Q6：密码为什么不能直接存明文？

明文密码一旦数据库泄露，用户账号会立即暴露。项目使用 BCrypt 存储密码哈希。BCrypt 会自动加 salt，并通过计算成本增加暴力破解难度。登录时不是解密密码，而是用用户输入和哈希进行匹配校验。

### Q7：任务发布时为什么要绑定 Issue？

项目目标是让任务来自真实工程工作，而不是随意创建文本任务。绑定 Issue 可以让任务和仓库问题、PR、审核流程形成闭环，也方便后续通过 Gitea/GitHub 同步 Issue 状态、PR 状态和 Webhook 事件。

### Q8：任务为什么需要状态机？

任务从创建到完成不是简单 CRUD，而是有明确生命周期：待管理员审核、已发布、进行中、审核中、已完成、已关闭。状态机能限制非法操作，例如未发布任务不能被普通用户接取，已进行中的任务不能被重复接取。

### Q9：如何保证同一个任务不会被多人同时接取？

当前实现中，服务层会在事务内校验任务状态和是否已有 active assignment，然后创建接取记录并更新任务状态。生产级还需要进一步加数据库唯一约束或乐观锁，防止高并发下两个请求同时通过校验。

### Q10：为什么列表查询使用 Specification？

任务列表有多个可选筛选条件，例如关键词、分类、标签、难度、技术栈、状态、分页和排序。Specification 可以根据请求参数动态拼接查询条件，避免为每种条件组合写一个 Repository 方法。

### Q11：JPA 和 SQL 脚本之间如何协调？

开发阶段使用 JPA 实体快速迭代表结构，`ddl-auto=update` 可以提高效率。但正式交付和生产部署应以版本化 SQL 迁移为准。项目目前维护了 P4 `init.sql`，后续可迁移到 Flyway，并将生产环境改为 `ddl-auto=validate`。

### Q12：为什么统一响应体很重要？

统一响应体可以让前端始终按相同结构处理成功和失败结果。比如 `code` 用于业务判断，`message` 用于展示，`details` 用于调试，`traceId` 用于定位日志。这样接口规模变大后仍然易于联调和排错。

### Q13：项目中如何做权限控制？

当前使用 Spring Security 做基础认证，业务层做角色校验。例如发布任务需要 MAINTAINER 或 ADMIN，接取任务要求登录用户且 ADMIN 默认不能接取普通任务。后续可以扩展到资源级权限，例如维护者只能审核自己发布的任务。

### Q14：Gitea 在项目中的作用是什么？

Gitea 是平台内代码托管底座，负责仓库、Issue、PR、分支和 Webhook 等基础能力。Git Guild 不自研 Git 存储，而是保存业务需要的元数据，并围绕任务、审核、推荐和成长做平台能力。

### Q15：如果 Swagger 打不开，你怎么排查？

先区分是 Swagger UI 页面打不开，还是 `/api-docs` 生成失败。如果 UI 能打开但 `/api-docs` 返回 500，通常是 OpenAPI 文档生成异常，可能和 springdoc 版本兼容、Controller 方法签名或 DTO 解析有关。业务接口可以先用 Postman 验证，再单独修 Swagger。

### Q16：你如何验证功能正确？

我做了三类验证：第一，使用 JUnit 和 Mockito 测认证、JWT、任务发布和接取的核心分支；第二，使用 Spring Boot Test 验证上下文能启动；第三，使用 Postman 和 Docker MySQL 做真实接口联调，并查询数据库验证用户、任务和接取记录写入正确。

### Q17：如果要把项目提升到生产级，你会优先改什么？

我会优先做四件事：第一，引入 Flyway 管理数据库迁移；第二，完善 refreshToken 存储和撤销机制；第三，加强任务接取并发控制；第四，补 Controller 集成测试和 Testcontainers MySQL 测试，保证接口和数据库行为更接近真实生产环境。
