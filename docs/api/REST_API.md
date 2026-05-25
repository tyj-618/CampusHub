# CampusHub REST API 接口文档

版本：v1  
基础路径：`/api`  
数据格式：`application/json; charset=utf-8`

## 1. 通用约定

### 1.1 认证方式

除注册、登录、分类列表、帖子列表、帖子详情、评论列表、热门帖子接口外，其余接口默认需要登录。

请求头：

```http
Authorization: Bearer <token>
```

### 1.2 统一响应结构

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 40001,
  "message": "用户名或密码错误",
  "data": null
}
```

### 1.3 分页响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "size": 10,
    "total": 128,
    "pages": 13,
    "records": []
  }
}
```

分页参数默认值：

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| page | integer | 1 | 页码，从 1 开始 |
| size | integer | 10 | 每页数量，建议最大 50 |

### 1.4 通用错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40001 | 用户名或密码错误 |
| 40100 | 未登录或 token 无效 |
| 40300 | 无权限 |
| 40400 | 资源不存在 |
| 40900 | 资源状态冲突，例如重复点赞 |
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

点赞状态 `status`：

| 值 | 含义 |
| --- | --- |
| 0 | 已点赞 |
| 1 | 已取消 |

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
| username | string | 是 | 3-32 位，字母数字下划线 | 用户名 |
| password | string | 是 | 6-32 位 | 密码 |
| nickname | string | 是 | 1-32 位 | 昵称 |

示例：

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

说明：密码入库前必须加密，不返回密码字段。

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
  "token": "jwt-or-session-token",
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

说明：如果使用 Redis 保存 token 或登录态，此接口应清理服务端登录状态。

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
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "热爱校园生活",
  "role": 0,
  "status": 0,
  "createdAt": "2026-05-25T20:30:00"
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
| nickname | string | 否 | 1-32 位 | 昵称 |
| avatarUrl | string | 否 | 最大 255 位 | 头像地址 |
| bio | string | 否 | 最大 255 位 | 个人简介 |

响应 `data`：同当前用户信息。

### 3.3 查看用户主页

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
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "热爱校园生活",
  "postCount": 12,
  "commentCount": 34,
  "createdAt": "2026-05-25T20:30:00"
}
```

说明：不返回密码、角色、状态等敏感或管理字段。

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

说明：只返回 `status = 0` 的启用分类，按 `sort_order` 升序。

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
| title | string | 是 | 1-100 位 | 标题 |
| content | string | 是 | 1-5000 位 | 正文内容 |

响应 `data`：

```json
{
  "postId": 1001
}
```

说明：创建帖子时同步创建 `post_stat` 记录，计数初始值为 0。

### 5.2 分页查询帖子列表

```http
GET /api/posts
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量 |
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
  "createdAt": "2026-05-25T20:30:00"
}
```

说明：只查询 `status = 0` 的正常帖子。

### 5.3 获取帖子详情

```http
GET /api/posts/{postId}
```

路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| postId | integer | 帖子 ID |

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
  "createdAt": "2026-05-25T20:30:00",
  "updatedAt": "2026-05-25T20:30:00"
}
```

说明：

- 访问详情时增加浏览量。
- 浏览量可以先同步更新 MySQL，后续优化为 Redis 缓存加定时批量刷库。
- 未登录用户访问时 `liked` 默认为 `false`。

### 5.4 编辑帖子

```http
PUT /api/posts/{postId}
```

认证：需要登录，仅作者或管理员可操作。

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| categoryId | integer | 否 | 必须存在且启用 | 分类 ID |
| title | string | 否 | 1-100 位 | 标题 |
| content | string | 否 | 1-5000 位 | 正文内容 |

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

说明：第一版建议软删除，将 `post.status` 更新为 `1`。

### 5.6 获取用户发布的帖子

```http
GET /api/users/{userId}/posts
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量 |

响应：同帖子列表分页结构。

### 5.7 获取热门帖子榜

```http
GET /api/posts/hot
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| limit | integer | 否 | 10 | 返回数量，建议最大 50 |
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

说明：第一版可按 `post_stat.hot_score` 排序；后续可迁移到 Redis ZSet。

## 6. 评论模块

### 6.1 发表评论

```http
POST /api/posts/{postId}/comments
```

认证：需要登录

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| content | string | 是 | 1-500 位 | 评论内容 |

响应 `data`：

```json
{
  "commentId": 3001
}
```

说明：

- 评论成功后增加 `post_stat.comment_count`。
- 评论别人的帖子时发送评论事件，后续由 RocketMQ 消费生成通知。

### 6.2 查询帖子评论列表

```http
GET /api/posts/{postId}/comments
```

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量 |

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
  "createdAt": "2026-05-25T21:00:00"
}
```

说明：第一版只做一级评论，按创建时间升序或降序均可，建议默认升序。

### 6.3 删除评论

```http
DELETE /api/comments/{commentId}
```

认证：需要登录，仅评论作者、帖子作者或管理员可操作。

响应 `data`：

```json
true
```

说明：第一版建议软删除，将 `comment.status` 更新为 `1`，并扣减 `post_stat.comment_count`。

### 6.4 获取当前用户评论列表

```http
GET /api/users/me/comments
```

认证：需要登录

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | integer | 否 | 1 | 页码 |
| size | integer | 否 | 10 | 每页数量 |

响应 `data.records[]`：

```json
{
  "id": 3001,
  "postId": 1001,
  "postTitle": "高数复习资料怎么整理？",
  "content": "我一般先整理错题，再刷历年卷。",
  "createdAt": "2026-05-25T21:00:00"
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

说明：

- 同一用户对同一帖子只能有一条 `post_like` 记录。
- 如果已取消点赞，再次点赞应将 `status` 改回 `0`。
- 点赞别人的帖子时发送点赞事件，后续由 RocketMQ 消费生成通知。

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
| size | integer | 否 | 10 | 每页数量 |
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
  "createdAt": "2026-05-25T21:10:00"
}
```

说明：只查询当前登录用户作为 `receiver_id` 的通知。

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

认证：需要登录

响应 `data`：

```json
true
```

说明：只能操作当前登录用户自己的通知。

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

第一版可以先不实现管理后台页面，但建议保留少量管理接口，方便展示权限控制。

### 9.1 隐藏帖子

```http
PUT /api/admin/posts/{postId}/hide
```

认证：需要管理员

响应 `data`：

```json
true
```

说明：将 `post.status` 更新为 `2`。

### 9.2 恢复帖子

```http
PUT /api/admin/posts/{postId}/restore
```

认证：需要管理员

响应 `data`：

```json
true
```

说明：将 `post.status` 更新为 `0`。

### 9.3 禁用用户

```http
PUT /api/admin/users/{userId}/disable
```

认证：需要管理员

响应 `data`：

```json
true
```

说明：将 `user.status` 更新为 `1`。

### 9.4 启用用户

```http
PUT /api/admin/users/{userId}/enable
```

认证：需要管理员

响应 `data`：

```json
true
```

说明：将 `user.status` 更新为 `0`。

## 10. 接口清单

| 模块 | 方法 | 路径 | 说明 | 是否登录 |
| --- | --- | --- | --- | --- |
| 认证 | POST | `/api/auth/register` | 用户注册 | 否 |
| 认证 | POST | `/api/auth/login` | 用户登录 | 否 |
| 认证 | POST | `/api/auth/logout` | 用户退出 | 是 |
| 用户 | GET | `/api/users/me` | 当前用户信息 | 是 |
| 用户 | PUT | `/api/users/me` | 修改当前用户资料 | 是 |
| 用户 | GET | `/api/users/{userId}` | 用户主页 | 否 |
| 分类 | GET | `/api/categories` | 分类列表 | 否 |
| 帖子 | POST | `/api/posts` | 发布帖子 | 是 |
| 帖子 | GET | `/api/posts` | 帖子列表 | 否 |
| 帖子 | GET | `/api/posts/{postId}` | 帖子详情 | 否 |
| 帖子 | PUT | `/api/posts/{postId}` | 编辑帖子 | 是 |
| 帖子 | DELETE | `/api/posts/{postId}` | 删除帖子 | 是 |
| 帖子 | GET | `/api/users/{userId}/posts` | 用户帖子列表 | 否 |
| 热榜 | GET | `/api/posts/hot` | 热门帖子榜 | 否 |
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

## 11. 后续实现建议

第一阶段先实现 MySQL 版本的同步接口，保证业务闭环。

第二阶段加入 Redis：

- 登录 token 或用户会话缓存
- 热门帖子榜缓存
- 帖子浏览量缓存
- 点赞状态缓存

第三阶段加入 RocketMQ：

- 评论事件 `CommentEvent`
- 点赞事件 `LikeEvent`
- 消费事件生成站内通知
- 异步更新帖子统计

第四阶段补充 JUC 和 JVM 亮点：

- 线程池批量刷新浏览量
- 热榜定时计算任务
- 压测后根据 GC 日志和 JVM 参数进行基础调优

