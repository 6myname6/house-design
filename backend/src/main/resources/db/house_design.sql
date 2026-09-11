-- ============================================================
-- 筑梦家 · 数据库初始化脚本（house_design 库全量建表）
-- 用法：启动后端前手动执行一次，或用任一 MySQL 客户端导入
--   mysql -uroot -p house_design < db/house_design.sql
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username`   VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `nickname`   VARCHAR(64)           DEFAULT NULL COMMENT '昵称，默认取 username',
    `avatar`     VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（不可更新）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- 设计项目表
CREATE TABLE IF NOT EXISTS `t_design_project`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目 ID',
    `user_id`           BIGINT       NOT NULL COMMENT '归属用户（逻辑外键 → t_user.id）',
    `name`              VARCHAR(128) NOT NULL COMMENT '项目名称',
    `description`       VARCHAR(512)          DEFAULT NULL COMMENT '项目描述',
    `style`             VARCHAR(512)          DEFAULT NULL COMMENT '用户自定义风格要求（自由文本，如「原木色、温馨、多绿植」）',
    `style_label`       VARCHAR(32)           DEFAULT NULL COMMENT '预设风格 code（DesignStyle，如 modern-minimalist）',
    `design_image_path` VARCHAR(512)          DEFAULT NULL COMMENT '设计图相对路径（storage 下）',
    `design_image_url`  VARCHAR(512)          DEFAULT NULL COMMENT '设计图可访问 URL',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='设计项目表';

-- AI 生成任务表
CREATE TABLE IF NOT EXISTS `t_generated_model`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    `project_id`        BIGINT       NOT NULL COMMENT '所属项目（逻辑外键 → t_design_project.id）',
    `user_id`           BIGINT       NOT NULL COMMENT '发起用户（逻辑外键 → t_user.id）',
    `status`            VARCHAR(16)  NOT NULL COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    `provider`          VARCHAR(32)           DEFAULT NULL COMMENT 'AI 提供方（mock/zhipu）',
    `model_url`         VARCHAR(512)          DEFAULT NULL COMMENT '3D 模型（glb/gltf）URL',
    `preview_image_url` VARCHAR(512)          DEFAULT NULL COMMENT '预览缩略图 URL',
    `panorama_url`      VARCHAR(512)          DEFAULT NULL COMMENT '全景图 URL（智谱出图时填充）',
    `scene_config`      LONGTEXT              DEFAULT NULL COMMENT '3D 场景 JSON（程序化渲染用）',
    `error_message`     VARCHAR(1024)         DEFAULT NULL COMMENT '失败原因',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI 生成任务表';

-- 帖子表
CREATE TABLE IF NOT EXISTS `t_post`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
    `user_id`       BIGINT       NOT NULL COMMENT '作者（逻辑外键 → t_user.id）',
    `content`       TEXT                  DEFAULT NULL COMMENT '帖子正文',
    `images`        TEXT                  DEFAULT NULL COMMENT '图片 URL 列表（JSON 字符串 ↔ List<String>）',
    `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数器）',
    `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数（冗余计数器）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子表';

-- 帖子评论表（两级：parent_id NULL=顶层评论，非NULL=回复某条评论）
CREATE TABLE IF NOT EXISTS `t_post_comment`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
    `post_id`    BIGINT       NOT NULL COMMENT '所属帖子（逻辑外键 → t_post.id）',
    `user_id`    BIGINT       NOT NULL COMMENT '评论者（逻辑外键 → t_user.id）',
    `parent_id`  BIGINT                DEFAULT NULL COMMENT '被回复的评论 ID（NULL=顶层评论；非NULL=该评论的回复）',
    `content`    TEXT                  DEFAULT NULL COMMENT '评论文字（与 images 至少填一个）',
    `images`     TEXT                  DEFAULT NULL COMMENT '评论图片 URL 列表（JSON 字符串 ↔ List<String>）',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子评论表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `t_post_like`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`    BIGINT   NOT NULL COMMENT '被赞帖子（逻辑外键 → t_post.id）',
    `user_id`    BIGINT   NOT NULL COMMENT '点赞用户（逻辑外键 → t_user.id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子点赞表';

-- 评论点赞表（独立于帖子点赞，仅存评论的点赞）
CREATE TABLE IF NOT EXISTS `t_comment_like`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id` BIGINT   NOT NULL COMMENT '被赞评论（逻辑外键 → t_post_comment.id）',
    `user_id`    BIGINT   NOT NULL COMMENT '点赞用户（逻辑外键 → t_user.id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评论点赞表';
