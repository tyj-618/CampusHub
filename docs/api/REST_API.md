# CampusHub REST API 接口文档

版本：v1  
基础路径：`/api`  
数据格式：`application/json; charset=utf-8`

## 1. 通用约定

### 1.1 认证方式

除注册、登录、分类列表、帖子列表、帖子详情、用户公开主页、用户帖子列表、评论列表、热门帖子接口外，其余接口默认需要登录。

需要登录的接口在请求头中携带：

```http
Authorization: Bearer <token>
```

### 1.2 统一响应结构

所有接口都返回统一结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败响应示例：

```json
{
  "code": 40100,
  "message": "未登录或 token 无效",
  "data": null
}
```

### 1.3 分页响应结构

分页接口的 `data` 统一为：

```json
{
  "page": 1,
  "size": 10,
  "total": 128,
  "pages": 13,
  "records": []
}
```

分页参数：

| 参数 | 类型 | 默认值 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 1 | `>= 1` | 页码，从 1 开始 |
| size | integer | 10 | `1 - 50` | 每页数量 |

### 1.4 通用错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40001 | 用户名或密码错误 |
| 40100 | 未登录或 token 无效 |
| 40300 | 无权限 |
| 40400 | 资源不存在 |
| 40900 | 资源状态冲突 |
| 40901 | 用户名已存在 |
| 50000 | 系统内部错误 |

### 1.5 枚举说明

用户角色 `role`：

| 值 | 含义 |
| --- | --- |
| 0 | 普通用户 |
| 1 | 管理员 |

用户状态 `status`：

| 值 | 含义 |
| --- | --- |
| 0 | 正常 |
| 1 | 禁用 |

帖子状态 `status`：

| 值 | 含义 |
| --- | --- |
| 0 | 正常 |
| 1 | 删除 |
| 2 | 隐藏 |

通知类型 `type`：

| 值 | 含义 |
| --- | --- |
| 1 | 评论通知 |
| 2 | 点赞通知 |

通知阅读状态 `readStatus`：

| 值 | 含义 |
| --- | --- |
| 0 | 未读 |
| 1 | 已读 |

## 2. 认证模块

### 2.1 用户注册

```http
POST /api/auth/register
```

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| username | string | 是 | 3-32 位，只能包含字母、数字、下划线 | 用户名 |
| password | string | 是 | 6-32 位 | 密码 |
| nickname | string | 是 | 最长 32 位 | 昵称 |

请求示例：

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "小艾"
}
```

响应 `data`：

```json
{
  "userId": 1,
  "username": "alice",
  "nickname": "小艾"
}
```

### 2.2 用户登录

```http
POST /api/auth/login
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

响应 `data`：

```json
{
  "token": "session-token",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "username": "alice",
    "nickname": "小艾",
    "avatarUrl": null,
    "role": 0
  }
}
```

### 2.3 用户退出

```http
POST /api/auth/logout
```

认证：需要登录  
响应 `data`：

```json
true
```

## 3. 用户模块

### 3.1 获取当前用户信息

```http
GET /api/users/me
```

认证：需要登录

响应 `data`：

```json
{
  "id": 1,
  "username": "alice",
  "nickname": "小艾",
  "avatarUrl": null,
  "bio": "热爱校园生活",
  "role": 0,
  "status": 0,
  "createdAt": "2026-05-26T20:30:00"
}
```

### 3.2 修改当前用户资料

```http
PUT /api/users/me
```

认证：需要登录

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| nickname | string | 否 | 最长 32 位 | 昵称 |
| avatarUrl | string | 否 | 最长 255 位 | 头像地址 |
| bio | string | 否 | 最长 255 位 | 个人简介 |

响应 `data`：同“获取当前用户信息”。

### 3.3 查看用户公开主页

```http
GET /api/users/{userId}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| userId | integer | 用户 ID |

响应 `data`：

```json
{
  "id": 1,
  "username": "alice",
  "nickname": "小艾",
  "avatarUrl": null,
  "bio": "热爱校园生活",
  "postCount": 12,
  "commentCount": 34,
  "createdAt": "2026-05-26T20:30:00"
}
```

## 4. 分类模块

### 4.1 获取分类列表

```http
GET /api/categories
```

响应 `data`：

```json
[
  {
    "id": 1,
    "name": "课程交流",
    "code": "course",
    "sortOrder": 10
  }
]
```

说明：只返回 `status = 0` 的启用分类，并按 `sort_order` 升序排列。

## 5. 帖子模块

### 5.1 发布帖子

```http
POST /api/posts
```

认证：需要登录

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| categoryId | integer | 是 | 必须存在且启用 | 分类 ID |
| title | string | 是 | 最长 100 位 | 标题 |
| content | string | 是 | 最长 5000 位 | 正文内容 |

响应 `data`：

```json
{
  "postId": 1001
}
```

### 5.2 分页查询帖子列表

```http
GET /api/posts
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量，最大 50 |
| categoryId | integer | 否 | 无 | 分类 ID |
| keyword | string | 否 | 无 | 标题或正文关键词 |
| sort | string | 否 | latest | `latest` 最新，`hot` 热度 |

响应 `data.records[]`：

```json
{
  "id": 1001,
  "title": "高数复习资料怎么整理？",
  "summary": "想问问大家期末复习有什么方法...",
  "category": {
    "id": 1,
    "name": "课程交流",
    "code": "course"
  },
  "author": {
    "id": 1,
    "nickname": "小艾",
    "avatarUrl": null
  },
  "viewCount": 120,
  "likeCount": 8,
  "commentCount": 5,
  "hotScore": 36.5,
  "createdAt": "2026-05-26T20:30:00"
}
```

### 5.3 获取帖子详情

```http
GET /api/posts/{postId}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| postId | integer | 帖子 ID |

认证：可选。已登录时会返回当前用户是否点赞。

响应 `data`：

```json
{
  "id": 1001,
  "title": "高数复习资料怎么整理？",
  "content": "想问问大家期末复习有什么方法...",
  "category": {
    "id": 1,
    "name": "课程交流",
    "code": "course"
  },
  "author": {
    "id": 1,
    "nickname": "小艾",
    "avatarUrl": null
  },
  "viewCount": 121,
  "likeCount": 8,
  "commentCount": 5,
  "liked": false,
  "createdAt": "2026-05-26T20:30:00",
  "updatedAt": "2026-05-26T20:30:00"
}
```

说明：访问详情时会记录浏览量。当前实现使用内存计数 + 定时批量刷库，详情响应里的 `viewCount` 可能不是刚刚访问后的实时值。

### 5.4 编辑帖子

```http
PUT /api/posts/{postId}
```

认证：需要登录，仅作者或管理员可操作。

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| categoryId | integer | 是 | 必须存在且启用 | 分类 ID |
| title | string | 是 | 最长 100 位 | 标题 |
| content | string | 是 | 最长 5000 位 | 正文内容 |

响应 `data`：

```json
true
```

### 5.5 删除帖子

```http
DELETE /api/posts/{postId}
```

认证：需要登录，仅作者或管理员可操作。

响应 `data`：

```json
true
```

说明：当前实现为软删除，将 `post.status` 更新为 `1`。

### 5.6 获取用户发布的帖子

```http
GET /api/users/{userId}/posts
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量，最大 50 |

响应：同“分页查询帖子列表”。

### 5.7 获取热门帖子榜

```http
GET /api/posts/hot
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| limit | integer | 否 | 10 | 返回数量，最大 50 |
| categoryId | integer | 否 | 无 | 分类 ID |

响应 `data`：

```json
[
  {
    "id": 1001,
    "title": "高数复习资料怎么整理？",
    "categoryName": "课程交流",
    "viewCount": 121,
    "likeCount": 8,
    "commentCount": 5,
    "hotScore": 36.5
  }
]
```

说明：默认按 `post_stat.hot_score` 排序；开启 Redis 时会缓存热门帖子结果。

## 6. 评论模块

### 6.1 发表评论

```http
POST /api/posts/{postId}/comments
```

认证：需要登录

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| content | string | 是 | 最长 500 位 | 评论内容 |

响应 `data`：

```json
{
  "commentId": 3001
}
```

说明：评论成功后会增加帖子评论数和热度；评论别人的帖子时会发布评论事件，当前默认同步生成通知，开启 RocketMQ profile 后异步消费生成通知。

### 6.2 查询帖子评论列表

```http
GET /api/posts/{postId}/comments
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量，最大 50 |

响应 `data.records[]`：

```json
{
  "id": 3001,
  "content": "我一般先整理错题，再刷历年卷。",
  "author": {
    "id": 2,
    "nickname": "小林",
    "avatarUrl": null
  },
  "createdAt": "2026-05-26T21:00:00"
}
```

### 6.3 删除评论

```http
DELETE /api/comments/{commentId}
```

认证：需要登录，仅评论作者、帖子作者或管理员可操作。

响应 `data`：

```json
true
```

说明：当前实现为软删除，将 `comment.status` 更新为 `1`，并扣减帖子评论数和热度。

### 6.4 获取当前用户评论列表

```http
GET /api/users/me/comments
```

认证：需要登录

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量，最大 50 |

响应 `data.records[]`：

```json
{
  "id": 3001,
  "postId": 1001,
  "postTitle": "高数复习资料怎么整理？",
  "content": "我一般先整理错题，再刷历年卷。",
  "createdAt": "2026-05-26T21:00:00"
}
```

## 7. 点赞模块

### 7.1 点赞帖子

```http
POST /api/posts/{postId}/like
```

认证：需要登录

响应 `data`：

```json
{
  "liked": true,
  "likeCount": 9
}
```

说明：点赞别人的帖子时会发布点赞事件，当前默认同步生成通知，开启 RocketMQ profile 后异步消费生成通知。

### 7.2 取消点赞帖子

```http
DELETE /api/posts/{postId}/like
```

认证：需要登录

响应 `data`：

```json
{
  "liked": false,
  "likeCount": 8
}
```

### 7.3 查询当前用户是否点赞帖子

```http
GET /api/posts/{postId}/like
```

认证：需要登录

响应 `data`：

```json
{
  "liked": true
}
```

## 8. 通知模块

### 8.1 查询通知列表

```http
GET /api/notices
```

认证：需要登录

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量，最大 50 |
| readStatus | integer | 否 | 无 | `0` 未读，`1` 已读 |

响应 `data.records[]`：

```json
{
  "id": 5001,
  "type": 1,
  "content": "小林评论了你的帖子",
  "readStatus": 0,
  "sender": {
    "id": 2,
    "nickname": "小林",
    "avatarUrl": null
  },
  "postId": 1001,
  "commentId": 3001,
  "createdAt": "2026-05-26T21:10:00"
}
```

### 8.2 查询未读通知数

```http
GET /api/notices/unread-count
```

认证：需要登录

响应 `data`：

```json
{
  "count": 3
}
```

### 8.3 标记单条通知已读

```http
PUT /api/notices/{noticeId}/read
```

认证：需要登录，只能操作当前用户自己的通知。

响应 `data`：

```json
true
```

### 8.4 标记全部通知已读

```http
PUT /api/notices/read-all
```

认证：需要登录

响应 `data`：

```json
{
  "updatedCount": 3
}
```

## 9. 管理接口

管理接口都需要管理员登录。

### 9.1 隐藏帖子

```http
PUT /api/admin/posts/{postId}/hide
```

响应 `data`：

```json
true
```

说明：将 `post.status` 更新为 `2`。

### 9.2 恢复帖子

```http
PUT /api/admin/posts/{postId}/restore
```

响应 `data`：

```json
true
```

说明：将 `post.status` 更新为 `0`。

### 9.3 禁用用户

```http
PUT /api/admin/users/{userId}/disable
```

响应 `data`：

```json
true
```

说明：将 `user.status` 更新为 `1`。当前代码禁止管理员禁用自己，但未禁止管理员禁用其他管理员。

### 9.4 启用用户

```http
PUT /api/admin/users/{userId}/enable
```

响应 `data`：

```json
true
```

说明：将 `user.status` 更新为 `0`。

## 10. 接口清单

| 模块 | 方法 | 路径 | 说明 | 认证 |
| --- | --- | --- | --- | --- |
| 认证 | POST | `/api/auth/register` | 用户注册 | 否 |
| 认证 | POST | `/api/auth/login` | 用户登录 | 否 |
| 认证 | POST | `/api/auth/logout` | 用户退出 | 是 |
| 用户 | GET | `/api/users/me` | 当前用户信息 | 是 |
| 用户 | PUT | `/api/users/me` | 修改当前用户资料 | 是 |
| 用户 | GET | `/api/users/{userId}` | 用户公开主页 | 否 |
| 分类 | GET | `/api/categories` | 分类列表 | 否 |
| 帖子 | POST | `/api/posts` | 发布帖子 | 是 |
| 帖子 | GET | `/api/posts` | 帖子列表 | 否 |
| 帖子 | GET | `/api/posts/{postId}` | 帖子详情 | 可选 |
| 帖子 | PUT | `/api/posts/{postId}` | 编辑帖子 | 是 |
| 帖子 | DELETE | `/api/posts/{postId}` | 删除帖子 | 是 |
| 帖子 | GET | `/api/users/{userId}/posts` | 用户帖子列表 | 否 |
| 帖子 | GET | `/api/posts/hot` | 热门帖子榜 | 否 |
| 评论 | POST | `/api/posts/{postId}/comments` | 发表评论 | 是 |
| 评论 | GET | `/api/posts/{postId}/comments` | 帖子评论列表 | 否 |
| 评论 | DELETE | `/api/comments/{commentId}` | 删除评论 | 是 |
| 评论 | GET | `/api/users/me/comments` | 当前用户评论列表 | 是 |
| 点赞 | POST | `/api/posts/{postId}/like` | 点赞帖子 | 是 |
| 点赞 | DELETE | `/api/posts/{postId}/like` | 取消点赞 | 是 |
| 点赞 | GET | `/api/posts/{postId}/like` | 查询点赞状态 | 是 |
| 通知 | GET | `/api/notices` | 通知列表 | 是 |
| 通知 | GET | `/api/notices/unread-count` | 未读通知数 | 是 |
| 通知 | PUT | `/api/notices/{noticeId}/read` | 单条通知已读 | 是 |
| 通知 | PUT | `/api/notices/read-all` | 全部通知已读 | 是 |
| 管理 | PUT | `/api/admin/posts/{postId}/hide` | 隐藏帖子 | 管理员 |
| 管理 | PUT | `/api/admin/posts/{postId}/restore` | 恢复帖子 | 管理员 |
| 管理 | PUT | `/api/admin/users/{userId}/disable` | 禁用用户 | 管理员 |
| 管理 | PUT | `/api/admin/users/{userId}/enable` | 启用用户 | 管理员 |

## 11. 当前实现补充说明

### 11.1 Redis

项目中 Redis 目前主要用于两个方向：

- 登录 token 存储：默认使用内存版 `InMemoryTokenStore`；启用 Redis 配置后可以使用 `RedisTokenStore`。
- 热门帖子缓存：默认使用 `NoOpHotPostCache`；启用 Redis 配置后使用 `RedisHotPostCache` 缓存热门帖子列表。

热门帖子缓存采用轻量 Cache Aside 实现：

- 空结果也会写入缓存，减少无数据查询反复访问数据库。
- 缓存 TTL 会增加随机抖动，降低同一批 key 同时过期造成的缓存雪崩风险。
- 缓存未命中时使用 Redis 短期互斥锁，降低热点 key 失效后大量请求同时回源造成的缓存击穿风险。

### 11.2 RocketMQ

评论和点赞会发布领域事件：

- `CommentCreatedEvent`
- `PostLikedEvent`

默认 profile 下使用同步事件发布器，便于本地开发和测试；启用 `rocketmq` profile 后，通过 RocketMQ 发送和消费事件，再生成站内通知。

### 11.3 浏览量批量更新

帖子详情接口调用后不会立即同步更新数据库浏览量，而是先记录到内存计数器，再由定时任务批量刷新到 `post_stat`。这样可以降低高频浏览请求对数据库的写压力。

### 11.4 集成测试

测试环境使用 H2 内存数据库和 `test` profile，核心接口集成测试覆盖：

- 注册
- 登录
- 分类查询
- 发帖
- 评论
- 点赞
- 评论列表
- 点赞状态
- 未读通知数
- 通知列表
