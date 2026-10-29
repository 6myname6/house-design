# 筑梦家 · AI Agent 与 RAG 升级方案

> 版本：v1.0（已确认，待实施）
> 日期：2026-10-26
> 目标岗位：大模型应用开发工程师（RAG / Agent / 工具调用 / 业务集成）
> 实施原则：文档先行，经确认后再按阶段写代码；每阶段独立可验收，不做一次性大改。

---

## 1. 背景与目标

### 1.1 现状

当前 AI 能力集中在「多模态问答」与「户型图 → 效果图生成」两条链路：

| 能力 | 现状 | 代码位置 |
| --- | --- | --- |
| 模型接入 | LangChain4j 1.0.1-beta6，原生 `ChatModel` 直连百炼 Qwen3-VL-Plus | `backend/src/main/java/com/housedesign/Service/AI/impl/AiChatServiceImpl.java` |
| 会话记忆 | 自研 `RedisChatMemoryStore`，30 条滑动窗口 + 24h TTL | `backend/src/main/java/com/housedesign/common/RedisChatMemoryStore.java` |
| 对话接口 | `POST /api/ai/chat`，同步返回整段文本，无流式 | `backend/src/main/java/com/housedesign/controller/AiChatController.java` |
| 生成链路 | 户型识别 → 逐房间生图 → photo-tour，异步状态机 | `backend/src/main/java/com/housedesign/Service/impl/GenerationServiceImpl.java` |
| 检索增强 | 无。模型只依赖参数与系统提示词 | — |
| 工具调用 | 无。业务操作全部由前端按钮触发 | — |
| 评测/可观测 | 仅应用日志 + Actuator health | — |

### 1.2 岗位能力映射

| 岗位关键词 | 本项目当前 | 升级后目标 |
| --- | --- | --- |
| RAG | 0 | 文档入库 → 分块 → Embedding → 混合检索 → Rerank → 引用溯源 → 离线评测 |
| Agent | 0 | 工具注册 + 多步编排 + 人工确认 + 审计 |
| 工具调用 | 0 | Function Calling / JSON Schema 校验 / 权限与幂等 |
| 业务集成 | 弱（AI 与业务并列） | AI 直接驱动项目、报价、生成、预约等业务动作 |
| 工程化 | 强 | 保持并复用现有异步、Redis、JWT、Docker 能力 |

### 1.3 设计原则

1. **复用优先**：Agent 工具直接调用现有 Service，不新建一套业务逻辑。
2. **可重建索引**：MySQL 是分块文本的权威存储，向量库是可随时重建的二级索引。
3. **失败可观测**：每次检索、工具调用、模型调用都要有可追踪记录。
4. **安全内建**：用户身份只从 `UserContext` 取，绝不信任模型输出的 `userId`。
5. **不超前设计**：一期不上多智能体、不上图数据库、不上独立向量集群。

### 1.4 已确认决策（2026-10-26）

| # | 决策项 | 结论 |
| --- | --- | --- |
| 1 | 向量存储 | 接受新增 PostgreSQL 16 + pgvector 服务，部署由五服务变六服务 |
| 2 | 推进顺序 | 按「RAG MVP → 混合检索 → Agent → 评测/流式 → 业务闭环」推进 |
| 3 | 业务集成 | 阶段五纳入本期范围（报价估算、BOM、预约量房） |

> 实施约定：后端业务代码由本人编写，助手提供设计、关键片段与代码审查；基础设施与配置（compose / 环境变量 / 依赖）由助手直接落地。

---

## 2. 总体架构

```
                    ┌────────────────────────────────────────────┐
                    │                Vue 3 前端                   │
                    │  对话（引用卡片 / 工具执行过程 / 确认弹窗）    │
                    └───────────────┬────────────────────────────┘
                                    │ /api
                    ┌───────────────▼────────────────────────────┐
                    │              Spring Boot 3                 │
                    │                                            │
                    │  ┌──────────────┐   ┌───────────────────┐  │
                    │  │ Agent 编排器  │──▶│  Tool Registry    │  │
                    │  │ 多步/超时/审计│   │ 项目/报价/生成/预约 │  │
                    │  └──────┬───────┘   └─────────┬─────────┘  │
                    │         │                     │            │
                    │  ┌──────▼───────┐   ┌─────────▼─────────┐  │
                    │  │ RAG 检索服务  │   │ 既有业务 Service   │  │
                    │  │ 混合+重排+引用│   │ 项目/生成/社区     │  │
                    │  └──────┬───────┘   └───────────────────┘  │
                    └─────────┼──────────────────────────────────┘
                              │
        ┌─────────────────────┼──────────────────────┐
        │                     │                      │
┌───────▼──────┐   ┌──────────▼─────────┐   ┌────────▼────────┐
│ MySQL 8      │   │ PostgreSQL+pgvector│   │ 阿里云百炼       │
│ 文档/分块/审计│   │ chunk 向量索引      │   │ Embedding/Rerank│
│ FULLTEXT     │   │ HNSW cosine        │   │ Chat/生图        │
└──────────────┘   └────────────────────┘   └─────────────────┘
```

### 2.1 模块划分

| 模块 | 包路径建议 | 职责 |
| --- | --- | --- |
| 知识库 | `Service/kb`、`controller/KnowledgeBaseController` | 文档管理、入库任务、重建索引 |
| RAG | `Service/rag` | 查询改写、混合检索、Rerank、上下文组装 |
| Agent | `Service/agent` | 工具注册、编排循环、确认、审计 |
| 评测 | `test/java/.../eval`、`docs/eval` | 离线评测集与指标计算 |
| 可观测 | `common/llm` | trace 记录、token/成本统计 |

---

## 3. RAG 设计

### 3.1 知识库内容范围

一期只做「装修领域公开知识」：

- 装修风格图鉴（现代、侘寂、法式、原木、奶油风等）
- 材料与工艺标准（瓷砖、地板、涂料、水电、防水）
- 施工流程与验收节点
- 报价区间与常见增项
- 避坑指南与平台规则

用户私有资料（户型图、合同、报价单）作为二期能力，按 `visibility` 隔离。

### 3.2 入库链路

```
上传/录入 → OSS 存原文件 → t_kb_document(PENDING)
   → 异步任务：解析(Tika) → 清洗 → 结构化分块 → Embedding(批量)
   → 写 t_kb_chunk(MySQL) + kb_chunk_vector(PG)
   → 状态 READY / FAILED（失败保留错误信息，可重试）
```

关键设计：

| 项 | 方案 | 理由 |
| --- | --- | --- |
| 解析 | Apache Tika（PDF / Word / Markdown / HTML） | 统一入口，避免每种格式写一套 |
| 分块 | 结构感知：按标题层级切，目标 500–800 token，overlap 80 | 保留章节语义，避免把一条工艺说明切断 |
| 元数据 | `doc_id`、`version`、`chunk_no`、`heading`、`source`、`visibility`、`owner_user_id` | 支撑过滤、引用与权限 |
| Embedding | 百炼 `text-embedding-v3`，维度配置化（默认 1024），批量 25 条/次 | 与现有百炼 Key 复用，成本可控 |
| 幂等 | `content_hash` 去重；重建索引时新版本 `is_active=1`、旧版本置 0 | 重复上传不产生重复召回 |
| 失败处理 | 状态机 + 错误信息 + 手动重试；启动时对账卡在中间态的任务 | 与现有生成链路一致的处理范式 |

### 3.3 检索链路

```
用户问题
  →（可选）多轮 Query 改写：结合最近 3 轮对话补全指代
  → 向量检索：pgvector cosine Top 20
  → 关键词检索：MySQL FULLTEXT（ngram 分词）Top 20
  → RRF 融合
  → Rerank（百炼 gte-rerank）Top 5
  → 相似度阈值过滤（低于阈值直接判“无命中”）
  → 组装上下文（带 [S1]…[S5] 编号）
  → 模型生成（要求逐句引用；无依据时明确拒答）
  → 返回 answer + citations[]
```

要点：

1. **混合检索**：纯向量对“防水高度 1.8 米”这类精确词不敏感，关键词检索补齐。
2. **Rerank 必须有对照数据**：记录开启/关闭时的 Recall@5，否则无法证明价值。
3. **引用溯源**：`citations` 返回 `documentId / title / chunkId / heading / snippet`，前端可定位原文。
4. **无命中策略**：相似度低于阈值时不调用模型扩写，直接返回“知识库暂未收录”，避免编造。
5. **权限过滤在检索层完成**：SQL/向量查询都带 `visibility=PUBLIC OR owner_user_id=当前用户`，不能只在展示层过滤。

### 3.4 二期增强（不在一期范围）

- 父子块：小块检索、大块入模，兼顾召回精度与上下文完整性。
- 语义缓存：相同问题的向量近似命中，直接复用答案，降低 token 成本。
- 文档版本对比与增量索引：只重算变更章节。
- Query 多路扩展（同义改写、风格词归一）。

### 3.5 评测方案

| 项 | 方案 |
| --- | --- |
| 评测集 | `docs/eval/rag_golden_set.jsonl`，30–50 条，覆盖风格/材料/工艺/报价/多轮指代 |
| 检索指标 | Recall@5、MRR |
| 生成指标 | Faithfulness（答案是否只依据检索内容）、Answer Relevancy |
| 判定方式 | LLM-as-judge + 人工抽检 20%；固定评分 rubric，避免口径漂移 |
| 回归 | 每次改分块、Embedding、Rerank、Prompt 后必须跑一遍并记录 |

验收线（一期）：Recall@5 ≥ 0.85，Faithfulness ≥ 0.90，无命中场景拒答率 100%。

---

## 4. Agent 设计

### 4.1 定位

把「问问设计师」从**问答**升级为**能执行动作的设计助手**。用户说“把客厅改成侘寂风，预算控制在 15 万，然后生成效果图”，Agent 应完成：查项目 → 改风格 → 算预算 → 触发生成 → 返回任务 ID。

### 4.2 工具清单（v1）

| 工具 | 参数 | 类型 | 复用 |
| --- | --- | --- | --- |
| `search_knowledge` | query, topK | 只读 | RAG 检索服务 |
| `get_project` | projectId | 只读 | DesignProjectService |
| `update_project_style` | projectId, style | 写，需确认 | DesignProjectService |
| `estimate_budget` | area, style, city, level | 只读 | 新增规则引擎 |
| `trigger_generation` | projectId | 写，需确认、幂等 | GenerationService |
| `get_generation_status` | taskId | 只读 | GenerationService |
| `create_site_visit` | name, phone, city, preferredTime | 写，需确认 | 新增线索服务 |

### 4.3 编排方式

一期采用**自研轻量 ReAct 循环**，不引入第二套框架：

```
ChatRequest(messages + toolSpecifications)
  → 模型返回 toolExecutionRequests
  → 参数 JSON Schema 校验 + 权限校验
  → 执行工具（超时、异常捕获）
  → 追加 ToolExecutionResultMessage
  → 继续下一轮（最多 5 步 / token 预算上限）
  → 模型输出最终答案
```

理由：现有对话已从 `@AiService` 改为原生 `ChatModel`（因为 1.0.1-beta6 的 AiService 不支持图片参数），继续用原生工具循环最一致，也便于逐层加审计与限流。

后续若要图形化编排，再评估 Spring AI Alibaba Graph；**不同时引入两套 AI 框架**。

### 4.4 安全与可靠性（面试重点）

| 风险 | 措施 |
| --- | --- |
| 越权 | 工具执行前从 `UserContext` 取 userId；所有写操作走既有归属校验（不存在/越权统一 404） |
| 模型伪造身份 | 工具参数中不暴露 userId；即使模型传入也忽略 |
| 误操作 | 写工具默认返回 `NEED_CONFIRM` + 一次性确认 token，前端确认后才执行 |
| 重复执行 | 生成类工具使用幂等键（`projectId + userConfirmedAt`）并复用 Redis 占位 |
| 死循环 | 最大步数 5、单步超时 30s、总 token 预算上限 |
| 工具失败 | 失败结果回传给模型让其自行降级；连续失败 2 次终止并给出可读原因 |
| 审计 | 每次 run / step 落库，含参数摘要、耗时、token、结果状态 |

### 4.5 Agent 数据流

```
POST /api/ai/agent/chat
  → 创建 t_agent_run(RUNNING)
  → 编排循环（每步写 t_agent_step）
  → 成功：answer + traceId + citations
  → 需确认：status=NEED_CONFIRM + pendingAction + confirmToken
POST /api/ai/agent/confirm
  → 校验 token（一次性、5 分钟有效、绑定 userId）
  → 执行工具 → 继续编排
```

---

## 5. 业务集成设计

一期先把“AI 输出 → 业务数据”打通，避免停留在聊天层：

| 能力 | 说明 | 表 |
| --- | --- | --- |
| 报价估算 | 面积 × 风格单价 × 城市系数 × 档次系数 + 增项规则；结果结构化落库 | `t_quote` / `t_quote_item` |
| 材料清单（BOM） | 按房间与工艺生成主材/辅材清单，可导出 | `t_quote_item` |
| 预约量房 | Agent 收集信息后创建线索，进入跟进状态机 | `t_site_visit` |
| 生成联动 | Agent 触发既有异步生成，返回 taskId 并轮询 | 复用 `t_generated_model` |

报价单导出 PDF 作为二期，一期先返回结构化 JSON 并由前端展示。

---

## 6. 数据模型（草案）

> 沿用现有规范：逻辑外键、`created_at/updated_at`、`CREATE TABLE IF NOT EXISTS`，存量库手工 ALTER。

### 6.1 MySQL 新表

```sql
-- 知识库文档（权威元数据）
CREATE TABLE IF NOT EXISTS `t_kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `source_type` VARCHAR(16) NOT NULL COMMENT 'FILE/URL/TEXT',
  `file_name` VARCHAR(255) NULL,
  `file_ext` VARCHAR(16) NULL,
  `oss_url` VARCHAR(512) NULL,
  `checksum` CHAR(64) NULL COMMENT 'SHA-256，去重',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE',
  `owner_user_id` BIGINT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARSING/INDEXING/READY/FAILED',
  `error_message` VARCHAR(1024) NULL,
  `version` INT NOT NULL DEFAULT 1,
  `chunk_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_owner_visibility` (`owner_user_id`, `visibility`),
  KEY `idx_checksum` (`checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库分块（权威文本，向量可重建）
CREATE TABLE IF NOT EXISTS `t_kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `document_id` BIGINT NOT NULL,
  `version` INT NOT NULL,
  `chunk_no` INT NOT NULL,
  `heading` VARCHAR(255) NULL,
  `content` TEXT NOT NULL,
  `content_hash` CHAR(64) NOT NULL,
  `token_count` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_ver_chunk` (`document_id`, `version`, `chunk_no`),
  KEY `idx_doc_active` (`document_id`, `is_active`),
  FULLTEXT KEY `ft_content` (`heading`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Agent 运行与步骤
CREATE TABLE IF NOT EXISTS `t_agent_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `conversation_id` VARCHAR(64) NULL,
  `goal` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED/NEED_CONFIRM',
  `step_count` INT NOT NULL DEFAULT 0,
  `total_tokens` INT NOT NULL DEFAULT 0,
  `latency_ms` INT NOT NULL DEFAULT 0,
  `error_message` VARCHAR(1024) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_agent_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT NOT NULL,
  `step_no` INT NOT NULL,
  `type` VARCHAR(16) NOT NULL COMMENT 'LLM/TOOL',
  `tool_name` VARCHAR(64) NULL,
  `arguments` JSON NULL,
  `result_summary` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL COMMENT 'OK/FAILED/BLOCKED',
  `latency_ms` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_step` (`run_id`, `step_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.2 PostgreSQL（pgvector）

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kb_chunk_vector (
  chunk_id BIGINT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  version INT NOT NULL,
  embedding vector(1024) NOT NULL,
  model VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_kb_vec_hnsw ON kb_chunk_vector
  USING hnsw (embedding vector_cosine_ops);
```

> 约定：MySQL 为准，PG 只存向量；重建索引时按 `document_id + version` 覆盖写入，删除时同步删除。

### 6.3 业务表（二期）

`t_quote`、`t_quote_item`、`t_site_visit` 在进入「业务集成」阶段前单独评审，避免提前建表。

---

## 7. 接口设计（草案）

> 全部需要 JWT，身份从 `UserContext` 取；错误语义沿用 400/401/404/409/503。

| 方法 | 路径 | 说明 | 阶段 |
| --- | --- | --- | --- |
| POST | `/api/kb/documents` | 上传/录入文档，创建入库任务 | 一 |
| GET | `/api/kb/documents` | 文档列表（含状态、分块数） | 一 |
| GET | `/api/kb/documents/{id}` | 文档详情 | 一 |
| POST | `/api/kb/documents/{id}/reindex` | 重建索引 | 一 |
| DELETE | `/api/kb/documents/{id}` | 删除文档（软删 + 向量清理） | 一 |
| POST | `/api/ai/rag/search` | 检索调试接口（返回融合前后结果） | 一 |
| POST | `/api/ai/rag/chat` | 基于知识库问答（带引用） | 一 |
| POST | `/api/ai/agent/chat` | Agent 对话（可触发工具） | 三 |
| POST | `/api/ai/agent/confirm` | 确认执行写操作 | 三 |
| GET | `/api/ai/agent/runs/{id}` | 查询执行轨迹（审计/展示） | 三 |
| GET | `/api/ai/chat/stream` | SSE 流式对话 | 四 |
| POST | `/api/quote/preview` | 报价预估 | 五 |
| POST | `/api/site-visits` | 预约量房 | 五 |

兼容策略：`POST /api/ai/chat` 保持不变；RAG 与 Agent 使用新端点，前端逐步切换。

---

## 8. 配置项（草案）

```yaml
app:
  rag:
    chunk-size: 700
    chunk-overlap: 80
    vector-top-k: 20
    keyword-top-k: 20
    rerank-top-n: 5
    min-score: 0.35
  embedding:
    model: text-embedding-v3
    dimension: 1024
    batch-size: 25
  rerank:
    model: gte-rerank
    timeout-seconds: 10
  agent:
    max-steps: 5
    step-timeout-seconds: 30
    total-token-budget: 12000
    confirm-token-ttl-seconds: 300
```

敏感项继续走环境变量，不落库。

---

## 9. 分阶段实施与验收

> **进度快照（最近更新）**
>
> | 项 | 状态 | 产出 |
> | --- | --- | --- |
> | PostgreSQL + pgvector 接入 | 已完成 | 双数据源配置（`@Primary` 主库 + `pgVectorDataSource`）、`kb_vector` 库、`kb_chunk_vector` 表（HNSW 余弦索引） |
> | 知识库建表 | 已完成 | `t_kb_document`、`t_kb_chunk`（含 `FULLTEXT ... WITH PARSER ngram`） |
> | 业务数据表 | 已完成 | `t_material`、`t_price_rule`（`price_tier` 三档支撑报价区间） |
> | 知识库语料 | 已完成 | `docs/kb-corpus/`：10 篇风格卡 + 住宅设计规范条目 + 材料与施工工艺 |
> | 实体与 Mapper | 已完成 | `KbDocument` / `KbChunk` / 三个枚举 + 两个 Mapper（含分块批量插入） |
> | 入库链路（第 3 步） | 待开发 | 文档创建接口 + 异步状态机 + Tika 解析 |
> | 切块与向量化 | 待开发 | 结构感知分块、Embedding 调用、向量写入 |
> | 检索接口 | 待开发 | 向量检索、阈值拒答、引用返回 |
>
> 阶段二至阶段五均未开始。

### 阶段一：RAG MVP

范围：文档管理、入库、纯向量检索、引用返回、评测集 v1。

验收：

- [ ] 上传 PDF/Markdown 后状态可从 PENDING 流转到 READY；失败可见原因并可重试。
- [ ] 同一文档重复上传不产生重复召回（checksum/版本生效）。
- [ ] 问答返回 `citations`，前端可定位到文档标题与章节。
- [ ] 30 条评测集 Recall@5 ≥ 0.85；无命中时拒答率 100%。
- [ ] 入库任务进程重启后可对账收口，不永久卡中间态。

### 阶段二：混合检索 + Rerank

范围：MySQL FULLTEXT、RRF 融合、Rerank、阈值策略。

验收：

- [ ] 同一评测集 Recall@5 相比纯向量有提升，并记录对照表。
- [ ] Rerank 超时可降级为融合结果，不影响主流程。
- [ ] 检索耗时 P95 在可接受范围（记录实测值，不设拍脑袋目标）。

### 阶段三：Agent 工具调用

范围：工具注册、ReAct 循环、写操作确认、审计、轨迹接口。

验收：

- [ ] 10 条多步任务脚本（如“改风格 + 估预算 + 生成”）成功率 ≥ 80%。
- [ ] 越权项目 ID 一律 404；模型伪造 userId 无效。
- [ ] 写操作必须确认后才执行；重复确认只执行一次。
- [ ] 超过最大步数/超时/预算时安全终止并返回可读原因。
- [ ] 每次 run 可在 `t_agent_run` / `t_agent_step` 中完整回溯。

### 阶段四：评测、可观测、流式

范围：Langfuse 或 OpenTelemetry trace、指标看板、SSE。

验收：

- [ ] 每次请求可看到 retrieval / rerank / LLM / tool 分段耗时与 token 成本。
- [ ] 指标：RAG 命中率、工具成功率、P95 延迟、单次成本。
- [ ] 前端流式输出可用，断流可重试且不重复落历史。

### 阶段五：业务闭环（已纳入本期）

范围：报价估算、BOM、预约量房、PDF 导出。

验收：

- [ ] Agent 可完成“估预算 → 生成报价单 → 预约量房”链路。
- [ ] 报价计算有单元测试覆盖系数与边界（面积、档次、城市）。
- [ ] 业务数据与 AI 输出可追溯（runId 关联报价单）。

---

## 10. 风险与备选方案

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 引入 PostgreSQL 增加运维复杂度 | 部署服务从 5 个变 6 个（已确认接受） | 仅用于向量索引，可随时重建；备份策略独立；本地 Docker Compose 一键起 |
| LangChain4j 1.0.1-beta6 工具 API 能力有限 | Agent 编排受限 | 先用手写循环 + `ToolSpecification`；若阻塞，升级 Spring Boot 3.4+ 与 LangChain4j 再评估 |
| 中文全文检索效果 | 关键词召回差 | MySQL ngram 分词；不够再评估 ES，不提前引入 |
| Embedding/Rerank 成本 | 入库与查询费用上升 | 批量 + 缓存 + 只对 Top20 重排；记录单次成本 |
| 评测主观性 | 指标不可信 | 固定 rubric、双模型交叉判定、人工抽检 |
| 范围膨胀 | 长期不落地 | 严格按阶段验收，一期不做多智能体与私有知识库 |

---

## 11. 简历映射（实施完成后）

项目标题：**筑梦家 —— AI 装修设计 Agent 平台（RAG + 多模态 + 工具调用）**

建议 bullets（每条都要有数字）：

1. **RAG 检索增强**：文档解析 → 结构感知分块 → Embedding → 向量 + 关键词混合检索 → RRF → Rerank → 引用溯源；30 条评测集 Recall@5 xx%、Faithfulness xx%。
2. **Agent 工具调用**：注册 7 个业务工具，ReAct 多步编排 + JSON Schema 校验 + 写操作二次确认 + 全链路审计；10 条多步任务成功率 xx%。
3. **多模态生成链路**：保留现有两阶段 Pipeline（压缩为 3 行内）。
4. **评测与可观测**：Langfuse trace 分段耗时与 token 成本；P95 xx ms、单次成本 xx 元。
5. **安全与一致性**：越权 404、身份只取服务端上下文、幂等与并发锁（压缩现有内容）。
6. **部署上线**：Docker Compose 六服务、healthcheck、优雅停机、每日备份（压缩现有内容）。

---

## 12. 实施顺序建议

1. 先做阶段一，拿到 Recall@5 基线数据（没有数据就没有后面的“提升”）。
2. 再做阶段二，用同一评测集证明混合检索 + Rerank 的增益。
3. 阶段三与阶段二可并行设计，但代码上先完成检索再接工具。
4. 阶段四在阶段三后做，否则 trace 字段会反复改。
5. 阶段五已纳入本期，但仍在阶段四后落地，避免业务表提前固化。

---

> 本文档只做设计，不含代码实现。确认范围与优先级后，再按阶段拆分任务并逐阶段落地。
