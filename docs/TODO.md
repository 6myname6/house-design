# 筑梦家 · 待办清单

## 杂志风 UI 重构（编辑杂志 · 建筑工作室，🚧 进行中）

> 目标：全站统一为「编辑杂志 / 建筑工作室」视觉语言——纸感米白底、墨黑文字、克制陶土点睛、宋体展示标题、等宽图纸标注、细发线分层、3-4px 收小圆角、去厚重阴影。纯视觉重构，**不改任何脚本逻辑与接口契约**。

### 设计原则（所有页面共同遵守）

1. **令牌单一数据源**：颜色 / 字号 / 间距 / 圆角 / 时长一律引用 `styles/tokens.scss` 的 `--hd-*` 变量，禁止在页面内写死 hex（含 `#fff`）；Element Plus 组件靠 `styles/element.scss` 全局覆盖，页面不重复造样式。
2. **字体三分工**：页头/卡片标题用衬线 `--hd-font-display`；编号 / 时间 / 小标签用等宽 `--hd-font-mono`（配合 `.hd-overline`：大字距 + 大写）；正文默认无衬线。
3. **分层靠发线不靠阴影**：卡片用纸白 `--el-fill-color-blank`（`#fffdf9`）底 + `--hd-hairline` 细发线；除弹层（EP 全局已配轻阴影）外不使用 box-shadow；圆角只用 `--hd-radius-base`(3px) / `--hd-radius-lg`(4px)。
4. **页面骨架统一**（参照 Home 标杆）：`.page` 容器 → `.page-head`（overline 小标签 + 衬线标题 + 墨色主按钮）→ 内容；列表条目用 `.entry / .entry-no / .entry-meta` 杂志条目范式；空/错/加载态用 `.state-box / .ink-btn / .text-btn`。
5. **动效克制**：入场用 `.hd-rise` 错峰（inline `animation-delay`），标题装饰用 `hd-line`；全局已尊重 `prefers-reduced-motion`，不另加花哨动画。
6. **反白文字令牌化**：陶土/墨色实底上的文字不再写 `#fff`；动手做第一页时先在 tokens.scss 基础层补 `--hd-on-primary`（纸白反白字）、`--hd-on-ink`（Login 舞台现用的 `#f3ede2` 提炼为变量），再替换全部 `color: #fff`。

### 进度

- [x] **基础层**（commit b455597）：`styles/tokens.scss`（色板/字体/字号/间距/圆角/布局/杂志专用变量）、`styles/index.css`（纸底正文、衬线标题、`.hd-overline`/`.hd-hairline`/`.hd-rise`、纸调滚动条、选中文本色、reduced-motion）、`styles/element.scss`（EP 陶土主色、去圆角去阴影、输入框/按钮/对话框/骨架/弹层覆盖）
- [x] **标杆页**（commit b455597）：`layout/TabBar.vue`（编号式侧边导航 01/02…）、`views/Home.vue`（页头 + 杂志条目列表 + 状态页范式）、`views/Login.vue`（左墨色宣言舞台 + 右纸色表单双栏，860px 断点隐藏舞台）
- [x] **Settings.vue**：此前已是令牌化写法（28 行占位页），无需改造
- [x] **基础层增量**：tokens.scss 补 `--hd-on-primary`（陶土底反白字 `#fffdf9`）/ `--hd-on-ink`（墨底暖纸白 `#f3ede2`）；Login 舞台原有 `#f3ede2` 与各档 rgba 透明度已归一为令牌（透明度变体用 `color-mix`，Chromium 实测支持）

### 待重构页面（建议顺序，每页一个提交、浏览器实测后再下一页）

- [x] **1. Register.vue**：已对齐 Login 双栏范式（2026-09-21）——左侧墨色宣言舞台（Join the Studio / 编号 02 / 注册语境文案），右侧纸色表单；复用 `hd-overline/hd-rise/hd-line` 与 860px 断点；底部「已有账号？前往登录 →」。脚本（字段/校验规则/`store.register` 自动登录跳转）逐字未动，浏览器实测：必填/长度/两次密码不一致校验均正常拦截，宽屏双栏与窄屏单栏均无控制台报错。
- [ ] **2. ProjectDetail.vue（426 行）**：按「单篇杂志文章」重构——返回链接用 `.text-btn`；衬线大标题 + 等宽元信息（风格/创建时间/编号 NO.）；信息区与生成记录卡片 `#fff` 底（285 行）→ 纸白 + 细发线；生成状态（PENDING/PROCESSING/…）用等宽 overline 小标签；生成记录列表向 `.entry` 条目范式靠拢。
- [ ] **3. Profile.vue（361 行）**：个人头部杂志化——衬线大昵称、等宽编号、统计数字 `tabular-nums`、头像加细发线框；3 处 `background: #fff`（227/271/311 行）数据卡 → 纸白 + 发线；2 处 `color: #fff`（233/334 行）→ `--hd-on-primary`；退出按钮保留 danger 语义色（`--hd-danger`，EP 已覆盖）；编辑弹窗样式随全局覆盖，删局部硬编码。
- [ ] **4. CreateProject.vue（362 行）**：**5 个风格封面硬编码高饱和渐变（131-135 行 `#E3E0D9/#FAE0D3/…`）→ 纸调低饱和渐变**（用 neutral/primary 令牌派生，保持 5 种风格可辨但不刺眼）；选中态 `box-shadow: 0 0 0 2px var(--hd-primary-50)`（305 行）→ 陶土细描边 1px + 浅陶土底，不用阴影；上传拖拽区、表单卡片纸白化去阴影；主按钮统一墨色/陶土规范。
- [ ] **5. Community.vue（860 行，最大）**：帖子卡片 `background:#fff`（558/786 行）→ 纸白卡 + 细发线；陶土/墨底上的 `color:#fff`（570/666/764 行）→ `--hd-on-primary`；作者名衬线、时间走等宽图注；发帖框/空状态对齐 `.state-box`；点赞/评论图标按钮保持 3px 圆角方形。**注意：该文件含未提交的点赞防连点修复（`_liking` 标志、乐观更新回滚、`@error` 坏图处理），只动模板 class 与 `<style>`，脚本逻辑一行不碰，提交时与重构分离**。
- [ ] **6. AiChat.vue（605 行，硬编码最多）**：AI 气泡 3 处 `#fff` 底（260/330/365/516 行）→ 纸白卡 + 细发线；用户气泡/发送按钮/重试按钮的 `color:#fff`（361/395/464/494/562 行）→ `--hd-on-primary`；loading spinner `border-top-color:#fff`（576 行）→ `--hd-on-ink`；圆形头像（多处 `border-radius:50%`）改 3px 圆角方形加细发线框（建筑工作室气质，气泡三角可保留或去除）；输入区、图片预览卡、markdown 正文排版对齐杂志正文（小标题衬线、引用加发线）。
- [ ] **收尾**：全站视图目录 grep 复查无硬编码 hex / 无页面级 box-shadow；TabBar 导航在各页高亮正常；窄屏（<860px）逐页检查。

### 验收标准

1. `frontend/src/views/**/*.vue` 与 `layout/**/*.vue` 中无硬编码颜色 hex、无自定义大圆角、无页面级阴影（50% 圆形元素除外）。
2. 6 个页面与 Login/Home/TabBar 三个标杆页视觉同源：纸底、发线、衬线标题、等宽标注、陶土克制点睛。
3. 纯样式/模板 class 变更：所有交互（注册登录、发帖点赞防连点、AI 发送与重试、生成轮询、资料编辑）行为零变化。
4. 每改完一页在浏览器桌面宽屏 + 窄屏两档实测，不积攒到最后统一验收。

## 手机验证码登录（2026-09-21，✅ 已完成 2026-09-22）

> 需求：A-10（需求文档 §10.5）——手机号 + 短信验证码登录；验证码 Redis 存储（`sms:code:{phone}` TTL 300s、`sms:limit:{phone}` TTL 60s）；手机号未注册自动建档（`username`=手机号、`password`=NULL），签发 JWT。契约：接口文档 §2.6/§2.7。
> 本期短信通道为 Mock（验证码打印后端日志），`SmsService` 接口预留真实通道替换。
> 写码方式：**后端由用户亲手实现，助手给思路/关键片段并审查纠错；前端由助手直接实现**。

### 后端（学习模式）

- [x] 1. **schema 变更**：`db/house_design.sql` 建表语句加 `phone VARCHAR(11)` + `UNIQUE KEY uk_phone`，`password` 放宽 NULL；存量库已执行 ALTER
- [x] 2. **User 实体**加 `phone` 字段
- [x] 3. **短信通道**：`SmsService` 接口（`send(phone, code)`）+ `MockSmsServiceImpl`（`log.info` 打印验证码）
- [x] 4. **验证码组件** `common/SmsCodeService`（参照 `LoginRateLimiter` 注入 `StringRedisTemplate`）：
  - `sendCode(phone)`：频控检查（`sms:limit:` 存在则 429）→ 生成 6 位随机码 → 写 `sms:code:`（TTL 300s）→ 写 `sms:limit:`（TTL 60s）→ 调 `SmsService`
  - `verify(phone, code)`：`GET` 比对，不一致返回 false（保留 key），一致 `DEL` 后返回 true
- [x] 5. **请求 DTO**：`SmsCodeRequest {phone}`、`SmsLoginRequest {phone, code}`（`@NotBlank` + 手机号正则）
- [x] 6. **LoginService.loginByPhone(SmsLoginRequest)**：验码失败抛 400 → 按 phone 查用户 → 不存在则自动建档 → 签发 JWT
- [x] 7. **LoginController** 加 `POST /sms/code`、`POST /sms/login`
- [x] 8. **WebConfig** 放行两个新路径
- [x] 9. **配置项**：`app.sms.code-ttl-seconds=300`、`app.sms.resend-interval-seconds=60`
- [x] 10. **本地实测**（浏览器 + redis-cli 验证 key/TTL 全部通过）

### 前端（助手直接实现）

- [x] `api/auth.js`：`sendSmsCodeApi`、`smsLoginApi`
- [x] `stores/user.js`：`loginByPhone(phone, code)` action（成功后同样 fetchMe）
- [x] `views/Login.vue`：表单顶部「账号登录 / 手机登录」Tab 切换；手机表单 = 手机号输入 + 验证码输入与「获取验证码」按钮（60s 倒计时、倒计时中禁用）；全部使用 `--hd-*` 令牌

### 验收结果（需求文档 §10.5 六条验收标准，全部通过）

- [x] 正常发送/倒计时/60s 频控（后端 429 + 前端禁用）；正确验证码登录（含自动建档）；错误/过期/重放均 400；参数校验拦截；Tab 切换互不影响

### ⚠️ 环境踩坑（重要，多 Redis 实例争抢 localhost:6379）

- **现象**：后端发码返回 200、JVM 内 set/get 自洽，但 `docker exec redis` 容器里查不到验证码 key。
- **根因**：机器上同时存在**两个 Redis**——Docker 容器 redis（docker-desktop）与 Ubuntu-24.04 里 apt 安装的独立 redis-server；Windows 上 `localhost:6379` 的 IPv4（wslrelay）与 IPv6（com.docker.backend）被分别转发到了这两个实例（对比双栈路径的 `INFO server` run_id 不一致而坐实）。Java/Lettuce 走 IPv4 → Ubuntu 那个 Redis，而排查用的是 Docker 容器，于是"写的地方"和"看的地方"不是同一个库。
- **修复**：停用并禁用 Ubuntu 内 redis（`sudo systemctl disable --now redis-server`），收敛为 Docker 容器唯一一个；重启后端后双栈 run_id 一致，验证码正确落容器。
- **教训 / 面试话术**：`localhost` 在 IPv4/IPv6 双栈 + WSL2 端口转发下不保证唯一；同机多实例要靠不同端口或 IP 隔离，排查"写入成功但查不到"时优先核对连接的实际 host/port/database 与服务端 run_id。

### 🔧 后续加固（接真实短信前必修，2026-09-22 评估）

> 当前"双 String key"对单一登录 + Mock 场景合理；以下三点在接入真实短信通道前补齐。

- [ ] **② 验证码校验失败次数上限（防暴力枚举）**
  - 背景：6 位数字共 100 万种，验证码 5 分钟有效，不限尝试次数理论上可被枚举。
  - 方案：新增 `sms:try:{phone}`（String 整数）——每次校验失败 `INCR`（首次失败设 TTL 与验证码对齐，300s），失败达 **5 次**直接 `DEL sms:code:{phone}` 作废，强制重新获取；校验成功时一并 `DEL sms:try:`。
  - 可选演进：把 `code + failCount` 合并为一个 Hash（`HSET/HINCRBY`），但 60s 频控 key 仍独立（Hash 无法对单字段设 TTL）。

- [ ] **③ 写缓存与发短信的顺序 / 原子性**
  - 现状：`sendCode` 是**先写 Redis（验证码+频控）、后调 `SmsService.send()`**；接真实短信后若发送失败，用户没收到码却已进入 60s 等待。
  - 方案 A（推荐）：**先调发送、成功后再写两个 key**；
  - 方案 B：保持先写，但 `send()` 抛异常时回滚 `DEL` 掉 code/limit 两个 key。
  - 附加：两条 SET 之间进程崩溃会导致频控缺失（低危），可用 Lua 脚本把"写 code + 写 limit"做成原子操作。

- [ ] **④ 同手机号 / 同 IP 每日发送总量上限（防资损）**
  - 背景：`sms:limit` 只管 60s 间隔，真实短信按条收费，不限总量一天可被刷上千条。
  - 方案：新增 `sms:daily:{phone}:{yyyyMMdd}`（String 整数，TTL 到当日 24 点）——每次发送前 `INCR`，超过上限（如 10 条/日）拒绝；同一逻辑可按 IP 维度再加 `sms:daily:ip:{ip}:{date}`，对应 A-7 防刷。

> 另：key 建议补"业务场景"段（`sms:code:login:{phone}` / `sms:code:reset:{phone}`），避免以后 A-6 找回密码与登录验证码互相覆盖/越场重放——此项未列入本清单，可视情况提前做。

## AI 多模态对话（2026-09-18，🚧 开发中）

> 需求：新增「AI 设计助手」对话能力，AI 具备图文理解（用户可发装修图片提问）。
> 技术方案：LangChain4j 1.0.1-beta6（`@AiService` 声明式）+ 智谱 **glm-4.6v-flash** 视觉模型（OpenAI 兼容协议），Key 走 `ZHIPU_API_KEY` 环境变量。

- [x] **模型接入层**：`pom.xml` 引入 langchain4j open-ai + spring boot starter；yml 配 `langchain4j.open-ai.chat-model`，由 Starter 自动装配 `openAiChatModel`（手写 AiConfig 已删除，避免重复 bean）
- [x] **声明式接口**：`AiChatAssistant`（`@AiService` + 装修设计师 `@SystemMessage`，`SYSTEM_PROMPT` 常量供图文链路复用）；**仅保留纯文本重载**（1.0.1 的 @AiService 不支持图片参数，见下方踩坑）
- [x] **Service 分层**：`AiChatService` + `AiChatServiceImpl`，纯文本 / 图文 / 纯图（默认提问"请解释这张图"）三分支
- [x] **异常转译**：`RateLimitException / TimeoutException / InternalServerException / UnresolvedModelServerException` → `BusinessException(503, "AI 服务繁忙")`；401（Key 错/欠费）不吞，原样暴露
- [x] **请求 DTO**：`AiChatRequest { question, images(List<String> DataURL) }`
- [x] **正式对话接口**：`AiChatController` —— `POST /api/ai/chat`（JSON：question + images DataURL 列表）；Controller 内解析 `data:{mime};base64,{数据}` 头 → `ImageContent.from(base64, mimeType)`；四分支（双空/空数组 400 / 纯文本 / 纯图 / 图文）；DataURL 缺分号等非法格式 400（2026-09-19 Swagger 实测 5 用例通过，图片走 base64，localhost URL 智谱云端拉不到）
- [x] **清理测试接口**：已删除 `TestAiController`（GET /api/chat，`@RequestParam List<ImageContent>` 无法绑定，仅纯文本可用）
- [x] **版本踩坑（重要）**：langchain4j 1.0.1（starter 1.0.1-beta6 实际依赖的核心包）的声明式 `@AiService` **不支持 `List<ImageContent>` 方法参数**（`DefaultAiServices.validateParameters` 要求 ≥2 参时每个参数都必须有 @V/@UserMessage/@UserName/@MemoryId，且消息组装只取文本模板）。最终架构：**纯文本走 `AiChatAssistant`（@AiService）；图文/纯图在 Service 层直接注入 `ChatModel`，手工 `UserMessage.from(List<Content>{TextContent + ImageContent...})` 调用**（注意 `UserMessage.from(String, List)` 的 String 是昵称不是正文）
- [x] **Bean 冲突踩坑**：Starter 已按 yml 自动装配 `openAiChatModel`，不要再手写 `AiConfig` 里的 `@Bean ChatModel`（两个同类型 bean 导致启动报 IllegalConfigurationException），已删除 AiConfig
- [ ] **纯文本拒答不稳定（实测发现）**：glm-4.6v-flash 对"非装修问题礼貌拒绝"指令遵循弱（图片场景能拒，纯文本问冒泡排序仍答代码）；待强化系统提示词（few-shot 示例 / 更强约束措辞）后回归
- [x] **多轮记忆（AI-3，✅ 2026-09-23 完成，本期仅纯文本链路，学习模式一步步教）**：契约见接口文档 §11.3
  - [x] 1. `AiChatRequest` 加 `conversationId` 字段
  - [x] 2. 新增响应 DTO `AiChatMemoryResponse {conversationId, answer}`（dto/response）
  - [x] 3. 新增配置类 `AiConfig`：`ChatMemoryProvider` Bean（`dev.langchain4j.memory.chat` 包），内部 `ConcurrentHashMap` + `computeIfAbsent`，窗口 `maxMessages(10)`
  - [x] 4. `AiChatAssistant` 改为记忆版 `chat(@MemoryId String, @UserMessage String)`；`@AiService(chatMemoryProvider="chatMemoryProvider")`，`@SystemMessage` 标在**方法**上（接口级在1.0.1不生效，见接口文档§11.2-9）
  - [x] 5. `AiChatService` 文本方法签名改为带 conversationId、返回 `AiChatMemoryResponse`
  - [x] 6. `AiChatServiceImpl`：conversationId 空则生成 UUID → 调记忆版 chat → 组装响应（保留 503 异常转译）
  - [x] 7. `AiChatController` 纯文本分支透传 conversationId，方法返回 `Result<?>` 兼容两种响应形态
  - [x] 8. 前端（助手实现）：`api/ai.js` + `AiChat.vue` 保存/回传 conversationId，新会话按钮重置
  - [x] 9. 浏览器实测通过：首轮返 id、追问"那要多少钱"正确承接 30平原木风上下文、新会话隔离不串
  - 后续（不在本期）：图文链路手工记忆；窗口淘汰（>10条）可选补测
- [ ] **AI-3 存储升级 Redis（🚧 2026-09-24 启动，企业标准，文档已先行）**：
  - [ ] 1. 新建 `common/RedisChatMemoryStore implements ChatMemoryStore`：`StringRedisTemplate` + `ChatMessageSerializer`/Jackson codec；key 前缀、TTL 走 `@Value`（`app.ai.memory-ttl-seconds:86400`）
  - [ ] 2. 改 `AiConfig`：删 JVM Map，Provider builder 加 `.chatMemoryStore(store)`
  - [ ] 3. application.yml 加 `app.ai.memory-ttl-seconds: 86400`
  - [ ] 4. 实测：发 3 轮 → 重启后端 → 追问仍承接上下文；Redis 中可见 `ai:memory:{id}` JSON
  - 选型：自研 Store 而非 langchain4j-redis——零新增依赖（复用 Lettuce）、TTL 可控、吃透 SPI
- [ ] **图片 URL/base64 混合模式（OSS 已就绪，前置条件满足，对应 F-3 已完成）**：`AiChatController.toImageContent` 按前缀分流——`http(s)://` 开头走 `ImageContent.from(URI.create(url))`（智谱服务器自行下载），`data:` 开头走现有 DataURL 解析；接口契约 `images: string[]` 与前端零改动。约束：必须公网可达（公共读或有效期 ≥5min 的签名 URL；用公网 endpoint 而非 `-internal`；不能开 Referer 防盗链，智谱拉取不带 Referer）。适用：已存档图片（帖子图/设计图）走 URL 省 33% base64 膨胀，用户本地新图仍走 base64
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
- [x] **第 2 层 自动重试（框架自带）**：LangChain4j 1.0.1 字节码核实默认 `maxRetries=2`（首发 + 2 次重试 = 最多 3 次 HTTP 请求），退避公式 `500ms × 1.5^i + 0~20% 抖动`（约 0.5s / 0.75s 两档等待，见 `RetryUtils$RetryPolicy`）；429/5xx/超时/IO 才重试，`NonRetriableException`（400/401/403）直接抛；注意重试只对瞬时抖动有效，持续限流时重试只增加等待，次数不是越大越好
- [ ] **第 2 层（补）显式配置**：yml 加 `langchain4j.open-ai.chat-model.max-retries: 3`，把隐式默认值写明便于调参
- [ ] **第 3 层 异常转译（下一步先做这个）**：重试仍失败时，不能一律返回 500
  - 位置：学习期先在 `TestAiController.askAi` try-catch；正式业务改为 `AiChatService` 接口 + `AiChatServiceImpl` 分层（参照 LoginService 模式，@AiService 代理接口内加不了逻辑）
  - 映射：`dev.langchain4j.exception.RateLimitException`（429，`HttpException` 子类）→ `BusinessException(503, "AI 服务繁忙，请稍后再试")`；超时同理 503
  - **易错点**：不要 catch 所有 Exception 都转 503——`401 api-key 错误/欠费`是配置问题，应保留原错误暴露出来，不能伪装成"繁忙"；只转译限流(429)/超时/5xx 这几类瞬时错误
- [ ] **第 4 层 降级兜底**：对话失败时返回友好兜底文案而非报错；文生图已有更优范式——异步任务状态机把失败存 `errorMessage`，不阻塞主流程（已实现）
- [ ] **前端重试体验**：收到 503 时弹窗带「重试」按钮（`frontend/src/api/request.js` 响应拦截器目前只做消息提示）
- [ ] **第 5 层 多模型 fallback（上线前评估，勿提前过度设计）**：主模型持续失败自动切备用（智谱付费模型 / 另一 OpenAI 兼容厂商）；LangChain4j 无一行配置开关，自写包装类 try A catch(限流/超时) → B
- [ ] **第 6 层 结果缓存（性价比高，复用现有 Redis）**：装修风格知识类问题重复率高，相同问题缓存回答 TTL 1h，命中即返回不调模型，既提速又从根上减少限流；key 如 `ai:chat:{md5(systemPrompt+question)}`

## 照片漫游重构：AI 读户型 + 多房间写实生图 + Viewer3D（2026-09-27 启动，🚧 文档已先行）

> 需求：V-1 照片漫游落地 + V-4 自定义户型 A/B 层。决策（已确认）：宽幅照片漫游（非真 360）；AI 视觉识别户型图房间/拓扑/面积 + 查看页户型小地图；生图模型 `cogview-3-flash`（免费，模型名配置化可切 cogview-4）。
> 契约：接口文档 §4.1（两阶段流程）/ §7.1（sceneConfig photo-tour v2）/ §9（409 防并发、识别失败）。数据库无 DDL 变更（scene_config LONGTEXT 承载）。
> 存储：房间效果图落 `storage/panoramas/`；户型图读本地文件转 base64 喂视觉模型（localhost URL 智谱拉不到）。

### 后端（小步切片，每步可独立验证）

- [x] 1. **配置修正**：`application.yml` — `app.ai.image-model` 改为 `${AI_IMAGE_MODEL:cogview-3-flash}`（原误配视觉模型名）；新增 `app.ai.room-image-size: 1440x720`、`app.ai.max-rooms: 6`；视觉模型复用 `langchain4j.open-ai.chat-model`（glm-4.6v-flash）
- [x] 2. **AIimageService 参数化**：`generateImageUrl(prompt, size)` 重载，size 从配置注入（单参签名默认走配置宽幅）；Maven compile 通过
- [x] 3. **户型识别 DTO**：`dto/ai/FloorPlanResult`、`RoomPlan`（id/name/approxArea/features/connects/doorSides/bbox）、`RoomBBox`（x/y/w/h）
- [x] 4. **FloorPlanAnalysisService + ZhipuFloorPlanAnalysisServiceImpl**：注入 LangChain4j `ChatModel`（SystemMessage 固定 JSON 契约 + UserMessage(TextContent+ImageContent)）；local 存储读盘 base64（从 designImageUrl 的 /files/ 前缀解析相对路径，normalize+startsWith 防穿越），公网 URL 走 ImageContent.from(URI)；剥离 ```json 围栏后 Jackson 反序列化；校验规范化（id 蛇形唯一/connects 去噪+双向闭合/doorSides 对齐/bbox 裁剪/房间数截断/entryRoomId 兜底）
- [x] 5. **sceneConfig v2 组装 DTO**：`dto/scene/PhotoTourScene`、`RoomScene`、`Hotspot`；doorSide → x 规则化（left .24 / center .50 / right .76，y=.66，同方位多门错位错开）
- [x] 6. **GenerationServiceImpl.runGeneration 重构**：识别重试 1 次 → FAILED 友好提示；逐房间串行生图+落 `panoramas/`，单间重试 1 次仍败整套 FAILED（注明房间名）；组装 PhotoTourScene 写 sceneConfig，入口房间图写 panorama/preview；**顺带修复旧 bug：成功路径漏 updateById 导致 SUCCESS 不落库**
- [x] 7. **防并发（G-4 后端兜底）**：同项目存在 PENDING/PROCESSING 任务 → BusinessException(409,「该项目正在生成中，请等待当前任务完成」)
- [x] 8. **实测（2026-09-27 全通过）**：标准 CAD 户型图——识别 6 房间 JSON 正确（面积/bbox/双向连接）、每房间 1440×720 出图落盘、sceneConfig v2 闭合；**防并发实测首版 selectCount 有 check-then-act 竞态（并行双发双双放行，任务 17/18），已改 Redis SETNX（`generation:active:{projectId}`，终态 finally 释放 + 30min TTL 兜底）；复测并行双发严格 200+409、进行中再发 409、任务 19 FAILED/任务 20 SUCCESS 后占位均自动释放**；识别偶发失败（视觉模型高峰期超时，同图任务 19 败/20 成）由重试 1 次 + 友好提示兜底

### 前端（助手直接实现）

- [x] 9. **路由**：`/viewer/:generationId` → `views/Viewer3D.vue`，`meta.hideTab` 全屏沉浸
- [x] 10. **Viewer3D.vue**（已实测任务 16）：getGeneration 拉任务（支持 PENDING/PROCESSING 轮询）→ 解析 sceneConfig（v2 漫游 / v1 单房间无热点降级）；全屏 cover + **Ken Burns 32s 缓慢来回扫视 + 鼠标视差 ±14px**（静态照片产生空间动感）；呼吸圆点热点（pulse 扩散环 + 房间名，点击 500ms 交叉淡入，切房交替扫视方向）；底部 6 房间缩略图条；顶部房间编号/名称/估算面积；左下户型小地图（原图 + bbox 叠块当前陶土高亮 + 房间图例可点）；首次引导提示 6s 自动消失；键盘 ← → 切房、Esc 返回；加载/失败态；prefers-reduced-motion 降级；杂志风令牌
- [x] 11. **ProjectDetail.vue 入口改造**：「查看效果图 →」按钮 → `router.push('/viewer/'+id)`；删除 el-dialog 全景弹窗与 panoramaVisible；生成历史 SUCCESS 项点击也进 Viewer（进行中/失败项仍侧栏切换）
- [x] 12a. **浏览器实测**：首屏 6 缩略图/3 热点/小地图叠块正确；客厅→餐厅→厨房、缩略图直跳主卧四处联动（画面/编号/小地图/缩略图）正常；返回跳 `/projects/12`；入口独立页无 dialog
- [x] 12b. **409 复测 + 知识库**：重启后并行双发严格 200+409、进行中再发 409、任务终态（19 FAILED / 20 SUCCESS）占位均自动释放；已沉淀 `docs/知识库/04-多房间照片漫游.md` + README 索引

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
  - 后续优化：**两段式 TTL**——当前固定窗口从第 1 次失败起算，若 5 次失败拖得分散（接近 600s），触发锁定时剩余锁定时间可能不足。改进：第 1 次失败 `EXPIRE 600`（计数窗口）；计数达阈值那一刻再 `EXPIRE 600` 一次（锁定窗口），保证锁定恒有完整 10 分钟。再进一步可升级滑动窗口（ZSET）
- [x] **登录限流安全加固（2026-10-11，防恶意锁定/撞库/用户名枚举）**
  - [x] Lua 脚本原子化 INCR+首次 EXPIRE（账号/IP/短信失败计数共用），防两步之间崩溃导致 key 永不过期；`DefaultRedisScript<Long>`
  - [x] parseInt 脏数据容错：NumberFormatException 时删 key 并放行
  - [x] 新增 IP 维度 `login:ip:fail:{ip}`，阈值 20/10 分钟（`app.login-limit.ip-max-fail-count` / `ip-lock-seconds`），登录成功不清零只靠 TTL；`common/IpUtils` 取 X-Forwarded-For 首段，NoRepeatSubmitAspect 复用
  - [x] 用户不存在不写账号计数（防锁死他人），仅记 IP；对固定 cost=10 的 `DUMMY_HASH` 跑一次 BCrypt 拉平耗时（实测两路径均约 90ms）
  - [x] 失败统一抛 BusinessException(400, "用户名或密码错误")，Controller 删除 null 兜底分支
  - [x] 实测（2026-10-11）：不存在用户连刷零账号 key；账号第 5 次 429；IP 第 20 次 429；成功登录只清账号不清 IP
  - 后续：IP 取信仅在可信代理后采信 X-Forwarded-For（修复计划已列）；同 IP 注册限频 A-7 待做
- [x] **令牌失效机制（A-9 退出登录先行，对应需求 S-4）**：改密/登出使旧 JWT 失效
  - 契约：接口文档 §2.5——`POST /api/auth/logout`（**需鉴权**，不加入 WebConfig 放行列表）；登出把当前 token 写 Redis 黑名单立即失效，拦截器对所有受保护接口增加黑名单校验
  - [x] 后端 `common/TokenBlackListService.java`（参照 `LoginRateLimiter` 模式注入 `StringRedisTemplate`）：
    - `blacklist(token)`：key = `jwt:blacklist:` + `SHA-256(token)` 十六进制串（不存原始 token，避免 payload 落 Redis），value 固定 `"1"`，TTL = token 剩余有效期（`exp - now`，秒级，最小兜底 1s），到期自动清理
    - `isBlacklisted(token)`：`hasKey` 判断；Redis 异常时如何处理需斟酌（建议放行并打 error 日志——fail-open 保证可用性，与"登录限流强依赖"区分，黑名单只影响登出生效时机）
  - [x] ~~`JwtUtil` 增加 `getExpiration(token)`~~：实际未新增该方法，直接在 Service 内 `jwtUtil.parseToken(token).getExpiration()` 取 exp；黑名单的 SHA-256 计算放 Service 内
  - [x] `LoginController` 新增 `POST /api/auth/logout`：从 `Authorization` 头取 token → `TokenBlackListService.blacklist(token)` → `Result.success("成功退出")`；无请求体
  - [x] `JwtInterceptor.preHandle`：`isValid` 通过后、写 `UserContext` 前加 `isBlacklisted` 判断，命中走现有 `reject(...)` 返回 401（实际提示文案为"token非法！"，后续可统一为"登录已过期，请重新登录"）
  - [x] 前端 `api/auth.js`：新增 `logoutApi()` → `request.post('/api/auth/logout')`
  - [x] 前端 `stores/user.js`：`logout()` 改 `async`——`try { await logoutApi() } catch { /* 吞掉 */ } finally { 清 token/userInfo/removeToken() }`，接口成败不阻塞本地清理
  - [x] 前端 `views/Profile.vue`：`onLogout` 改 `async` 并 `await userStore.logout()` 后再提示 + 跳登录页
  - [ ] 注意 401 拦截器副作用：`request.js` 响应拦截器对 logout 请求返回 401 会弹"登录已过期"并跳登录页，与正常登出殊途同归但可能多一条 toast；如体验不佳，实现时给该请求加静默标记（如 `config.skipAuthRedirect`）
  - [x] 验证（需本地 Redis，2026-10-07 实测）：① 登录拿 token → 调 logout 返回 200；② 旧 token 再调 `/api/auth/me` 返回 401；③ `redis-cli` 确认 `jwt:blacklist:*` key 存在且 TTL ≈ 7 天（实测 604688s）；④ 重新登录的新 token 访问正常；⑤ Redis 中 key 到期自动消失（TTL 机制保证，未实测等待）；⑥ 无 token/伪造 token 调 logout 返回 401 且前端仍退回登录页
  - 不做：不引入 refresh token 体系；A-5 修改密码实现时直接复用 `TokenBlackListService`（改密成功后拉黑当前 token）

## 体验与扩展

- [ ] **项目列表分页**（§3.2）
- [ ] **帖子搜索/筛选**（按内容/标签）
- [x] **对象存储（阿里云 OSS）**：`FileStorageService` 抽象 + 模板方法基类 `AbstractFileStorageService`（校验/命名/MIME/远程下载），Local 与 OSS 双实现按 `app.storage.type`（local/oss）条件装配；OSS 实现单例 `OSSClient`（@PostConstruct fail-fast + @PreDestroy 关连接池）、显式 Content-Type 与一年缓存头。2026-09-20 实测：oss 上传匿名 200、响应头正确、MD5 一致；local 回退正常
- [ ] **CDN 加速域名**：OSS 已通，后续在其前面挂 CDN，仅需替换 public-base-url
