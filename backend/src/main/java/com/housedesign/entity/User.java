package com.housedesign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 t_user。
 * 表结构见 backend/src/main/resources/db/schema.sql。
 */
@Data
@TableName("t_user")
public class User {

    /** 用户 ID（PK，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt 加密存储，返回前端时需脱敏） */
    private String password;

    /** 昵称，默认取 username */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 创建时间（数据库 DEFAULT CURRENT_TIMESTAMP 自动生成，不可更新） */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
