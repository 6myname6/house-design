package com.housedesign.dto.response;

import lombok.Data;

@Data
/**
 * 个人数据
 */
public class PersonalDataResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 掩码邮箱（如 3***@qq.com），未绑定为 null */
    private String maskedEmail;
}
