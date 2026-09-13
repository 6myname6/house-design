# 筑梦家 · 待办清单

## 社区模块（装修小圈）

- [ ] **帖子点赞/取消点赞**（§5.4）
  - 位置：`PostController` + `PostService.togglePostLike(postId)`
  - 表 `t_post_like` 已建（`UNIQUE(post_id, user_id)` 防重），参照 `toggleCommentLike` 模式实现
- [ ] **帖子评论**（§5.5）
  - 位置：`PostController` + `PostService.commentPost(postId, CommentRequest)`
  - `t_post_comment` 已支持（parent_id 置 NULL 即顶层评论）
- [ ] **删除评论**（§5.6）
  - 位置：`CommentController` + `PostService.deleteComment(commentId)`
  - 仅作者可删，归属校验参照 `deletePost`

## 性能优化

- [ ] **帖子列表 N+1 查询优化**
  - 现状：`posts.stream().map(post -> toResponse(post, userMapper.selectById(post.getUserId())))`，每帖查一次作者 → N 帖 = N+1 次 SQL
  - 方案：先收集作者 ID 去重 → `userMapper.selectBatchIds(authorIds)` 一次查出 → `Collectors.toMap(User::getId, u -> u)` 内存分组 → map 时 `authorMap.get(post.getUserId())`
  - 效果：N+1 次 SQL 降到 2 次
  - 位置：`backend/src/main/java/com/housedesign/Service/impl/PostServiceImpl.java` 的 `postList`

## 生成链路（§4，已实现 ✅）

- [x] **GenerationServiceImpl**：`POST /api/projects/{projectId}/generate`，校验项目归属 + 有设计图 → 创建 PENDING 任务 → 线程池异步处理
- [x] **AI 直连智谱**（方案改为**不用 mock**）：`ZhipuImageService`（WebClient 发起 `images/generations` + 轮询 `async-result/{task_id}`），不做 ImageTo3DService 抽象/工厂
- [x] **状态轮询接口**：`GET /api/generations/{id}`、`GET /api/projects/{projectId}/generations`、`GET /api/generations`
- [x] **AI 提示词组装**：按 §4.1 流程——`style_label` 反查枚举 prompt + `style` 自由文本拼装，结果落 `t_generated_model`
- [x] **图片持久化**：智谱临时 URL 下载落盘 `./storage/designs/`（`FileStorageService.downloadFromUrl`）

## 3D 查看（前端，规划中）

- [ ] **前端 `Viewer3D.vue`**：按 `sceneConfig.type` 渲染 `photo-tour`（照片漫游）。~~`procedural-apartment`（Three.js 重建，原依赖 mock）~~——mock 方案已废弃，不再提供

## 安全加固

- [ ] **CORS 严格配置**：限定可信域名，不用 `*` + `allowCredentials`
- [ ] **上传文件头校验**：扩展名白名单已有，补 Magic Number 校验防伪装文件
- [ ] **登录限流（Redis INCR+EXPIRE，体现 Redis 技术栈）**
  - 需求：对应需求文档 S-3，防暴力破解与批量登录
  - 实现：登录失败 `INCR login:fail:{username}` + `EXPIRE`，失败次数 ≥ 5 次锁 10 分钟；成功登录 `DEL` 清计数
  - 为什么用 Redis：高频短时计数（INCR 原子 + 自动过期），MySQL 无过期机制且写压力大
  - 面试话术：知道什么时候用 Redis（登录限流）与不用（点赞低频数据用 MySQL 原子更新）
  - 依赖：`spring-boot-starter-data-redis` + 本地 Redis
- [ ] **令牌失效机制**：改密/登出使旧 JWT 失效

## 体验与扩展

- [ ] **项目列表分页**（§3.2）
- [ ] **帖子搜索/筛选**（按内容/标签）
- [ ] **对象存储/CDN 替换**本地文件系统
