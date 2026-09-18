# 筑梦家 · 待办清单

## AI 多模态对话（2026-09-18，🚧 开发中）

> 需求：新增「AI 设计助手」对话能力，AI 具备图文理解（用户可发装修图片提问）。
> 技术方案：LangChain4j 1.0.1-beta6（`@AiService` 声明式）+ 智谱 **glm-4.6v-flash** 视觉模型（OpenAI 兼容协议），Key 走 `ZHIPU_API_KEY` 环境变量。

- [x] **模型接入层**：`pom.xml` 引入 langchain4j open-ai + spring boot starter；`AiConfig` 注册 `visionChatModel`；yml 配 `langchain4j.open-ai.chat-model`
- [x] **声明式接口**：`AiChatAssistant`（`@AiService` + 装修设计师 `@SystemMessage`），纯文本 / 文+图两个重载
- [x] **Service 分层**：`AiChatService` + `AiChatServiceImpl`，纯文本 / 图文 / 纯图（默认提问"请解释这张图"）三分支
- [x] **异常转译**：`RateLimitException / TimeoutException / InternalServerException / UnresolvedModelServerException` → `BusinessException(503, "AI 服务繁忙")`；401（Key 错/欠费）不吞，原样暴露
- [x] **请求 DTO**：`AiChatRequest { question, images(List<String> DataURL) }`
- [ ] **正式对话接口**：`AiChatController` —— `POST /api/ai/chat`（JSON：question + images DataURL 列表）；Controller 内解析 `data:{mime};base64,{数据}` 头 → `ImageContent.from(base64, mimeType)`；四分支（双空 400 / 纯文本 / 纯图 / 图文）。**注意：图片走 base64，localhost URL 智谱云端拉不到**
- [ ] **清理测试接口**：新接口 Swagger 实测通过后删除 `TestAiController`（GET /api/chat，`@RequestParam List<ImageContent>` 无法绑定，仅纯文本可用）
- [ ] **多轮记忆**：`ChatMemoryProvider`（会话 id → `MessageWindowChatMemory`，起步内存 Map，重启丢失可接受）+ `@AiService` 方法加 `@MemoryId String conversationId`；DTO 加 `conversationId`（首轮为空则生成）
- [ ] **前端聊天页**：`api/ai.js` + `views/AiChat.vue`（气泡 UI、图片选择/预览、`FileReader` 转 DataURL、loading/503 重试）+ 路由 + TabBar 入口

## 新增需求（2026-09-14，前端）

- [ ] **登录页背景轮播**（需求文档 A-8 / §10.1）
  - 登录/注册页背景 5 张装修效果图，`setInterval` 每 5 秒切换，淡入淡出
  - 素材：静态目录或复用生成结果 `panoramaUrl`
  - 位置：`frontend/src/views/Login.vue`、`Register.vue`
- [ ] **帖子详情页**（需求文档 C-12 / §10.2）
  - 前端：新增 `PostDetail.vue`（`/posts/:id`），从「我的发布」列表点击进入；复用小圈评论区交互（点赞/评论/回复）
  - 后端：需新增**单帖详情接口** `GET /api/posts/{id}`（返回 PostResponse，归属校验 + 404 防枚举）；评论列表复用 `GET /api/posts/{postId}/comments`
  - 位置：`frontend/src/views/Profile.vue`（我的发布入口）、`frontend/src/router/index.js`
- [ ] **左侧导航栏收起/展开**（需求文档 N-8 / §10.3）
  - 侧边栏头部折叠按钮；收起仅留图标、内容区变宽
  - 位置：`frontend/src/layout/TabBar.vue`、`frontend/src/App.vue`

## 社区模块（装修小圈）

- [x] **帖子点赞/取消点赞**（§5.4）
  - 位置：`PostController` + `PostService.togglePostLike(postId)`
  - 表 `t_post_like` 已建（`UNIQUE(post_id, user_id)` 防重），参照 `toggleCommentLike` 模式实现
- [x] **帖子评论**（§5.5，含评论列表 `GET /api/posts/{postId}/comments`）
  - 位置：`PostController` + `PostService.commentApost(postId, CommentRequest)`
  - `t_post_comment` 已支持（parent_id 置 NULL 即顶层评论）
- [x] **删除评论**（§5.6）
  - 位置：`CommentController` + `PostService.delComment(commentId)`
  - 仅作者可删，归属校验参照 `deletePost`

## 性能优化

- [ ] **帖子列表 N+1 查询优化**
  - 现状：`posts.stream().map(post -> toResponse(post, userMapper.selectById(post.getUserId())))`，每帖查一次作者 → N 帖 = N+1 次 SQL
  - 方案：先收集作者 ID 去重 → `userMapper.selectBatchIds(authorIds)` 一次查出 → `Collectors.toMap(User::getId, u -> u)` 内存分组 → map 时 `authorMap.get(post.getUserId())`
  - 效果：N+1 次 SQL 降到 2 次
  - 位置：`backend/src/main/java/com/housedesign/Service/impl/PostServiceImpl.java` 的 `postList`

## 生成链路（§4，已实现 ✅）

- [x] **GenerationServiceImpl**：`POST /api/projects/{projectId}/generate`，校验项目归属 + 有设计图 → 创建 PENDING 任务 → 线程池异步处理
- [x] **AI 直连智谱**（方案改为**不用 mock**）：`AIimageService`（WebClient 发起 `images/generations` + 轮询 `async-result/{task_id}`），不做 ImageTo3DService 抽象/工厂
- [x] **状态轮询接口**：`GET /api/generations/{id}`、`GET /api/projects/{projectId}/generations`、`GET /api/generations`
- [x] **AI 提示词组装**：按 §4.1 流程——`style_label` 反查枚举 prompt + `style` 自由文本拼装，结果落 `t_generated_model`
- [x] **图片持久化**：智谱临时 URL 下载落盘 `./storage/designs/`（`FileStorageService.downloadFromUrl`）

## AI 调用稳定性（2026-09-16，LangChain4j 接入后）

背景：对话已切 LangChain4j（`AiChatService` @AiService，智谱 OpenAI 兼容协议）。实测高峰期智谱返回 `429 code=1305「该模型当前访问量过大」`，默认被全局异常处理器兜底成 500。不稳定分四类：**限流 / 超时 / 上游故障 / 网络抖动**，按下面六层防线由易到难处理。

- [x] **第 1 层 超时控制**：`application.yml` 已配 `langchain4j.open-ai.chat-model.timeout: 60s`，防请求无限挂住占 Tomcat 线程
- [x] **第 2 层 自动重试（框架自带）**：LangChain4j 默认重试 3 次 + 指数退避（堆栈可见 `RetryUtils$RetryPolicy.withRetry`）；注意重试只对瞬时抖动有效，持续限流时重试只增加等待，次数不是越大越好
- [ ] **第 2 层（补）显式配置**：yml 加 `langchain4j.open-ai.chat-model.max-retries: 3`，把隐式默认值写明便于调参
- [ ] **第 3 层 异常转译（下一步先做这个）**：重试仍失败时，不能一律返回 500
  - 位置：学习期先在 `TestAiController.askAi` try-catch；正式业务改为 `AiChatService` 接口 + `AiChatServiceImpl` 分层（参照 LoginService 模式，@AiService 代理接口内加不了逻辑）
  - 映射：`dev.langchain4j.exception.RateLimitException`（429，`HttpException` 子类）→ `BusinessException(503, "AI 服务繁忙，请稍后再试")`；超时同理 503
  - **易错点**：不要 catch 所有 Exception 都转 503——`401 api-key 错误/欠费`是配置问题，应保留原错误暴露出来，不能伪装成"繁忙"；只转译限流(429)/超时/5xx 这几类瞬时错误
- [ ] **第 4 层 降级兜底**：对话失败时返回友好兜底文案而非报错；文生图已有更优范式——异步任务状态机把失败存 `errorMessage`，不阻塞主流程（已实现）
- [ ] **前端重试体验**：收到 503 时弹窗带「重试」按钮（`frontend/src/api/request.js` 响应拦截器目前只做消息提示）
- [ ] **第 5 层 多模型 fallback（上线前评估，勿提前过度设计）**：主模型持续失败自动切备用（智谱付费模型 / 另一 OpenAI 兼容厂商）；LangChain4j 无一行配置开关，自写包装类 try A catch(限流/超时) → B
- [ ] **第 6 层 结果缓存（性价比高，复用现有 Redis）**：装修风格知识类问题重复率高，相同问题缓存回答 TTL 1h，命中即返回不调模型，既提速又从根上减少限流；key 如 `ai:chat:{md5(systemPrompt+question)}`

## 3D 查看（前端，规划中）

- [ ] **前端 `Viewer3D.vue`**：按 `sceneConfig.type` 渲染 `photo-tour`（照片漫游）。~~`procedural-apartment`（Three.js 重建，原依赖 mock）~~——mock 方案已废弃，不再提供

## 安全加固

- [ ] **CORS 严格配置**：限定可信域名，不用 `*` + `allowCredentials`
- [ ] **上传文件头校验**：扩展名白名单已有，补 Magic Number 校验防伪装文件
- [x] **登录限流（Redis INCR+EXPIRE，对应需求 S-3 登录部分）**
  - 契约：接口文档 §2.2——同一 username 连续失败 5 次锁 10 分钟；前 4 次仍 400「用户名或密码错误」；第 5 次及锁定期 429，message 带剩余分钟
  - [x] 依赖/配置：`pom.xml` 引入 `spring-boot-starter-data-redis`；`application.yml` 配 `spring.data.redis.host/port`（环境变量占位，与 DB_PASSWORD 同风格）+ `app.login-limit.max-fail-count=5`、`lock-seconds=600`
  - [x] 限流组件 `common/LoginRateLimiter.java`（注入 `StringRedisTemplate`）：
    - 进入登录先 `GET login:fail:{username}`，计数 ≥ 5 → 用 `TTL` 取剩余秒数拼提示，抛 `BusinessException(429, ...)`，**不查库不校验密码**（锁定期不泄露用户是否存在）
    - 登录失败：`INCR`，仅返回值 = 1（首次失败）时 `EXPIRE 600`——固定窗口，后续失败不续期
    - 登录成功：`DEL` 清计数
  - [x] 改造 `LoginServiceImpl.login`：锁定判断 → 查用户/BCrypt 校验 → 失败时调限流记录（达阈值抛 429，未达则维持现状返回 null，Controller 照旧 400）→ 成功清计数
  - [ ] 验证：错密连试 5 次前 4 次 400、第 5 次 429；锁定期即使密码正确也 429；成功登录后计数清零；日志只打 username 不打密码（需本地起 Redis 实测）
  - 为什么用 Redis：高频短时计数（INCR 原子 + 自动过期），MySQL 无过期机制且写压力大；权衡：计数未持久化，Redis 重启即解锁（简历项目可接受，接口文档 §9 已注明）
  - 面试话术：知道什么时候用 Redis（登录限流）与不用（点赞低频数据用 MySQL 原子更新）
  - 后续（不在本期）：注册接口同 IP 限频（S-3 中 A-7 部分），key 用 `register:fail:{ip}`，可升级滑动窗口
  - 后续优化：**两段式 TTL**——当前固定窗口从第 1 次失败起算，若 5 次失败拖得分散（接近 600s），触发锁定时剩余锁定时间可能不足。改进：第 1 次失败 `EXPIRE 600`（计数窗口）；计数达阈值那一刻再 `EXPIRE 600` 一次（锁定窗口），保证锁定恒有完整 10 分钟。再进一步可升级滑动窗口（ZSET）或 Lua 脚本保证原子性
- [ ] **令牌失效机制**：改密/登出使旧 JWT 失效

## 体验与扩展

- [ ] **项目列表分页**（§3.2）
- [ ] **帖子搜索/筛选**（按内容/标签）
- [ ] **对象存储/CDN 替换**本地文件系统
