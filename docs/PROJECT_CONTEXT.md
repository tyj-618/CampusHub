# CampusHub 项目上下文

本文档用于承接原 Forum 对话上下文，避免路径变更后任务目标丢失。

## 路径确认

当前项目工作目录：

```text
D:\GitCode\CampusHub
```

旧对话中曾使用过以下目录：

```text
D:\GitCode\forum
D:\GitCode\CompusHub
```

后续开发以 `D:\GitCode\CampusHub` 为准。

## 项目定位

CampusHub 是一个面向大学生的校园生活交流论坛系统，围绕课程交流、校园生活、二手闲置、失物招领、活动组队、求助问答等场景，提供发帖、评论、点赞、热门排行和站内通知能力。

项目目标是作为后端实习简历项目：功能不做大而全，但要完整、清晰，并能自然体现 Java、Spring、MySQL、Redis、JUC、JVM、RocketMQ 等技术栈。

## 标准开发流程

1. 需求边界确认
2. 数据库表设计
3. 接口设计
4. 技术架构设计
5. 项目初始化
6. 核心功能开发
7. Redis / RocketMQ / JUC 能力接入
8. 测试与压测
9. 简历描述与项目文档整理

当前进度：已完成需求边界和数据库脚本，正在整理 REST API 接口文档。

## 第一版功能边界

必须完成：

- 用户模块：注册、登录、退出、查看个人信息、修改昵称/头像/简介。
- 分类模块：固定校园分类，第一版不做复杂后台分类管理。
- 帖子模块：发布、编辑、删除、详情、分页浏览、分类筛选、按时间或热度排序。
- 评论模块：一级评论、删除自己的评论、查看帖子评论列表。
- 点赞模块：帖子点赞、取消点赞、统计点赞数。
- 热门榜模块：根据浏览量、点赞数、评论数计算热度。
- 通知模块：评论和点赞触发站内通知，支持查看和标记已读。

暂不纳入第一版：

- 楼中楼评论
- 评论点赞
- 收藏
- 关注
- 私信
- 图片上传
- Elasticsearch 搜索
- 复杂后台管理

## 数据库设计

第一版核心表共 7 张：

```text
user        用户表
category    帖子分类表
post        帖子表
post_stat   帖子统计表
comment     评论表
post_like   帖子点赞表
notice      站内通知表
```

SQL 脚本路径：

```text
src/main/resources/db/schema.sql
src/main/resources/db/data.sql
```

初始分类：

```text
course      课程交流
life        校园生活
market      二手闲置
lost_found  失物招领
activity    活动组队
help        求助问答
```

