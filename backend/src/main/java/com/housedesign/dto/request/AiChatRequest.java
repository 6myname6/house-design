package com.housedesign.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequest {

    // 会话标识(首次可为空)
    @Size(max = 64, message = "会话标识非法")
    private String conversationId;
    // 用户提的问
    @Size(max = 2000, message = "输入问题不能超过2000字")
    private String question;
    // 用户传的图
    @Size(max = 4, message = "最多上传4张图片")
    private List<String> images;
}
