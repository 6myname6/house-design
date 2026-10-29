-- ============================================================
-- 筑梦家 · 数据库初始化脚本（house_design 库全量建表）
-- 用法：启动后端前手动执行一次，或用任一 MySQL 客户端导入
--   mysql -uroot -p house_design < db/house_design.sql
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名；邮箱注册时由用户填写，3-32 字符',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱（A-11 邮箱注册/登录）',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '密码（BCrypt 加密存储）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称，默认取 username',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（不可更新）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 设计项目表
CREATE TABLE IF NOT EXISTS `t_design_project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目 ID',
    `user_id` BIGINT NOT NULL COMMENT '归属用户（逻辑外键 → t_user.id）',
    `name` VARCHAR(128) NOT NULL COMMENT '项目名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '项目描述',
    `style` VARCHAR(512) DEFAULT NULL COMMENT '用户自定义风格要求（自由文本，如「原木色、温馨、多绿植」）',
    `style_label` VARCHAR(32) DEFAULT NULL COMMENT '预设风格 code（DesignStyle，如 modern-minimalist）',
    `design_image_path` VARCHAR(512) DEFAULT NULL COMMENT '设计图相对路径（storage 下）',
    `design_image_url` VARCHAR(512) DEFAULT NULL COMMENT '设计图可访问 URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '设计项目表';

-- AI 生成任务表
CREATE TABLE IF NOT EXISTS `t_generated_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    `project_id` BIGINT NOT NULL COMMENT '所属项目（逻辑外键 → t_design_project.id）',
    `user_id` BIGINT NOT NULL COMMENT '发起用户（逻辑外键 → t_user.id）',
    `status` VARCHAR(16) NOT NULL COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    `provider` VARCHAR(32) DEFAULT NULL COMMENT 'AI 提供方（mock/zhipu）',
    `model_url` VARCHAR(512) DEFAULT NULL COMMENT '3D 模型（glb/gltf）URL',
    `preview_image_url` VARCHAR(512) DEFAULT NULL COMMENT '预览缩略图 URL',
    `panorama_url` VARCHAR(512) DEFAULT NULL COMMENT '全景图 URL（智谱出图时填充）',
    `scene_config` LONGTEXT DEFAULT NULL COMMENT '3D 场景 JSON（程序化渲染用）',
    `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI 生成任务表';

-- 帖子表
CREATE TABLE IF NOT EXISTS `t_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
    `user_id` BIGINT NOT NULL COMMENT '作者（逻辑外键 → t_user.id）',
    `content` TEXT DEFAULT NULL COMMENT '帖子正文',
    `images` TEXT DEFAULT NULL COMMENT '图片 URL 列表（JSON 字符串 ↔ List<String>）',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数器）',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数（冗余计数器）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子表';

-- 帖子评论表（两级：parent_id NULL=顶层评论，非NULL=回复某条评论）
CREATE TABLE IF NOT EXISTS `t_post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
    `post_id` BIGINT NOT NULL COMMENT '所属帖子（逻辑外键 → t_post.id）',
    `user_id` BIGINT NOT NULL COMMENT '评论者（逻辑外键 → t_user.id）',
    `parent_id` BIGINT DEFAULT NULL COMMENT '被回复的评论 ID（NULL=顶层评论；非NULL=该评论的回复）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复评论的作者（逻辑外键 → t_user.id；前端据此显示"回复 xxx"，顶层评论为 NULL）',
    `content` TEXT DEFAULT NULL COMMENT '评论文字（与 images 至少填一个）',
    `images` TEXT DEFAULT NULL COMMENT '评论图片 URL 列表（JSON 字符串 ↔ List<String>）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子评论表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `t_post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id` BIGINT NOT NULL COMMENT '被赞帖子（逻辑外键 → t_post.id）',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户（逻辑外键 → t_user.id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子点赞表';

-- 评论点赞表（独立于帖子点赞，仅存评论的点赞）
CREATE TABLE IF NOT EXISTS `t_comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id` BIGINT NOT NULL COMMENT '被赞评论（逻辑外键 → t_post_comment.id）',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户（逻辑外键 → t_user.id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论点赞表';

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `t_kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档 ID',
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `source_type` VARCHAR(16) NOT NULL DEFAULT 'FILE' COMMENT 'FILE/URL/TEXT',
    `file_name` VARCHAR(255) NULL COMMENT '原始文件名',
    `file_ext` VARCHAR(16) NULL COMMENT '扩展名',
    `oss_url` VARCHAR(512) NULL COMMENT '原文件 OSS 地址',
    `checksum` CHAR(64) NULL COMMENT 'SHA-256 十六进制，用于去重',
    `visibility` VARCHAR(16) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE',
    `owner_user_id` BIGINT NULL COMMENT '私有文档归属用户',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARSING/INDEXING/READY/FAILED',
    `error_message` VARCHAR(1024) NULL COMMENT '失败原因',
    `version` INT NOT NULL DEFAULT 1 COMMENT '文档版本，重建索引时递增',
    `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '当前版本分块数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_owner_visibility` (`owner_user_id`, `visibility`),
    KEY `idx_checksum` (`checksum`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库文档表';

-- 知识库分片表
CREATE TABLE IF NOT EXISTS `t_kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分块 ID',
    `document_id` BIGINT NOT NULL COMMENT '所属文档',
    `version` INT NOT NULL COMMENT '文档版本',
    `chunk_no` INT NOT NULL COMMENT '文档内序号，从 0 开始',
    `heading` VARCHAR(255) NULL COMMENT '所属章节标题',
    `content` TEXT NOT NULL COMMENT '分块正文',
    `content_hash` CHAR(64) NOT NULL COMMENT '分块内容 SHA-256，用于去重',
    `token_count` INT NOT NULL DEFAULT 0 COMMENT '估算 token 数',
    `char_count` INT NOT NULL DEFAULT 0 COMMENT '字符数',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1=当前版本有效，0=历史版本',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_ver_chunk` (
        `document_id`,
        `version`,
        `chunk_no`
    ),
    KEY `idx_doc_active` (`document_id`, `is_active`),
    KEY `idx_content_hash` (`content_hash`),
    FULLTEXT KEY `ft_content` (`heading`, `content`)
    WITH
        PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库分块表';

-- 材料库：给 Agent 工具查价用，不做向量化。
-- 价格是精确数字，必须整行命中，不能让模型"语义检索"出一个大概的价格。
CREATE TABLE IF NOT EXISTS `t_material` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '材料名称，如「强化复合地板 橡木纹」',
    `category` VARCHAR(32) NOT NULL COMMENT '品类：FLOOR/WALL/BOARD/PAINT/HARDWARE/LIGHT/SANITARY',
    `spec` VARCHAR(128) NULL COMMENT '规格，如「1220×2440×18mm」',
    `unit` VARCHAR(16) NOT NULL COMMENT '计价单位：㎡ / m / 片 / 套',
    `unit_price` DECIMAL(10, 2) NOT NULL COMMENT '参考单价（元，含税）',
    `brand` VARCHAR(64) NULL COMMENT '品牌',
    `eco_level` VARCHAR(16) NULL COMMENT '环保等级：E1/E0/ENF，仅板材类有效',
    `style_tags` VARCHAR(255) NULL COMMENT '适配风格，逗号分隔，如「北欧,日式」',
    `remark` VARCHAR(512) NULL COMMENT '备注：施工注意、适用场景',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1=可选用，0=已下架',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_active_price` (`is_active`, `unit_price`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '材料库表';

-- 计价规则表：施工工序的人工+辅料单价，报价工具按 code 精确取用。
-- price_tier 用于支撑「基础/中档/高档」三档估算，对应模糊预算下给区间而不是给单点。
CREATE TABLE IF NOT EXISTS `t_price_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '规则编码，工具按 code 精确匹配',
    `name` VARCHAR(128) NOT NULL COMMENT '规则名称，如「墙面乳胶漆（含腻子）」',
    `work_type` VARCHAR(32) NOT NULL COMMENT '工序：DEMOLITION/PLUMBING/ELECTRIC/TILE/CARPENTRY/PAINT/INSTALL/DESIGN',
    `calc_type` VARCHAR(16) NOT NULL COMMENT '计算方式：AREA 面积 / LENGTH 长度 / QUANTITY 件数 / FIXED 一口价',
    `unit` VARCHAR(16) NOT NULL COMMENT '计价单位：㎡ / m / 件 / 项',
    `unit_price` DECIMAL(10, 2) NOT NULL COMMENT '单价（元）',
    `price_tier` VARCHAR(16) NOT NULL DEFAULT 'STANDARD' COMMENT '档次：BASIC 基础 / STANDARD 中档 / PREMIUM 高档',
    `min_charge` DECIMAL(10, 2) NULL COMMENT '最低收费，小面积时不低于此价',
    `remark` VARCHAR(512) NULL COMMENT '备注：包含范围、不含项',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用，0=停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_tier` (`code`, `price_tier`),
    KEY `idx_work_type` (`work_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '计价规则表';