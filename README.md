# 筑梦家 HouseDesign · 房屋装修效果可视化平台

上传房屋设计图，由 AI 生成对应装修风格的**写实效果图**与**可交互 3D 漫游场景**，并支持社区分享交流。让业主在施工前就能"身临其境"预览装修完成后的效果。

> 本项目为个人**全栈项目**：从零重建一套 Spring Boot 3 + Vue 3 + Three.js 的完整应用，用于沉淀全栈工程能力。
---
> 当前状态：**后端为主**（认证/项目/社区已可用），前端与 AI 生成为规划中。详见下方开发进度。

## 开发进度

> 代码按里程碑小步提交，每阶段可独立验证。进度随开发持续更新。

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 环境搭建 + 基线功能验证 | 完成 |
| P1 | 后端地基（工程骨架 / 统一响应 / 实体建表 / JWT / 配置） | 完成 |
| P2 | 认证 + 文件上传模块 | 完成 |
| P3 | 项目 CRUD + 社区模块（发帖/评论/点赞） | 🔨 进行中（项目 CRUD 已完成，社区帖子点赞/评论待补，AI 生成待做） |
| P4 | Vue3 前端 + Three.js 3D 查看器 | 待开始 |
| P5 | 真实 AI 接入 + 社区完善 + 打包部署 | 待开始 |

---

## 技术栈

| 端 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Spring Boot 3.2.5 | Web 服务 / 内嵌 Tomcat |
| 后端 | MyBatis-Plus | ORM，`BaseMapper` 通用 CRUD + LambdaQueryWrapper + 分页插件 |
| 后端 | JWT（jjwt） | 登录鉴权 + 接口归属校验（拦截器 + ThreadLocal） |
| 后端 | springdoc-openapi | Swagger 接口文档（/swagger-ui.html + /v3/api-docs） |
| 后端 | MySQL 8 / BCrypt | 数据存储 / 密码加密 |
| 后端 | Spring WebClient（规划中） | 异步调用外部 AI 服务 |
| 前端 | Vue 3 + Vite 5 + Element Plus（待开发） | SPA 界面 |
| 前端 | Pinia / Vue Router / Axios（待开发） | 状态管理 / 路由 / 请求封装 |
| 前端 | Three.js（待开发） | 3D 场景构建与交互 |
| AI | 智谱 CogView-4（可插拔，规划中） | 户型图 → 写实效果图 |

---

## 核心功能

- **账号**：注册 / 登录 / 个人信息，JWT 鉴权 + 密码 BCrypt，支持改昵称与头像
- **风格**：内置 5 种装修风格（现代简约 / 奶油轻法式 / 意式轻奢 / 新中式 / 原木风），`DesignStyle` 枚举为单一数据源，`GET /api/styles` 提供风格列表
- **设计项目**：创建（含设计图上传，支持「用户自定义风格要求 + 预设风格标签」双输入）/ 列表 / 详情 / 删除，全部仅限本人操作（归属校验 + 404 防枚举探测）
- **装修小圈**：发帖（图文）/ 分页列表 / 我的帖子 / 删帖；**评论点赞**（独立于帖子点赞）；**楼中楼回复**（parent_id 两级）
- **AI 生成**（规划中）：一键生成 3D 效果，异步任务 + 状态轮询；结果含效果图与 3D 场景
- **3D 查看**（规划中）：旋转 / 缩放 / 平移 / 自动环视；一键切换风格
- **文件**：统一上传接口（头像、帖子图片、设计图），扩展名白名单 + UUID 重命名
- **接口文档**：Swagger UI 在线浏览，`/v3/api-docs` JSON 可导入 Apifox/Postman

## 架构亮点

- **JWT 鉴权 + ThreadLocal 用户上下文**：`JwtInterceptor` 校验 token 并写入 `UserContext`，`afterCompletion` 清理防止线程复用串号；资源归属一律从 `UserContext.getUserId()` 获取，杜绝前端伪造。
- **全局异常处理器**：`BusinessException`（携带业务码）+ `@RestControllerAdvice` 统一把异常转 `Result`，404/400/500 语义明确，避免 500 错误页泄露堆栈。
- **风格枚举单一数据源**：`DesignStyle` 一个枚举同时承担入参校验（code 反查）、响应展示（label）、AI 提示词（prompt）三重职责；`@JsonFormat(OBJECT)` 序列化 + `@JsonIgnore` 屏蔽 prompt，防提示词泄露与注入。
- **文件存储抽象**：`FileStorageService` 接口 + `LocalFileStorageServiceImpl` 磁盘实现，业务层只管传 URL；未来换 OSS 只换实现类，业务零改动。
- **点赞防重与级联清理**：帖子/评论点赞表均以 `UNIQUE(目标id, user_id)` 防重复；删帖用 `@Transactional` 级联删除评论与两种点赞。
- **Controller 薄 / Service 厚**：Controller 只收参数、调服务、包 `Result`；校验、归属判断、事务全在 Service 层，职责清晰。
- **安全配置零落地**：数据库密码、JWT 密钥、AI Key 全部经环境变量注入（`${VAR:default}`），不硬编码。

---

## 快速启动

### 环境要求

- JDK 17+ / Maven 3.8+ / MySQL 8.x（运行中）/ Node.js 18+

### 1. 数据库

无需手工建库——JDBC URL 已带 `createDatabaseIfNotExist=true`，首次启动自动创建 `house_design` 库。按本机情况注入密码：

```bash
# Windows (CMD)
set DB_PASSWORD=你的MySQL密码
# Git Bash / macOS / Linux
export DB_PASSWORD=你的MySQL密码
```

> ⚠️ 表结构变更后（如新增列），`CREATE TABLE IF NOT EXISTS` 不会修改已存在的表，需手动执行 `docs/数据库设计.md` 中的 ALTER 迁移语句。

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

### 4. 接入真实 AI（可选，规划中）

默认 `provider: mock`，零配置即可体验全流程。接智谱真实文生图：

```bash
export ZHIPU_API_KEY=你的Key
```

> 无 Key 时自动回退 mock，链路不受影响。

---

## 目录结构

```
HouseDesign/
├── backend/                      # Spring Boot 后端
│   └── src/main/java/com/housedesign/
│       ├── config/               # WebConfig / MybatisPlusConfig（分页）/ OpenApiConfig（Swagger）
│       ├── common/               # Result（统一响应）、BusinessException、GlobalExceptionHandler、UserContext
│       ├── controller/           # Controller（薄：收参 / 调服务 / 包 Result）
│       ├── dto/                  # request / response DTO（含 PageResult 分页通用响应）
│       ├── entity/               # MyBatis-Plus 实体（User / DesignProject / Post* / CommentLike / ...）
│       ├── interceptor/          # JwtInterceptor（token 校验 + 用户上下文）
│       ├── mapper/               # MyBatis-Plus BaseMapper 接口
│       ├── Service/              # 业务接口 + impl 实现
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
| `t_generated_model` | AI 生成任务与结果 | 状态枚举 + `project_id`（规划中） |
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
