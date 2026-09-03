# 筑梦家 HouseDesign · 房屋装修效果可视化平台

上传房屋设计图，由 AI 生成对应装修风格的**写实效果图**与**可交互 3D 漫游场景**，并支持社区分享交流。让业主在施工前就能"身临其境"预览装修完成后的效果。

> 本项目为个人**全栈项目**：从零重建一套 Spring Boot 3 + Vue 3 + Three.js 的完整应用，用于沉淀全栈工程能力。
---

## 开发进度

> 代码按里程碑小步提交，每阶段可独立验证。进度随开发持续更新。

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 环境搭建 + 基线功能验证 | 完成 |
| P1 | 后端地基（工程骨架 / 统一响应 / 实体建表 / JWT / 配置） | 🔨 P1-1 骨架完成，P1-2 进行中 |
| P2 | 认证 + 文件上传模块 | 待开始 |
| P3 | 项目 CRUD + AI 生成异步链路（含 mock 降级） | 待开始 |
| P4 | Vue3 前端 + Three.js 3D 查看器 | 待开始 |
| P5 | 真实 AI 接入 + 社区模块 + 打包部署 | 待开始 |

---

## 技术栈

| 端 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Spring Boot 3.2.5 | Web 服务 / 内嵌 Tomcat |
| 后端 | Spring Data JPA (Hibernate) | ORM，`ddl-auto=update` 自动建表 |
| 后端 | Spring Security 风格 JWT（jjwt 0.11.5） | 登录鉴权 + 接口归属校验 |
| 后端 | Spring WebFlux (WebClient) | 异步调用外部 AI 服务 |
| 后端 | MySQL 8 / BCrypt | 数据存储 / 密码加密 |
| 前端 | Vue 3 + Vite 5 + Element Plus | SPA 界面 |
| 前端 | Pinia / Vue Router / Axios | 状态管理 / 路由 / 请求封装 |
| 前端 | Three.js 0.166 | 3D 场景构建与交互 |
| AI | 智谱 CogView-4（可插拔） | 户型图 → 写实效果图 |

---

## 核心功能

- **账号**：注册 / 登录 / 个人信息，JWT 鉴权 + 密码 BCrypt，支持改昵称与头像
- **设计项目**：创建 / 列表 / 详情 / 删除（一个项目 = 一张设计图 + 一种装修风格，图源支持户型图 / CAD 导出 / 手绘稿）
- **AI 生成**：一键生成 3D 效果，**异步任务 + 状态轮询**；结果含效果图与 3D 场景
- **3D 查看**：旋转 / 缩放 / 平移 / 自动环视；5 种装修风格一键切换
- **社区「装修小圈」**：发帖 / 点赞（唯一约束防重复）/ 评论（冗余计数器），仅可操作自己的数据
- **文件**：统一上传接口（头像、帖子图片）

## 架构亮点

- **AI 服务可插拔（策略 + 工厂）**：定义 `ImageTo3DService` 接口，`ImageTo3DServiceFactory` 按配置的 `provider` 选择实现——`mock`（内置程序化户型重建，免 Key 全链路可用）/ `zhipu`（智谱 CogView 文生图）/ `meshy` 等通用外部图生 3D。**未配置 API Key 时自动回退 mock**，保证"即开即用"。
- **生成任务异步状态机**：`PENDING → PROCESSING → SUCCEEDED / FAILED`，异步线程池处理 + 前端轮询状态。
- **资源归属校验**：所有项目 / 帖子 / 点赞按 `userId` 隔离，越权返回 403。
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

### 2. 后端（默认 :8080）

```bash
cd backend
mvn spring-boot:run
```

启动后自动建表；设计图与生成结果存放于 `backend/storage/`。

### 3. 前端（默认 :5173）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 （已配置 `/api`、`/files` 代理到 8080）

### 4. 接入真实 AI（可选）

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
│       ├── config/               # CORS / 异步 / 静态资源 / 属性绑定
│       ├── common/               # 统一响应 Result、全局异常、工具
│       ├── controller/ dto/ entity/ repository/
│       ├── security/             # JWT 签发校验、当前用户解析、BCrypt
│       └── service/              # 业务 + AI 可插拔服务层
└── frontend/                     # Vue3 前端
    └── src/
        ├── api/ router/ store/ three/
        ├── components/ views/
```

---

## 数据库设计（6 张表）

| 表 | 说明 | 关键设计 |
| --- | --- | --- |
| `t_user` | 用户 | BCrypt 密码 |
| `t_design_project` | 设计项目 | `user_id` 归属 |
| `t_generated_model` | AI 生成任务与结果 | 状态枚举 + `project_id` |
| `t_post` | 社区帖子 | `comment_count` 冗余计数器 |
| `t_post_comment` | 评论 | 扁平结构 |
| `t_post_like` | 点赞 | `UNIQUE(post_id, user_id)` 防重复赞 |

---

## License

MIT

---

*个人项目 作者：[6myname6](https://github.com/6myname6)*
