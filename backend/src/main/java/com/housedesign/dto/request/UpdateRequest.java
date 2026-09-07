package com.housedesign.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRequest {
    private String nickname;
    private String avatar;
}
