# Git-Guild 后端开发复盘与面试准备

## 1. 项目简介

Git-Guild 是一个面向开源协作和新手贡献者成长的任务协作平台。平台围绕“仓库导入、任务发布、任务审核、贡献者接单、成果提交、维护者审核、成长激励、推荐匹配、通知提醒、排行榜与徽章展示”等流程，将代码托管平台上的仓库、Issue、Pull Request 与平台内部的任务、用户角色、成长数据关联起来，帮助维护者把真实开源需求拆分成可领取的任务，也帮助初学者找到适合自己的贡献入口。

后端采用 Spring Boot 3.5、Java 17、Spring MVC、Spring Security、JWT、Spring Data JPA、Bean Validation、MySQL、H2 测试库、JUnit 5、Mockito、MockMvc、JaCoCo 等技术实现。整体以 RESTful API 为核心，统一使用 `/api/v1` 作为业务接口前缀，统一返回 `ApiResponse` 响应结构，并通过 `GlobalExceptionHandler` 管理参数校验、业务异常和服务端兜底异常。

## 2. 后端技术栈

- Java 17：使用现代 Java 语法、record DTO、强类型枚举和面向对象领域模型。
- Spring Boot 3.5：负责自动配置、依赖管理、Web 容器集成和应用启动。
- Spring MVC：实现 Controller、请求映射、JSON 序列化、参数绑定和响应返回。
- Spring Security：实现接口鉴权、角色权限、JWT 认证过滤器和无状态会话。
- JJWT：生成、解析和校验访问令牌与刷新令牌。
- Spring Data JPA / Hibernate：实现实体映射、Repository 查询、事务管理和关系建模。
- MySQL：作为生产或本地开发数据库。
- H2：作为测试 profile 下的内存数据库，用于集成测试快速启动。
- Bean Validation：通过 `@Valid`、`@NotBlank`、`@Size`、`@Pattern` 等注解进行请求体参数校验。
- JUnit 5、Mockito、MockMvc：用于单元测试、服务测试和用户视角接口测试。
- JaCoCo：用于测试覆盖率统计。
- springdoc-openapi：提供 Swagger/OpenAPI 文档能力。

## 3. 后端架构设计

后端按业务领域拆分包结构：

- `user`：用户注册、登录、刷新令牌、登出、当前用户、修改密码。
- `security`：JWT 签发与解析、认证过滤器、安全配置、当前登录用户 Principal。
- `quest`：任务发布、任务列表、任务详情、任务接取、任务提交管理员审核、管理员审核、任务分类与标签。
- `review`：成果提交、提交草稿、维护者审核、审核记录与审核项。
- `codehost`：代码托管仓库、Issue、Pull Request、本地镜像、Gitea 适配与同步。
- `growth`：XP、等级、贡献记录、排行榜、徽章展示。
- `recommendation`：任务推荐、贡献者推荐、推荐理由。
- `notification`：通知列表、已读、全部已读、业务通知发送。
- `guide`：仓库新手引导。
- `common`：统一响应、业务异常、全局异常处理。

整体分层如下：

1. Controller 层：只负责 HTTP 入参、认证用户解析、响应包装，不直接承载复杂业务。
2. Service 层：承载业务规则、状态流转、权限判断、事务控制。
3. Repository 层：通过 Spring Data JPA 操作实体。
4. Domain 层：用实体方法封装状态变更，例如任务状态流转、提交审核状态变更、成长值累计。
5. DTO 层：隔离接口响应和数据库实体，避免向前端暴露敏感字段或懒加载对象。

底层原理：Spring MVC 根据注解扫描 Controller，将请求路径映射到方法；Spring Security 在请求进入 Controller 前执行认证过滤链；JWT Filter 从请求头读取 Bearer Token 并构建 Authentication；Service 方法通过 `@Transactional` 开启事务；JPA 将实体变化同步到数据库；异常由 `@RestControllerAdvice` 统一转换为 JSON 响应。

## 4. 数据库与领域模型

核心实体包括：

- `User`：用户账户，包含用户名、邮箱、密码哈希、角色、状态、tokenVersion。
- `CodeRepository`：代码仓库镜像，保存仓库名、来源地址、托管平台类型、默认分支、同步状态。
- `CodeIssue`：外部 Issue 本地镜像，保存外部 Issue 编号、标题、状态。
- `CodePullRequest`：外部 PR 本地镜像，保存外部 PR 编号、源分支、目标分支、状态、合并时间。
- `Quest`：悬赏任务，关联发布者、仓库、Issue、分类、标签、难度、技术栈、XP 奖励和状态。
- `QuestAssignment`：任务接取记录，关联任务和接取人，记录接取状态。
- `AdminReviewRecord`：管理员审核任务的记录。
- `Submission`：用户成果提交，关联任务、提交人和 PR。
- `ReviewRecord` / `ReviewItem`：维护者审核记录和逐项反馈。
- `GrowthProfile`：用户成长档案，包含总 XP、等级、完成任务数。
- `XpTransaction`：XP 流水，记录 XP 发放原因。
- `ContributionRecord`：贡献记录，作为成长激励幂等键。
- `Notification`：通知记录。

设计重点：

- 用外键表达领域关系，避免仅靠 ID 字段导致对象关系分散。
- 用唯一约束与业务查询保证幂等，例如同一用户同一任务只发放一次 XP。
- 用枚举表达固定状态，例如 `QuestStatus`、`AssignmentStatus`、`SubmissionStatus`、`ReviewDecision`。
- 用领域方法封装状态变化，例如 `Quest.approve()`、`Quest.markCompleted()`、`Submission.approve()`。

底层原理：JPA 通过实体注解建立对象与表的映射。`@ManyToOne`、`@OneToOne`、`@ManyToMany` 等关系注解会让 Hibernate 生成或维护外键关系。事务提交时，Hibernate 的脏检查机制会发现实体字段变化并生成 SQL 更新。

## 5. 用户认证与 JWT 模块

### 5.1 已实现功能

- 用户注册：校验用户名、邮箱、密码复杂度，写入 BCrypt 加密后的密码。
- 用户登录：校验邮箱密码，返回 accessToken、refreshToken 和用户信息。
- 刷新令牌：使用 refreshToken 换取新的访问令牌。
- 登出：通过 `tokenVersion` 轮转让旧 Token 失效。
- 当前用户：从 JWT 中解析当前用户 ID，再查询数据库返回用户信息。
- 修改密码：校验旧密码，写入新密码哈希，同时轮转 tokenVersion。

### 5.2 关键设计

- JWT 中保存 `userId`、`roles`、`tokenType`、`tokenVersion`。
- accessToken 用于访问业务接口，refreshToken 用于刷新令牌。
- SecurityConfig 设置无状态会话：`SessionCreationPolicy.STATELESS`。
- 除注册、登录、刷新、Swagger、公开任务列表等接口外，其余接口默认需要认证。

底层原理：JWT 是由 Header、Payload、Signature 三部分组成的签名令牌。后端用 HMAC Secret 对 Header 和 Payload 签名。请求到达时，服务端重新计算签名并校验过期时间。因为 JWT 自包含用户信息，所以后端不需要存储 Session。`tokenVersion` 用于解决 JWT 天然无法主动失效的问题：用户登出或改密后，数据库中的 tokenVersion 增加，旧 Token 版本不匹配即失效。

## 6. 角色权限设计

项目使用三类角色：

- `BEGINNER`：初学者，可以浏览任务、接取任务、提交成果、查看推荐和成长信息。
- `MAINTAINER`：维护者，可以导入仓库、发布任务、审核用户成果。
- `ADMIN`：管理员，可以审核任务发布、下架任务、管理任务分类和标签。

权限控制方式：

- 全局认证由 SecurityConfig 控制。
- 管理员专属接口使用 `@PreAuthorize("hasRole('ADMIN')")`。
- 业务级权限在 Service 中判断，例如成果审核必须是任务发布者、仓库拥有者或管理员。

底层原理：Spring Security 将 JWT 中的角色转换为 GrantedAuthority。`hasRole('ADMIN')` 实际会匹配 `ROLE_ADMIN`。方法执行前，AOP 拦截器会根据注解判断当前 Authentication 是否具备权限。

## 7. 任务与悬赏模块

### 7.1 已实现功能

- 发布任务：维护者基于仓库和 Issue 创建任务草稿。
- 提交管理员审核：任务从 `DRAFT` 或 `REJECTED` 进入 `PENDING_ADMIN_REVIEW`。
- 管理员审核：管理员可以通过发布、驳回或下架任务。
- 任务列表：支持关键词、分类、标签、难度、技术栈、状态、分页和排序。
- 任务详情：公开任务可匿名查看；非公开状态只允许发布者、管理员或相关接取人查看。
- 接取任务：初学者可接取已发布任务，任务进入进行中。
- 任务分类/标签：支持公开查询，管理员创建与更新。

### 7.2 状态流转

任务状态包括：

- `DRAFT`：草稿。
- `PENDING_ADMIN_REVIEW`：等待管理员审核。
- `PUBLISHED`：已发布，可被接取。
- `IN_PROGRESS`：已有用户接取并进行中。
- `IN_REVIEW`：用户已提交成果，等待维护者审核。
- `COMPLETED`：审核通过，任务完成。
- `REJECTED`：管理员驳回发布。
- `CLOSED`：任务关闭或下架。

底层原理：状态机可以防止非法业务操作。例如只有 `PENDING_ADMIN_REVIEW` 才能被管理员发布或驳回，只有 `PUBLISHED`/`IN_PROGRESS` 可以被接取。状态流转被封装在实体和 Service 中，而不是允许前端直接传状态值修改。

## 8. 代码支撑平台模块

### 8.1 已实现功能

- 仓库导入：维护者导入 Gitea/GitHub 风格仓库地址，后端保存本地仓库镜像。
- 仓库详情：查询仓库基础信息、默认分支和同步状态。
- 仓库同步：更新仓库同步状态。
- Issue 查询：查询仓库下 Issue 本地镜像。
- 分支创建：提供分支创建接口响应。
- Commit 创建：提供提交文件变更接口响应。
- Pull Request 创建与详情：保存和查询 PR 本地镜像。
- Webhook 接收：接收代码托管平台事件。
- PR 同步服务：通过 GiteaAdapter 同步外部 PR，使用外部 PR 编号作为幂等依据。

### 8.2 关键设计

- 平台内部不直接依赖外部平台作为业务主库，而是维护本地镜像。
- `CodePullRequestRepository.findByRepositoryRepositoryIdAndExternalPrId` 用于防止重复插入 PR。
- 外部平台不可用时，以业务异常返回，避免网络异常穿透到 Controller。

底层原理：外部系统集成通常使用 Adapter 模式隔离第三方 API。业务层只依赖抽象接口，不直接关心 Gitea 的响应格式。同步时使用 upsert 思路：先按外部唯一 ID 查询本地记录，存在则更新，不存在则插入，避免重复数据。

## 9. 成果提交与维护者审核模块

### 9.1 已实现功能

- 提交草稿：用户接取任务后，可以查询可提交 PR 列表、仓库、默认分支和完成标准。
- 创建成果提交：提交任务 ID、PR ID 和说明，任务进入审核状态。
- 查询提交详情：提交者、发布者、管理员等相关人员可以查看提交。
- 维护者审核：维护者可通过、要求修改或拒绝提交。
- 审核项反馈：支持逐项 checkpoint 反馈。
- 审核通过后触发成长激励和通知。

### 9.2 核心约束

- 只有任务接取人才能提交成果。
- 不能重复创建待审核提交。
- 提交的 PR 必须属于任务关联仓库。
- 审核通过时，PR 必须是 `MERGED` 状态。
- 审核者必须是维护者或管理员，并且需要满足任务发布者、仓库拥有者或管理员的身份。

底层原理：成果提交与审核是典型的事务型业务。审核通过时需要同时更新 Submission、Quest、QuestAssignment、GrowthProfile、XpTransaction、ContributionRecord。如果中间失败，应通过事务回滚保证状态一致。

## 10. 成长激励、排行榜与徽章

### 10.1 已实现功能

- 成长档案：记录用户累计 XP、等级和完成任务数量。
- XP 流水：记录每次 XP 发放原因。
- 贡献记录：记录用户完成的任务贡献。
- 成长摘要：查询当前用户等级、总 XP、下一等级 XP 和完成任务数。
- XP 排行榜：公开查询，按总 XP、完成任务数、用户 ID 排序。
- 徽章展示：当前用户查询规则型徽章获得状态和进度。

### 10.2 徽章规则

- `FIRST_COMPLETION`：完成任务数量 >= 1。
- `XP_APPRENTICE`：累计 XP >= 100。
- `QUEST_EXPLORER`：完成任务数量 >= 3。
- `LEVEL_RISER`：等级 >= 3。

底层原理：排行榜基于聚合表 `growth_profiles` 排序，不实时扫描所有贡献记录，查询效率更高。徽章当前采用规则计算，不落单独徽章表，适合规则较少且稳定性尚未完全确定的阶段。后续如需要运营配置徽章，可增加 `badges`、`user_badges` 表。

## 11. 推荐匹配模块

### 11.1 已实现功能

- 推荐任务：根据用户历史贡献技术栈、难度、已接任务等因素为初学者推荐任务。
- 推荐贡献者：根据成长档案为任务推荐潜在贡献者。
- 推荐理由：返回当前用户与任务之间的解释性匹配原因。

### 11.2 推荐策略

任务推荐采用 `tech-difficulty` 策略：

- 已完成任务的技术栈会形成用户偏好。
- 难度与用户历史完成情况匹配时加分。
- 已接取或已完成任务会被排除。
- 返回 score、strongMatch、reasons，方便前端解释推荐结果。

底层原理：推荐系统可以先从规则打分开始，而不是一开始就上机器学习。规则打分具备可解释性强、实现成本低、适合小数据量项目的优点。随着数据增加，可再引入协同过滤或向量召回。

## 12. 通知模块

### 12.1 已实现功能

- 查询当前用户通知列表。
- 标记单条通知已读。
- 标记全部通知已读。
- 任务提交、审核结果等业务动作触发通知。

### 12.2 事务设计

通知发送使用独立事务，并且调用方捕获异常，避免通知失败影响主业务。

底层原理：通知属于旁路副作用，不应该导致主流程失败。例如审核通过已经成功写入，如果通知服务失败，不应该回滚审核结果。使用 `REQUIRES_NEW` 可以让通知在独立事务中提交；调用方捕获异常可以保证主事务不被通知异常污染。

## 13. 统一异常与响应

所有业务接口统一返回 `ApiResponse`：

- `code`：成功为 `SUCCESS`，失败为业务错误码。
- `message`：简要说明。
- `data`：业务数据。
- `details`：错误细节。
- `timestamp`：响应时间。
- `traceId`：请求追踪 ID。

常见错误码：

- `VALIDATION_FAILED`：参数校验失败。
- `UNAUTHORIZED`：未登录或令牌无效。
- `FORBIDDEN`：权限不足。
- `USER_NOT_FOUND`：用户不存在。
- `QUEST_NOT_FOUND`：任务不存在。
- `REPOSITORY_NOT_FOUND`：仓库不存在。
- `INTERNAL_ERROR`：服务端内部错误。

底层原理：`@RestControllerAdvice` 可以全局拦截 Controller 抛出的异常，将 Java 异常转换成统一 JSON。这样前端不需要针对不同异常格式写多套处理逻辑。

## 14. 测试与质量保障

### 14.1 测试类型

- 单元测试：验证领域模型和 Service 业务规则。
- Controller 测试：验证 HTTP 入参、响应结构和当前用户解析。
- 集成测试：启动 Spring 上下文、使用 H2 数据库和真实 Spring Security 调用接口。
- P3 文档覆盖测试：按用户旅程覆盖 P3 API 文档中的全部接口。

### 14.2 测试命令

```powershell
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd" test
```

最终结果：

```text
Tests run: 97, Failures: 0, Errors: 0, Skipped: 7
```

其中 skipped 是 Gitea live/spike 测试，需要外部环境时才运行。

底层原理：MockMvc 不需要启动真实端口即可模拟 HTTP 请求，仍然可以经过 Spring MVC 参数绑定、过滤器链、Controller 和异常处理器。H2 使用 MySQL 模式模拟数据库行为，使集成测试更快、更稳定。

## 15. 后端开发流程复盘

1. 阅读 P3/P4 文档，明确 API 规范、业务流程和数据库脚本。
2. 搭建 Spring Boot 后端结构，按领域拆分包。
3. 实现认证模块，完成注册、登录、JWT、刷新、登出、改密。
4. 实现任务核心模块，包括发布、提交审核、管理员审核、列表、详情、接单。
5. 实现成果提交与维护者审核模块，建立 Submission、ReviewRecord、ReviewItem。
6. 实现代码支撑平台镜像模块，接入仓库、Issue、PR 和 Gitea 适配器。
7. 实现成长激励模块，审核通过后发放 XP、写流水和贡献记录。
8. 实现推荐模块，按技术栈和难度进行规则打分。
9. 实现通知模块，业务动作触发通知，通知失败不影响主流程。
10. 补充排行榜和徽章 API 文档，并实现对应后端接口。
11. 补齐 P3 文档中缺失接口，保证全部文档接口可调用。
12. 使用 MockMvc 和 H2 完成用户视角接口测试，覆盖边界参数、权限和状态流转。
13. 统一异常处理和响应结构，提升前后端联调稳定性。

## 16. 可写入简历的项目描述

Git-Guild 是一个面向开源协作的新手贡献任务平台，支持维护者导入代码仓库并基于 Issue 发布任务，管理员审核任务后开放给初学者接取，用户完成任务后提交 Pull Request，由维护者审核并触发 XP、贡献记录、排行榜和徽章等成长激励。项目后端基于 Spring Boot 3、Spring Security、JWT、Spring Data JPA、MySQL 构建，采用 RESTful API、领域分层、统一异常处理和完整接口测试保障系统稳定性。

本人负责并完成后端核心开发，包括用户认证与角色权限、任务发布与管理员审核、任务接取、成果提交与维护者审核、代码托管平台仓库/Issue/PR 镜像、成长激励、通知、推荐匹配、排行榜与徽章展示、任务分类标签、新手引导等模块；设计并实现 JWT 无状态认证、任务与提交审核状态机、XP 幂等发放机制、基于技术栈与难度的规则推荐算法、统一响应和异常处理体系，并使用 JUnit5、Mockito、MockMvc、H2 构建覆盖 P3 API 文档的用户视角集成测试，最终后端测试 97 个用例全部通过。

## 17. 面试高频问题与回答

### Q1：这个项目解决什么问题？

回答：Git-Guild 解决的是开源项目维护者和新手贡献者之间协作成本高的问题。维护者可以把仓库 Issue 转化为带难度、技术栈、奖励 XP 和完成标准的任务；初学者可以浏览、接取、提交 PR；维护者审核后平台记录成长数据。底层上，它把外部代码托管平台数据映射成本地领域模型，再通过任务状态机和审核流程保证协作可控。

### Q2：为什么使用 JWT，而不是 Session？

回答：项目是前后端分离 REST API，JWT 更适合无状态认证。服务端不保存登录 Session，减少横向扩展时的 Session 共享问题。JWT 自带用户 ID、角色、令牌类型和 tokenVersion。底层原理是服务端用密钥签名 Token，请求时验证签名和过期时间。为了弥补 JWT 不易主动失效的问题，我加入 tokenVersion，登出或改密时数据库版本增加，旧 Token 即使未过期也会失效。

### Q3：Spring Security 在项目中怎么工作的？

回答：请求进入应用后先经过 Security Filter Chain。JWT Filter 从 Authorization Header 中取出 Bearer Token，使用 JwtTokenProvider 验证并解析 Token，然后构造 Authentication 放入 SecurityContext。后续 Controller 或 `@PreAuthorize` 就可以读取当前用户和角色。底层原理是 Servlet Filter 链在 Spring MVC 调用 Controller 前执行，SecurityContextHolder 使用线程上下文保存当前请求的认证信息。

### Q4：如何设计角色权限？

回答：项目分为 BEGINNER、MAINTAINER、ADMIN。BEGINNER 接取和提交任务，MAINTAINER 发布任务和审核提交，ADMIN 审核任务发布和管理分类标签。全局上 SecurityConfig 控制接口是否需要登录；方法级用 `@PreAuthorize` 控制管理员接口；业务级权限放在 Service 中判断，例如审核提交时必须是任务发布者、仓库拥有者或管理员。底层原理是角色会被转换为 GrantedAuthority，Spring Security 在方法调用前用 AOP 执行权限表达式。

### Q5：任务状态机是怎么设计的？

回答：任务状态从 DRAFT 到 PENDING_ADMIN_REVIEW，再到 PUBLISHED，用户接取后进入 IN_PROGRESS，提交成果后进入 IN_REVIEW，维护者审核通过后进入 COMPLETED。管理员驳回则进入 REJECTED，下架则进入 CLOSED。状态流转被封装在 Quest 实体和 Service 中，避免前端直接改状态。底层原理是有限状态机：每个状态只允许特定事件触发转移，这能防止非法业务路径。

### Q6：为什么任务发布后还要管理员审核？

回答：平台面向开源新手，任务质量会影响用户体验。管理员审核可以过滤描述不清、奖励不合理、仓库不可用的任务。技术上，维护者创建任务只是 DRAFT，提交后进入 PENDING_ADMIN_REVIEW，只有 ADMIN 调用审核接口通过后才 PUBLISHED。底层原理是将发布动作拆分成“提交申请”和“审核确认”，用状态机保证中间态可追踪。

### Q7：成果提交模块如何保证提交合法？

回答：提交成果时会校验提交者是否为任务接取人，任务是否处于可提交状态，是否已有待审核提交，PR 是否属于任务关联仓库。这样可以防止未接取任务的人提交、防止重复提交、防止拿其他仓库 PR 冒充。底层原理是 Service 层同时查询 Quest、Assignment、PullRequest、Submission，并在事务内完成状态变更。

### Q8：维护者审核通过后发生什么？

回答：审核通过后，Submission 变为 APPROVED，Quest 变为 COMPLETED，Assignment 变为 COMPLETED，同时调用 GrowthService 发放 XP、记录 XP 流水和贡献记录，并发送通知。底层原理是这些操作处于同一个事务中，确保审核结果和成长激励一致；如果主流程失败，事务回滚。

### Q9：XP 发放如何保证幂等？

回答：通过 ContributionRecord 作为幂等键，同一个用户同一个任务只能生成一条贡献记录。GrowthService 发放 XP 前先查询贡献记录是否存在，存在则直接跳过。数据库层也可以用唯一约束兜底。底层原理是幂等设计：同一个业务事件重复执行多次，结果和执行一次一致。

### Q10：排行榜为什么基于 growth_profiles 而不是实时扫描贡献记录？

回答：排行榜需要频繁查询，如果每次都扫描贡献记录聚合 XP，会有性能问题。growth_profiles 是用户成长聚合表，保存 totalXp、level、completedQuestCount，查询时只需要排序。底层原理是读优化：把频繁读取的聚合结果预先维护，牺牲少量写入复杂度换取查询效率。

### Q11：徽章为什么暂时不建表？

回答：当前徽章规则较少且固定，例如首次完成、XP 达 100、完成 3 个任务、等级达 3。这类徽章可以根据 GrowthProfile 实时计算，无需持久化。底层原理是规则型派生数据不一定需要落库，避免数据冗余和规则变更时的历史数据修正。后续若要支持运营配置、佩戴徽章、稀有徽章，再扩展 badge 表和 user_badge 表。

### Q12：推荐算法如何实现？

回答：推荐任务采用规则打分，结合用户历史完成任务的技术栈、任务难度和当前已接取/已完成任务进行过滤和排序。返回结果包含 score、strongMatch、reasons。底层原理是基于内容的推荐：根据任务属性和用户历史偏好计算匹配度，优点是可解释、冷启动成本低。

### Q13：为什么要保存代码平台的本地镜像？

回答：直接依赖外部平台会导致查询慢、不可控，外部服务不可用时业务受影响。本地保存 Repository、Issue、PullRequest 镜像后，平台核心流程可以基于本地数据运行。底层原理是数据同步与缓存：通过外部 ID 建立映射，定期或按需同步外部状态。

### Q14：GiteaAdapter 的作用是什么？

回答：GiteaAdapter 隔离了外部 Gitea API 调用，业务层不直接依赖 Gitea 请求细节。这样后续替换 GitHub、GitLab 或增加多平台支持时，只需要增加新的 Adapter。底层原理是适配器模式：把第三方接口转换成系统内部统一接口。

### Q15：Webhook 为什么需要幂等？

回答：代码平台的 Webhook 可能重复投递。如果每次投递都直接写入数据，可能导致重复 PR、重复通知或重复状态变更。项目通过外部资源 ID 查询已有记录再更新，避免重复插入。底层原理是分布式系统中消息至少一次投递常见，因此消费者必须具备幂等性。

### Q16：通知为什么用独立事务？

回答：通知是副作用，不应影响主业务。例如审核已经完成，通知发送失败不应该回滚审核结果。通知服务使用独立事务，调用方捕获异常。底层原理是事务传播行为 `REQUIRES_NEW` 会暂停当前事务，开启新事务执行通知写入。

### Q17：统一异常处理有什么好处？

回答：统一异常处理可以让前端始终收到同一结构的 JSON，例如 code、message、details、timestamp、traceId。这样前端无需区分校验异常、业务异常、类型转换异常。底层原理是 `@RestControllerAdvice` 通过 Spring MVC 的异常解析机制拦截异常并返回响应。

### Q18：如何处理参数校验？

回答：请求体使用 Bean Validation 注解，例如 NotBlank、Size、Pattern。Query/path 参数类型错误通过 GlobalExceptionHandler 捕获 MethodArgumentTypeMismatchException。底层原理是 Spring MVC 在参数绑定时会执行类型转换和校验，如果失败会抛异常，再由全局异常处理转为 400。

### Q19：为什么不用直接返回 Entity？

回答：直接返回 Entity 可能暴露敏感字段，例如密码哈希、邮箱、角色，也可能触发懒加载异常或循环引用。项目使用 DTO/record 组织响应。底层原理是 Entity 是持久化模型，DTO 是接口模型，二者职责不同。

### Q20：JPA 的懒加载有什么风险？

回答：懒加载对象在事务关闭后访问可能抛 LazyInitializationException。项目在 Service 的只读事务中完成必要字段读取，再映射为 DTO 返回。底层原理是懒加载代理需要 Hibernate Session 才能查询关联数据，事务结束后 Session 通常关闭。

### Q21：为什么测试使用 H2？

回答：H2 启动快，不依赖本机 MySQL，适合集成测试。通过 MySQL Mode 尽量模拟 MySQL 行为。底层原理是测试环境使用独立 profile，Spring Boot 加载 application-test.yml，将 DataSource 切换成 H2。

### Q22：MockMvc 和真实 Postman 测试有什么区别？

回答：MockMvc 不启动真实端口，但会经过 Spring MVC、过滤器链、Controller、异常处理器，适合自动化测试。Postman 是真实 HTTP 调用，适合手工联调。底层原理是 MockMvc 在 JVM 内模拟 Servlet 请求和响应，速度更快且可断言 JSON 字段。

### Q23：如何证明全部 P3 API 文档接口都可用？

回答：我新增了 P3ApiDocumentIntegrationTest，按用户旅程调用认证、分类标签、代码支撑、新手引导、任务、管理员审核、成果审核、推荐、排行榜、徽章等接口。最终完整测试结果为 97 个测试通过。底层原理是集成测试把接口文档转化为可执行用例，避免文档和实现脱节。

### Q24：如果 PR 未合并，为什么审核不能通过？

回答：平台要求成果提交对应的 PR 已经合并，才能认为任务完成。否则代码尚未真正进入仓库，不能发放奖励。底层原理是业务状态需要和外部代码状态保持一致，审核通过前检查 PullRequest.status 是否为 MERGED。

### Q25：如何防止重复接取或重复提交？

回答：接取时检查任务状态和已有 Assignment；提交时检查同一用户同一任务是否已有待审核提交；成长发放时检查 ContributionRecord。底层原理是在写入前做业务唯一性判断，并用数据库约束作为并发兜底。

### Q26：项目中事务边界怎么划分？

回答：会改变多个实体状态的业务方法使用 `@Transactional`，例如任务审核、成果审核、成长发放。只读查询使用 `@Transactional(readOnly = true)`。底层原理是 Spring 通过 AOP 创建事务代理，在方法开始时获取连接并开启事务，方法成功提交，异常回滚。

### Q27：为什么要有 traceId？

回答：traceId 可以帮助定位一次请求对应的错误日志。当前 ApiResponse 每次生成 `req-UUID`。底层原理是分布式系统常用请求追踪 ID 将前端报错、网关日志、服务日志关联起来。

### Q28：这个项目后续如何优化？

回答：可以从四个方向优化：第一，引入 Flyway 或 Liquibase 管理数据库迁移；第二，为代码平台同步增加异步队列和重试机制；第三，推荐系统从规则打分演进到向量召回或协同过滤；第四，徽章从规则计算扩展到可配置运营体系。底层原理分别对应数据库版本管理、异步消息可靠性、推荐召回排序、多表配置化建模。

### Q29：这个项目最大的后端亮点是什么？

回答：亮点是把任务协作、代码平台镜像、审核状态机和成长激励闭环整合到一个一致的后端系统里。不是简单 CRUD，而是有状态流转、权限边界、幂等发放、外部平台适配、推荐解释和完整接口测试。底层原理涉及认证授权、事务一致性、有限状态机、幂等设计、适配器模式和集成测试。

### Q30：如果面试官问你负责了什么，怎么回答？

回答：我负责 Git-Guild 后端整体设计与实现，包括认证授权、任务生命周期、管理员审核、成果提交与维护者审核、代码托管平台镜像、成长激励、排行榜徽章、推荐匹配、通知、任务分类标签、新手引导和 P3 文档接口覆盖测试。我重点解决了状态流转一致性、JWT 无状态认证、XP 幂等发放、接口统一异常处理、外部平台数据镜像和用户视角集成测试覆盖问题。
