# 筑梦家 HouseDesign · 住宅装修效果可视化平台

上传房屋设计图，由 AI 生成对应装修风格的**写实效果图**与**可交互 3D 漫游场景**，并支持社区分享交流。让业主在施工前就能"身临其境"预览装修完成后的效果。

> 本项目为个人**全栈项目**：从零重建一套 Spring Boot 3 + Vue 3 + Three.js 的完整应用，用于沉淀全栈工程能力。
---
> 当前状态：**前后端可用**（认证/项目/社区/AI 生成 + Vue3 前端界面），AI 多模态设计助手开发中，3D 查看器与部分体验优化为规划中。详见下方开发进度。

## 开发进度

> 代码按里程碑小步提交，每阶段可独立验证。进度随开发持续更新。

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 环境搭建 + 基线功能验证 | 完成 |
| P1 | 后端地基（工程骨架 / 统一响应 / 实体建表 / JWT / 配置） | 完成 |
| P2 | 认证 + 文件上传模块（含 Redis 登录限流、手机验证码登录） | 完成 |
| P3 | 项目 CRUD + 社区模块（发帖/评论/点赞/楼中楼） | 完成 |
| P4 | AI 生成链路（直连智谱 CogView，异步 + 轮询 + 本地持久化） | 完成 |
| P5 | Vue3 前端（登录/项目/小圈/我的/详情+生成，网页端布局） | 进行中 |
| P5.1 | 杂志风 UI 重构（编辑杂志·建筑工作室设计体系，需求 N-9） | 进行中（基础层+导航/首页/登录标杆页已完成，6 页迁移中） |
| P5.5 | AI 设计助手（LangChain4j + glm-4.6v-flash，图文理解对话） | 进行中 |
| P6 | 社区完善 + 打包部署 | 待开始 |

## 新增需求规划

> 详见 [docs/需求文档.md §10](docs/需求文档.md) 与 [docs/TODO.md](docs/TODO.md)。

**退出登录（A-9，⬜ 规划中，2026-09-20）**

- [ ] `POST /api/auth/logout`（需鉴权）：当前 token 写入 **Redis 黑名单**实现服务端立即失效，不只是前端删 token——旧 token 在 7 天自然过期前被窃取也无法再用
- [ ] 黑名单键 `jwt:blacklist:{SHA-256(token)}`，TTL 取 token 剩余有效期，到期自动清理；`JwtInterceptor` 验签后增加黑名单校验，命中返回 401
- [ ] 前端「我的」页退出按钮调接口（失败也清本地态）→ 跳登录页；后续改密（A-5）复用同一机制。契约见[接口文档 §2.5](docs/接口文档.md)

**AI 多模态对话（2026-09-18，🚧 开发中）**

- [x] 后端模型接入层与 Service 层（LangChain4j `@AiService` + 智谱 glm-4.6v-flash 视觉模型，纯文本/图文/纯图三分支 + 503 异常转译）
- [ ] `POST /api/ai/chat` 正式接口（JSON：文本 + DataURL 图片列表）
- [ ] 多轮上下文记忆（ChatMemory + 会话 id，起步内存存储）
- [ ] 前端聊天页（图片选择/预览、气泡 UI、loading/重试）

**2026-09-14（前端体验）**

- [ ] **登录页背景轮播**：登录/注册页背景 5 张装修效果图，每 5 秒自动切换
- [ ] **帖子详情页**：从「我的发布」点击进入 `/posts/:id`，可见他人点赞/评论，本人可互动
- [ ] **左侧导航栏收起/展开**：折叠按钮，收起仅留图标、内容区变宽

---

## 技术栈

| 端 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Spring Boot 3.2.5 | Web 服务 / 内嵌 Tomcat |
| 后端 | MyBatis-Plus | ORM，`BaseMapper` 通用 CRUD + LambdaQueryWrapper + 分页插件 |
| 后端 | JWT（jjwt） | 登录鉴权 + 接口归属校验（拦截器 + ThreadLocal） |
| 后端 | springdoc-openapi | Swagger 接口文档（/swagger-ui.html + /v3/api-docs） |
| 后端 | MySQL 8 / BCrypt | 数据存储 / 密码加密 |
| 后端 | Redis 7 | 登录失败计数限流（INCR + EXPIRE，5 次锁 10 分钟，见接口文档 §2.2 / 需求 S-3）；手机验证码（`sms:code` 5 分钟 + `sms:limit` 60 秒，见接口文档 §2.6/§2.7 / 需求 A-10）；退出登录 token 黑名单（规划中，见接口文档 §2.5 / 需求 A-9） |
| 后端 | Spring WebClient | 异步调用外部 AI（智谱 CogView 文生图） |
| 后端 | LangChain4j 1.0.1-beta6 | 声明式 AI Service 接入层（OpenAI 兼容协议），多模态对话 |
| 前端 | Vue 3 + Vite 5 + Element Plus（待开发） | SPA 界面 |
| 前端 | Pinia / Vue Router / Axios（待开发） | 状态管理 / 路由 / 请求封装 |
| 前端 | SCSS 设计令牌 + Element Plus 主题覆盖 | 「编辑杂志 · 建筑工作室」设计体系（需求 N-9）：纸感米白 / 墨黑 / 克制陶土、衬线展示 + 等宽图注、细发线分层；基础层与导航/首页/登录标杆页已完成，其余页面迁移中（见 docs/TODO.md） |
| 前端 | Three.js（待开发） | 3D 场景构建与交互 |
| AI | 智谱 CogView-4 | 设计图 → 写实效果图（直连，异步任务模式） |
| AI | 智谱 glm-4.6v-flash | 多模态对话（图文理解，装修设计助手；OpenAI 兼容协议接入） |

---

## 核心功能

- **账号**：注册 / 登录 / 手机验证码登录 / 个人信息，JWT 鉴权 + 密码 BCrypt，支持改昵称与头像；登录失败限流（Redis，连续失败 5 次锁 10 分钟）；手机验证码登录（Redis 存验证码：5 分钟有效、60 秒发送间隔、一次性，未注册手机号自动建档，本期短信为 Mock 打日志）
- **风格**：内置 5 种装修风格（现代简约 / 奶油轻法式 / 意式轻奢 / 新中式 / 原木风），`DesignStyle` 枚举为单一数据源，`GET /api/styles` 提供风格列表
- **设计项目**：创建（含设计图上传，支持「用户自定义风格要求 + 预设风格标签」双输入）/ 列表 / 详情 / 删除，全部仅限本人操作（归属校验 + 404 防枚举探测）
- **装修小圈**：发帖（图文）/ 分页列表 / 我的帖子 / 删帖；**帖子点赞 / 评论 / 删除评论**；**评论点赞**（独立于帖子点赞）；**楼中楼回复**（parent_id 两级）
- **AI 生成**：一键生成装修效果图（直连智谱 CogView），异步任务 + 状态轮询（PENDING→PROCESSING→SUCCESS/FAILED）；生成图持久化到所选存储后端（local 磁盘或 OSS），`panoramaUrl` 可长期访问
- **AI 设计助手**（🚧 开发中）：基于 LangChain4j + glm-4.6v-flash 的对话能力，支持纯文本 / 图片理解 / 图文混合提问（装修风格识别、建材建议等）；系统角色限定为资深装修设计师；模型限流/超时统一转 503 友好提示。接口契约见接口文档 §11
- **3D 查看**（规划中）：photo-tour 照片漫游渲染，前端 `Viewer3D.vue` 待开发
- **文件**：统一上传接口（头像、帖子图片、设计图），扩展名白名单 + UUID 重命名；存储后端 Local 磁盘 / 阿里云 OSS 配置开关一键切换（默认 local）
- **接口文档**：Swagger UI 在线浏览，`/v3/api-docs` JSON 可导入 Apifox/Postman

## 架构亮点

- **JWT 鉴权 + ThreadLocal 用户上下文**：`JwtInterceptor` 校验 token 并写入 `UserContext`，`afterCompletion` 清理防止线程复用串号；资源归属一律从 `UserContext.getUserId()` 获取，杜绝前端伪造。
- **全局异常处理器**：`BusinessException`（携带业务码）+ `@RestControllerAdvice` 统一把异常转 `Result`，404/400/500 语义明确，避免 500 错误页泄露堆栈。
- **风格枚举单一数据源**：`DesignStyle` 一个枚举同时承担入参校验（code 反查）、响应展示（label）、AI 提示词（prompt）三重职责；`@JsonFormat(OBJECT)` 序列化 + `@JsonIgnore` 屏蔽 prompt，防提示词泄露与注入。
- **文件存储抽象 + 双实现开关**：`FileStorageService` 接口 + 模板方法基类 `AbstractFileStorageService`（校验/UUID 命名/MIME 映射/远程下载），Local 磁盘与阿里云 OSS 两套实现按 `app.storage.type=local|oss` 条件装配（`@ConditionalOnProperty`），业务层与前端零感知；OSS 客户端单例管理连接池（启动 fail-fast 校验密钥、关闭时 shutdown），显式设置 Content-Type（防图片被当附件下载）与一年缓存头。
- **点赞防重与级联清理**：帖子/评论点赞表均以 `UNIQUE(目标id, user_id)` 防重复；删帖用 `@Transactional` 级联删除评论与两种点赞。
- **Controller 薄 / Service 厚**：Controller 只收参数、调服务、包 `Result`；校验、归属判断、事务全在 Service 层，职责清晰。
- **声明式 + 原生双路 AI 接入（LangChain4j）**：纯文本走 `@AiService` + `@SystemMessage` 角色设定；图文链路因 1.0.1 版 AiService 不支持图片参数，直连自动装配的 `ChatModel` 手工组装 `TextContent + ImageContent` 多模态消息；Service 层统一异常转译（429/超时/5xx → 503，401 配置错误不吞）。
- **安全配置零落地**：数据库密码、JWT 密钥、AI Key 全部经环境变量注入（`${VAR:default}`），不硬编码。

---

## 快速启动

### 环境要求

- JDK 17+ / Maven 3.8+ / MySQL 8.x（运行中）/ Node.js 18+ / Redis 7.x（登录限流与短信验证码已启用，**后端启动前必须运行**）

### 1. 数据库

无需手工建库——JDBC URL 已带 `createDatabaseIfNotExist=true`，首次启动自动创建 `house_design` 库。按本机情况注入密码：

```bash
# Windows (CMD)
set DB_PASSWORD=你的MySQL密码
# Git Bash / macOS / Linux
export DB_PASSWORD=你的MySQL密码
```

> ⚠️ 表结构变更后（如新增列），`CREATE TABLE IF NOT EXISTS` 不会修改已存在的表，需手动执行 `docs/数据库设计.md` 中的 ALTER 迁移语句。

### Redis（登录限流 + 短信验证码已启用，必需；登出黑名单规划中）

登录限流（需求 S-3 登录部分）与手机验证码（需求 A-10）已实现：登录/发码接口强依赖 Redis，未启动会导致报错。退出登录的 token 黑名单（需求 A-9，规划中，见接口文档 §2.5）将复用同一 Redis 实例，无需额外部署。Windows 推荐 Docker：

```bash
docker run -d --name house-redis -p 6379:6379 redis:7
```

连接配置走环境变量占位（`spring.data.redis.host/port`，`REDIS_PASSWORD` 可选）；限流键设计见 [docs/数据库设计.md](docs/数据库设计.md) 末尾「Redis 键设计」。

### 2. 后端（默认 :8080）

```bash
cd backend
mvn spring-boot:run
```

启动后需先执行 `backend/src/main/resources/db/house_design.sql` 建表；设计图与生成结果存放于 `backend/storage/`。接口文档：http://localhost:8080/swagger-ui.html

### 3. 前端（默认 :5173，待开发）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 （已配置 `/api`、`/files` 代理到 8080）

### 4. 配置 AI Key（AI 生成与 AI 对话共用，直连智谱）

文生图（CogView）与多模态对话（glm-4.6v-flash）直连智谱，共用同一个 API Key（环境变量注入，不落代码）：

```bash
# Windows (CMD)
set ZHIPU_API_KEY=你的Key
# PowerShell
$env:ZHIPU_API_KEY="你的Key"
# Git Bash / macOS / Linux
export ZHIPU_API_KEY=你的Key
```

> 未配置 `ZHIPU_API_KEY` 时，发起生成任务会进入 FAILED（`errorMessage` 提示配置 Key），AI 对话接口返回模型侧鉴权错误。智谱开放平台（bigmodel.cn）注册即可生成 Key，新用户含免费额度。

### 5. 存储后端切换（默认本地磁盘，可选阿里云 OSS）

由 `app.storage.type` 控制，默认 `local`（什么都不配即用）；切 OSS 需在阿里云创建**公共读** bucket，并注入三个环境变量后启动：

```powershell
# PowerShell（当前窗口临时生效）
$env:STORAGE_TYPE="oss"
$env:OSS_ACCESS_KEY_ID="你的AK"
$env:OSS_ACCESS_KEY_SECRET="你的SK"
```

bucket 名、地域 endpoint、公网域名在 `application.yml` 的 `app.storage.oss` 下按实际修改；密钥仅从环境变量读取。切换后上传接口返回 `https://{bucket}.{endpoint}/{dir}/{uuid}.ext`，接口契约与前端调用不变。

---

## 目录结构

```
HouseDesign/
├── backend/                      # Spring Boot 后端
│   └── src/main/java/com/housedesign/
│       ├── config/               # WebConfig / MybatisPlusConfig（分页）/ OpenApiConfig（Swagger）/ AsyncConfig（生成线程池）/ AiConfig（ChatModel）
│       ├── common/               # Result（统一响应）、BusinessException、GlobalExceptionHandler、UserContext、LoginRateLimiter
│       ├── controller/           # Controller（薄：收参 / 调服务 / 包 Result）
│       ├── dto/                  # request / response DTO（含 PageResult 分页通用响应）
│       ├── entity/               # MyBatis-Plus 实体（User / DesignProject / GeneratedModel / Post* / ...）
│       ├── interceptor/          # JwtInterceptor（token 校验 + 用户上下文）
│       ├── mapper/               # MyBatis-Plus BaseMapper 接口
│       ├── Service/              # 业务接口 + impl 实现
│       │   └── AI/               # AiChatAssistant（@AiService 声明式）+ AiChatService 多模态对话
│       └── util/                 # JwtUtil 等工具
│   └── src/main/resources/
│       ├── application.yml       # 配置（环境变量注入）
│       └── db/house_design.sql   # 建表脚本
├── docs/                         # 需求 / 产品 / 接口 / 数据库 / 待办清单
└── frontend/                     # Vue3 前端（待开发）
```

---

## 数据库设计（7 张表）

| 表 | 说明 | 关键设计 |
| --- | --- | --- |
| `t_user` | 用户 | BCrypt 密码 |
| `t_design_project` | 设计项目 | `user_id` 归属；`style`（自定义要求）+ `style_label`（预设 code）双字段 |
| `t_generated_model` | AI 生成任务与结果 | 状态枚举 + `project_id`；`scene_config`（photo-tour） |
| `t_post` | 社区帖子 | `comment_count` 冗余计数器 |
| `t_post_comment` | 评论 | 两级楼中楼（`parent_id`）+ 图文评论（`images` JSON） |
| `t_post_like` | 帖子点赞 | `UNIQUE(post_id, user_id)` 防重复赞 |
| `t_comment_like` | 评论点赞 | `UNIQUE(comment_id, user_id)`，独立于帖子点赞 |

详见 [docs/数据库设计.md](docs/数据库设计.md) 与 [docs/接口文档.md](docs/接口文档.md)。

---

## License

MIT

---

*个人项目 作者：[6myname6](https://github.com/6myname6)*
