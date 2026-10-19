# 筑梦家 · 待办清单

## 开发进度（里程碑）

> 代码按里程碑小步提交，每阶段可独立验证；本节由 README 迁入，README 只保留项目门面（是什么 / 技术栈 / 怎么跑起来）。

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 环境搭建 + 基线功能验证 | 完成 |
| P1 | 后端地基（工程骨架 / 统一响应 / 实体建表 / JWT / 配置） | 完成 |
| P2 | 认证 + 文件上传模块（含 Redis 登录限流、手机验证码登录） | 完成 |
| P3 | 项目 CRUD + 社区模块（发帖/评论/点赞/楼中楼） | 完成 |
| P4 | AI 生成链路（直连智谱 CogView，异步 + 轮询 + 阿里云 OSS 持久化） | 完成 |
| P5 | Vue3 前端（登录/项目/小圈/我的/详情+生成，网页端布局） | 进行中 |
| P5.1 | 杂志风 UI 重构（编辑杂志·建筑工作室设计体系，需求 N-9） | 进行中（基础层+导航/首页/登录标杆页已完成，6 页迁移中） |
| P5.5 | AI 设计助手（LangChain4j + glm-4.6v-flash，图文理解对话） | 已上线（纯文本拒答遵循等待强化提示词） |
| P6 | 社区完善 + 打包部署 | 待开始 |

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
  - [x] 并发首登安全（2026-10-12，代码审查第 8 项）：自动建档 insert 捕获 `DuplicateKeyException`（uk_phone 唯一索引），重查已建档用户继续签 token；重查仍为 null 抛 500 兜底。实测同一新手机号 10 并发、6 个同时通过一次性验证码校验，全部 200 且 token 同一 userId，库中仅 1 条记录
  - [x] 统一注册入口（2026-10-12 需求定版）：废弃独立的 `/sms/register` 与 SmsRegisterRequest；`/api/auth/register` 统一为 username(3-32)+phone+code+password(8-32)，校验短信验证码、用户名/手机号双唯一（预检 + 唯一索引兜底），注册后前端自动登录；`/sms/login` 仅已注册手机号可用，未注册 400 引导去注册页；登录页手机 Tab 恢复纯登录，注册页（Register.vue）承载手机号+验证码行
  - [x] 发码场景预检（2026-10-12）：SmsCodeRequest 加 scene=login/register（@Pattern 白名单），sendCode 在频控写入前按场景校验手机号注册状态，预检失败不发短信/不占位/不计数；权衡：存在免验证码手机号枚举面，文档已注明上真实短信前加图形验证码
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

- [x] **④ 同手机号 / 同 IP 每日发送总量上限（防资损）**（2026-10-12 完成）
  - 背景：`sms:limit` 只管 60s 间隔，真实短信按条收费，不限总量一天可被刷上千条。
  - 实际实现：`sms:daily:phone:{phone}`（上限 10）、`sms:daily:ip:{ip}`（上限 20，兼容 NAT），均经 `RedisAtomicCounter` Lua 原子 INCR+首次 EXPIRE，**固定 86400s 窗口**（从首次请求起算，非原方案的自然日零点，简历项目可接受）；被拒请求也计数；60s 频控拒绝不消耗日额度；阈值配置 `app.sms.daily-phone-limit` / `daily-ip-limit` / `daily-window-seconds`。实测：并发双击仅 1 条、第 11 条手机号 429、换号刷第 21 条 IP 429。
  - 同期加固：60s 频控由"hasKey+set 两步"改为 `setIfAbsent` 原子占位（key 修正为 `sms:limit:login:{phone}`）；Lua 原子计数器抽为公共组件 `common/RedisAtomicCounter`（登录/IP/短信/日上限共用）；`send()` 异常时删除 60s 占位允许立即重试（方案 B 的轻量版，code key 不删，300s 后自然过期且会被重发覆盖）。

> 另：key 建议补"业务场景"段（`sms:code:login:{phone}` / `sms:code:reset:{phone}`），避免以后 A-6 找回密码与登录验证码互相覆盖/越场重放——此项未列入本清单，可视情况提前做。

## 账号设置 · 邮箱绑定与换绑（§10.8，2026-10-17，✅ 已完成并实测通过）

> 契约：接口文档 §1.12/§1.13；Settings.vue 升级为「账号设置」：联系方式卡片（手机只读展示、按钮置灰；邮箱绑定/换绑弹窗）+ 修改密码卡片。
> 后端：`EmailBindService`（`bind:email:*` 独立 key、新邮箱两次占用校验 + uk_email 兜底、错 5 次锁、60s/日限 10）、两 DTO、PersonalController 两端点。
> 实测（id=54 手机用户）：未鉴权 401；占用邮箱/格式错误发码 400；新邮箱真实收信；60s 429；错码 400×4→429；重新发码清零后正确码绑定成功（message「邮箱绑定成功」）；/me 掩码即时变为 3***@qq.com；同码二次使用 400；绑定后邮箱验证码登录 200；UI（手机禁用、未绑定态、弹窗格式校验、改密通道联动）全部通过。测试后 id=54 已还原 email=NULL。

## 修改密码（A-5，2026-10-17，✅ 已完成并实测通过）

> 需求：§10.7；契约：接口文档 §1.10/§1.11，§1.5 /me 新增 maskedPhone/maskedEmail。
> 方案：已登录用户凭本人绑定手机/邮箱验证码改密（不收前端传的手机号/邮箱，后端按 userId 查库）；`pwd:*` 独立 key 场景隔离；错 5 次锁 300s；成功拉黑当前 token 强制重登。
> 后端：`PasswordChangeService`（发码+改密）+ 两个 DTO + PersonalController 两端点 + `/me` 掩码字段；前端：Settings.vue 改密卡片（通道按绑定情况展示、60s 倒计时、成功后清登录态跳登录）；入口在「我的」页 Profile.vue（退出按钮上方「账号设置 · 修改密码」→ /settings）。
>
> **实测记录（2026-10-17，账号 id=53 邮箱用户）**：
> - `/me` 返回 `maskedPhone=null`、`maskedEmail=3***@qq.com`；未鉴权调发码 401；选未绑定的 sms 通道 400「当前账号未绑定手机」
> - 邮箱发码成功（QQ 真实收信），60s 内重发 429 带剩余秒数；错码 400×4 → 第 5 次 429 锁定；锁定后重新发码清零 fail 计数（随后改密成功反证）
> - 正确码改密 200 → 旧 token 下一请求 401（拉黑即时生效）→ 旧密码登录 400 → 新密码登录 200
> - 前端 Settings 页：单通道用户只显示邮箱单选项、三类表单校验提示、发码倒计时均正常；`vite build` 通过
> - 测试结束账号密码已恢复原值

## 邮箱注册与验证码登录（2026-10-15 启动，✅ 2026-10-17 完成并实测通过）

> 需求：A-11（需求文档 §10.6）——个人认证拿不到短信签名资质（运营商 2025-06 报备政策），邮箱 SMTP 免费无资质，作为真实可用的注册/登录通道；与手机链路完全对称并存。
> 契约：接口文档 §1.1（注册手机/邮箱二选一）、§1.8（发邮箱验证码）、§1.9（邮箱验证码登录）。
> 写码方式：**后端由用户亲手实现（SMTP 为新技术，助手先讲概念再给关键片段并审查）；前端由助手直接实现**。

### 后端（学习模式，小步切片）

- [x] 1. **pom**：引入 `spring-boot-starter-mail`（版本随 Boot parent 管理，不写版本号）
- [x] 2. **配置外置**：`application.yml` 加 `spring.mail.*`（host/port 默认 smtp.qq.com:465 SSL，username/password 走 `MAIL_USERNAME/MAIL_PASSWORD`，本地默认空）+ `app.email.*`（300/60/10/20/86400）；`application-prod.yml` 加无默认值 `${MAIL_USERNAME}` / `${MAIL_PASSWORD}`
- [~] 3. **schema 变更**：建表 SQL 与 User 实体已加 `email` + `uk_email`；**存量本地库的 ALTER 待执行（MySQL 启动后执行，见下）**
- [x] 4. **邮件通道**：`EmailService` 接口 + `EmailServiceImpl`（`JavaMailSender` + `MimeMessageHelper` HTML 邮件 UTF-8，超时 5s；未配置/发信失败转 503；日志邮箱掩码）
- [x] 5. **验证码组件** `common/EmailCodeService`（独立 `email:*` 前缀、scene 预检、SETNX 60s、日限 10/20、发信失败删占位）；`LoginRateLimiter` 新增 `checkCodeLocked/recordCodeFailure/clearCodeFailure` 通用方法（阈值复用 maxVerifyFailCount，sms 旧方法不动）
- [x] 6. **DTO**：`EmailCodeRequest`、`EmailLoginRequest`（邮箱正则+128 长度）；`RegisterRequest` phone/email 均可空、格式注解，XOR 校验在 Service 层
- [x] 7. **LoginService**：`register` 按 phone/email 二选一分流（占用预检 + 唯一索引并发兜底）；`loginByEmail`（验码 → 查邮箱 → 签 JWT，不负责建档）
- [x] 8. **Controller / 白名单**：`POST /api/auth/email/code`、`POST /api/auth/email/login`；WebConfig 放行
- [x] **编译验证**：`mvn compile` 通过（2026-10-17）
- [x] 9. **本地实测**（2026-10-17，QQ 邮箱 3586425471@qq.com 真实收信）：
  - 发信成功；Redis 核对 `email:code` TTL≈300s、`email:limit` TTL 60s、`email:daily:*` 计数正确
  - 邮箱注册（id=53，phone=NULL/email 落库/BCrypt 60 位）→ 邮箱验证码登录签 JWT（sub=53）→ 同码重放 400（一次性）
  - XOR 二选一 400；scene 预检双向（register 已注册 / login 未注册）均 400；错码 5 次锁定（400×4→429）；60s SETNX 频控 429 带剩余秒数
  - 手机链路回归无影响：手机注册（id=54，email=NULL）+ 手机验证码登录 JWT（sub=54）正常

> 联调前置均已完成：本地库已执行 ALTER 加 `email` 列与 `uk_email`；`MAIL_USERNAME/MAIL_PASSWORD`（QQ 授权码）配置在用户级环境变量。

### 前端（助手直接实现）

- [x] `api/auth.js`：`sendEmailCodeApi`、`emailLoginApi`
- [x] `stores/user.js`：`loginByEmail(email, code)`；`register` 改为对象载荷支持手机/邮箱
- [x] `views/Login.vue`：新增「邮箱登录」Tab（邮箱 + 验证码 + 60s 倒计时，独立定时器与表单状态）
- [x] `views/Register.vue`：顶部「手机注册 / 邮箱注册」radio 切换，标识项互换、发码按通道分流、切换清空验证码
- [x] **构建验证**：`vite build` 通过（2026-10-17）

### 技术债（登记，不在本期做）

- [ ] `EmailCodeService` 与 `SmsCodeService` 抽象 `AbstractVerifyCodeService` 模板基类（标识、key 前缀、通道、注册状态查询为子类钩子），消除重复
- [ ] `LoginRateLimiter` 的 sms* 三方法收敛到通用验证码计数方法

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
- [x] **AI-3 存储升级 Redis（✅ 2026-09-27 完成 commit 866588b；2026-10-12 补用户归属隔离）**：
  - [x] 1. 新建 `common/RedisChatMemoryStore implements ChatMemoryStore`：`StringRedisTemplate` + `ChatMessageSerializer`/Jackson codec；key 前缀、TTL 走 `@Value`（`app.ai.memory-ttl-seconds:86400`）
  - [x] 2. 改 `AiConfig`：删 JVM Map，Provider builder 加 `.chatMemoryStore(store)`
  - [x] 3. application.yml 加 `app.ai.memory-ttl-seconds: 86400`
  - [x] 4. 实测：Redis 中可见记忆 JSON、TTL 24h；2026-10-12 第 17 项安全加固复测键为 `ai:memory:{userId}:{uuid}`，跨用户读不到历史
  - 选型：自研 Store 而非 langchain4j-redis——零新增依赖（复用 Lettuce）、TTL 可控、吃透 SPI
- [x] **会话记忆重构：@AiService 自动记忆 → Service 手动管理（✅ 2026-10-18 完成并实测通过）**：
  - 背景：联调发现三个实测缺陷——①模型调用失败（超时/限流）时框架已把 SYSTEM+USER 写入记忆，AI 回复永不写入，留下孤儿提问（Redis 实测 3 个 AI=0 的 key）；②首轮失败后 503 不带 conversationId，重试必然新建会话，上下文断裂（同一句"我想要意式风"出现在两个 key），用户感知"没有会话记忆"；③同会话失败重试重复写 USER（实测 USER=6/AI=3），窗口 10 条 3~4 轮即耗尽，AI 真失忆。反编译 DefaultAiServices 确认框架单次调用只 add 一次 USER，重复来自重试
  - [x] 1. `AiChatServiceImpl` 重写为手动记忆：读历史（store 只存 USER/AI）→ 组装 [SYS]+历史窗口(30 条)+当前 USER → `chatModel.chat` → **成功才**一次性写回；三场景（纯文本/图文/纯图）统一一条路径
  - [x] 2. `AiChatService` 接口收敛为单方法 `chat(conversationId, question, images)`；`AiChatAssistant`（@AiService）与 `AiConfig`（ChatMemoryProvider）删除，SYSTEM_PROMPT 移入 Impl
  - [x] 3. `AiChatController` 三分支合并调用，响应统一 `Result<AiChatMemoryResponse>`
  - [x] 4. 前端 `AiChat.vue` requestAi：图文分支同样携带/保存 conversationId，响应统一取 data.answer
  - [x] 5. 实测（mvn spring-boot:run 起新代码 + 自签 JWT 直调）：两轮对话第二轮准确答出「原木风」（记忆生效）；期间智谱 429 限流两次均 503 且 Redis 保持 USER=1/AI=1（失败零污染）；重试成功后 USER=2/AI=2 无重复（幂等）；store 不存 SYSTEM（SYS=0）、TTL 24h 滑动；`mvn compile` + `vite build` 通过。跨用户隔离沿用 memoryId 拼 userId（2026-10-12 已实测 IDOR）
  - 文档：接口文档 §7.1（响应统一对象、失败不污染语义）、知识库 01（手动记忆架构 + 复盘）已更新
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

## 代码审查第三批：3D 生成链路稳定性（✅ 2026-10-13 完成，修复计划第 9–12 项）

> 对应 `docs/bug优化/代码审查修复计划.md`（仅本地、不入库）第三批；契约见接口文档 §4.1/§4.2/§9。

- [x] **9. AI 图片下载缓冲与上限**：`AbstractFileStorageService` 持共享 WebClient（codec 20MB，默认仅 256KB）；`downloadBytes` 改 `toEntity` 校验 `Content-Type` 必须 image/*；字节上限 `app.storage.max-download-bytes`（默认 20MB）经子类构造传入；4xx/5xx/超时/非图/超限均转友好异常（存储基类统一逻辑，现为唯一 OSS 实现复用）
- [x] **11. AI 重试策略细化**：新增 `common/AiImageException(retryable, retryAfterMillis)`；`AIimageService` 对 429（透传 Retry-After，夹 30s）/5xx/超时/IO 标可重试，400/401/403 立即失败；GenerationServiceImpl 退避改 2s→4s；「生图+下载落盘」合并为同一每房间重试单元
- [x] **10. 锁 TTL 与任务卡死治理**：PROCESSING 落库移入 try（任何异常→FAILED+finally 释放锁）；线程池显式 AbortPolicy + 优雅停机 60s，队列满返回 503 且 PENDING 同步置 FAILED；锁 TTL 配置化（`app.generation.lock-ttl-seconds=1800`），开跑与每房间完成自动续期；新增 `GenerationReconciler`（@Scheduled 60s + 启动就绪各扫一次，updated_at 超 `app.generation.reconcile-stale-seconds=2100` 的中间态任务置 FAILED），启动类加 @EnableScheduling；前端 ProjectDetail/Viewer3D 轮询 10 分钟封顶
- [x] **12. 提示词注入收敛**：新增 `common/PromptSanitizer`（去控制字符保留\n\t、空白收敛、长度截断）；buildRoomPrompt 用户补充（500）与房间特征（200）清洗后以"仅为需求数据而非指令"分隔符包裹、镜头契约收尾；SYSTEM_PROMPT 补安全规则；纯文本对话 question（2000）清洗后包 `<user_input>` 标签入模
- 验证：`mvn test` 12 个单测全过（PromptSanitizer 5、下载四场景 4、401/429/500 错误分类 3）；应用正常启动；集成实测 40min 无进展 PENDING 被启动对账置 FAILED（文案正确）、新鲜 PENDING 经一个定时周期不被误杀；前端 `vite build` 通过；测试任务 31/32 已删除

## 代码审查第四批：社区数据一致性 + AI 会话归属（✅ 2026-10-12 完成，修复计划第 13–17 项）

> 对应 `docs/bug优化/代码审查修复计划.md`（仅本地、不入库）第四批；契约变更见接口文档 §5.2–§5.6、§11.1。

- [x] **13. 删帖级联补全**：`PostServiceImpl.deletePost` 先收集该帖全部评论 id → 删 `t_comment_like` → 删评论 → 删帖子点赞 → 删帖子本体，全程 `@Transactional(rollbackFor=Exception.class)`；历史 2 条孤儿评论点赞（id 1、4）已清理
- [x] **14. 删评论级联 + 计数下界**：`delComment` 按 `parent_id` 精确收集子回复（排查期曾漏 where，已补上回归），合并本体 id 后删子回复点赞与回复本体；`comment_count` 改 `GREATEST(comment_count - n, 0)` 原子回减
- [x] **15. 点赞并发幂等**：帖子/评论点赞 insert 捕获 `DuplicateKeyException` 按"已赞"幂等返回当前状态，不抛 500；两个 toggle 方法加 `@Transactional`；帖子取消点赞计数回减同样 `GREATEST` 兜底
- [x] **16. 空白评论绕过 + 分页边界**：回复接口加 `@Valid`，评论/回复/发帖统一"文字非空白 或 图片非空"双层校验（`"   "+[]` 返回 400）；帖子列表 pageNum/pageSize 控制器夹紧（1–50，缺省 20）+ `MybatisPlusConfig` 分页插件 `setMaxLimit(50L)` 全局兜底
- [x] **17. AI 会话 IDOR**：`AiChatServiceImpl.chat` 服务端组装 memoryId `{userId}:{conversationId}`（Redis 实际键 `ai:memory:{userId}:{uuid}`，前端只见原始 UUID）；conversationId 严格 UUID 正则白名单，非法 400「非法会话标识」
- 实测：级联删除三表无残留；并发点赞无 500、计数准确；pageSize=99999 实际 50；空白评论 400；A 用户传 B 的 conversationId 读不到历史。测试用户/帖子/评论/点赞/Redis key 已全部清理。

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
- [x] 4. **FloorPlanAnalysisService + ZhipuFloorPlanAnalysisServiceImpl**：注入 LangChain4j `ChatModel`（SystemMessage 固定 JSON 契约 + UserMessage(TextContent+ImageContent)）；户型图为 OSS 公网 URL，直接 `ImageContent.from(URI)` 交模型下载（2026-10-14 存储强制 OSS 后，原 local 读盘 base64 分支已移除）；剥离 ```json 围栏后 Jackson 反序列化；校验规范化（id 蛇形唯一/connects 去噪+双向闭合/doorSides 对齐/bbox 裁剪/房间数截断/entryRoomId 兜底）
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
- [x] **对象存储（阿里云 OSS，唯一存储后端）**：`FileStorageService` 抽象 + 模板方法基类 `AbstractFileStorageService`（校验/命名/MIME/远程下载），唯一实现 `OssFileStorageServiceImpl` 无条件装配；单例 `OSSClient`（@PostConstruct fail-fast + @PreDestroy 关连接池）、显式 Content-Type 与一年缓存头。本地磁盘实现与 `/files/**` 静态映射已移除，密钥缺失即启动失败。2026-10-14：历史本地文件（84 个）已一次性迁移至 OSS 并改写数据库 URL；实测上传/公网读 200
- [ ] **CDN 加速域名**：OSS 已通，后续在其前面挂 CDN，仅需替换 `app.storage.oss.public-base-url`

## 互踢登录与 AI 会话历史上云（A-12 / A-13，2026-10-19 详设）

> 起因（2026-10-19 实测发现）：①同一账号可在两个浏览器同时登录——JWT 无状态，已签发 token 互不感知；②换浏览器后 AI 历史会话"消失"——历史只存 localStorage 按浏览器隔离，云端 Redis 记忆完好却无任何找回入口（当日已用手写 rescue 脚本应急找回）。
> 决策：P1 互踢登录（A-12）+ P3 历史持久化上云（A-13）双管齐下；轻量 Redis 找回方案（P2）因记忆 24h TTL 不做（历史会再"消失"）。

### A-12 同账号单端互踢（✅ 2026-10-19 编码完成，curl 实测六项全过）

- [x] 1. Redis key：`login:active:{userId}` = SHA-256(token) 十六进制指纹，TTL = token 剩余有效期（≤7 天）；摘要算法复用 `TokenBlackListService.sha256Hex`（public static，抽 Util 留作后续重构）
- [x] 2. 签发点改造：`LoginServiceImpl` 三处签发（login / loginByPhone / loginByEmail）抽 `issueToken(user)` 私有方法：生成 token → 写 active key（覆盖=后登录踢先登录）→ 返回
- [x] 3. 拦截器校验：`JwtInterceptor` 黑名单检查之后追加——active key **存在且 ≠ 当前 token 摘要** → 401「账号已在其他地方登录，请重新登录」（文案 2026-10-19 用户定版）；**key 不存在直接放行**（兼容存量 token 与 Redis 清空场景，避免上线即全员掉线）
- [x] 4. fail-open：active key 查询异常时放行 + error 日志（与黑名单同级处理，可用性优先，互踢属增强安全而非硬依赖）
- [x] 5. 登出联动：`logout` 在 blacklist 之后 `DEL login:active:{userId}`（改密场景不改：token 已被拉黑，key 遗留无害，重新登录即覆盖）
- [x] 6. 前端配套：`request.js` 401 分支提示改为 `error.response.data.message || '登录已过期，请重新登录'`，后端被踢/过期/黑名单文案透出
- [x] 7. 实测（2026-10-19 部署栈 curl 六项）：A 登录写指纹 → B 登录覆盖 → A /me 401 契约文案 → B /me 200 → B 登出账本删除 → B 旧 token 401 黑名单；双浏览器 UX 确认待用户走查

### A-13 AI 会话历史持久化（跨浏览器一致）

**表**（见数据库设计 §8/§9）：`t_ai_session` 会话元数据（UK(user_id, conversation_id) 防 IDOR + 并发重复建档）、`t_ai_message` 消息正文（images JSON 数组，参照 PostComment 的 JacksonTypeHandler 先例）。

- [ ] 1. DDL：`house_design.sql` 追加两表；存量库（dev 3306 / deploy 数据卷）执行 CREATE TABLE IF NOT EXISTS
- [ ] 2. 实体/Mapper：AiChatSession、AiChatMessage（MyBatis-Plus，role 用 String 非枚举存储）
- [ ] 3. 写入服务：新增 `Service/AI/AiChatHistoryService.saveTurn(userId, conversationId, question, images, answer)`，@Transactional——session upsert（首次 title=首条提问前 20 字，uk 撞键捕获 DuplicateKeyException 重查，沿用 register 并发兜底先例）+ 追加 user/assistant 两条消息；调用点 = `AiChatServiceImpl` 成功写 Redis 记忆之后（「失败不落库」契约延续到 DB）
- [ ] 4. 读接口：`GET /api/ai/sessions`（我的会话列表，updated_at 倒序，返回 conversationId/title/messageCount/updatedAt）+ `GET /api/ai/sessions/{conversationId}`（消息数组按 id 正序；非本人/不存在统一 404 防 IDOR；conversationId 非 UUID 400）
- [ ] 5. 前端：AiChat.vue 进页拉会话列表，与本地按 serverConversationId 合并——云端同 cid 覆盖本地消息（云端为权威落库），本地独有会话保留；localStorage 降级为缓存与离线兜底
- [ ] 6. 实测：账号在浏览器 A 聊两轮 → 浏览器 B 登录同账号（顺带验证 A-12 踢线）→ B 进 AI 页看到完整历史 → 清 localStorage 刷新后仍在 → 切历史会话续聊上下文接上（cid 一致）
- [ ] 7. 文档回填：接口文档 §7.2/§7.3、数据库设计 §8/§9、需求文档 A-12/A-13

**边界与不做**：
- 会话删除/重命名接口本期不做（列表 + 只读找回已覆盖当前诉求）
- 历史正文不参与模型记忆：模型上下文仍走 Redis 30 条窗口，DB 只做展示层持久化，两套数据各司其职
- 单账号历史量上限暂不设限（个人项目量级），后续可加分页

## AI-4 记忆分层·摘要版（2026-10-19 立项，🚧 待启动，依赖 A-13）

> 目标：跨会话"永久记忆"——模型不仅记得 Redis 30 条窗口，还通过摘要画像记住更早的偏好/结论。检索层（向量化 RAG）无限期缓，等出现"远期精确事实召回"需求再评估。
> 决策依据：摘要层覆盖跨会话记忆 80% 价值、20% 成本、零新增依赖；架构读写路径见 2026-10-19 会话（写入三管道：热窗口/全量/摘要；组装公式：[SYS] + summary + 检索(可选) + 窗口 + USER）。

**框架设计（启动时补全详设）**：

- 存储：`t_ai_session` 加 `summary LONGTEXT` 列 + 摘要水位字段（记录已压缩到的 message id）
- 触发：窗口淘汰发生时异步压缩被淘汰消息（每 N 轮触发一次，按水位增量，幂等可重试），不阻塞对话响应
- 组装：`AiChatServiceImpl` 组装公式加 summary 段——每轮从 DB 读 summary 拼在 [SYS] 之后、窗口之前；摘要失败不阻塞对话（静默降级为无画像）
- 红利顺带：Redis 冷启动容灾——热窗口丢失时从 `t_ai_message` 回放最近 30 条重建（24h TTL 不再是记忆上限）
- 明确不做：向量化检索（依赖 embedding 模型 + 向量存储，独立立项）、把模型记忆本体搬进 DB（展示与上下文两层各司其职）

**前置**：A-13 完成（t_ai_message 全量落库是摘要的水位与压缩对象）。
