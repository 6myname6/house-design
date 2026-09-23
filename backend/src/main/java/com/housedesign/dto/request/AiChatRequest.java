package com.housedesign.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequest {

    // 会话标识(首次可为空)
    private String conversationId;
    // 用户提的问
    private String question;
    // 用户传的图
    private List<String> images;
}
