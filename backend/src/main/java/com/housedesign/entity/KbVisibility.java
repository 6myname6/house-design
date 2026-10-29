package com.housedesign.entity;

/**
 * 知识库文档可见范围（对应 t_kb_document.visibility，按枚举名存储）。
 */
public enum KbVisibility {

    /** 公开知识：所有登录用户可检索 */
    PUBLIC,

    /** 私有知识：仅 owner_user_id 本人可检索 */
    PRIVATE
}
