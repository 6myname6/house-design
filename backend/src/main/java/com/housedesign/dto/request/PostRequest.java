package com.housedesign.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    @Size(max = 2000, message = "内容不能超过2000字")
    private String content;
    @Size(max = 9, message = "最多上传9张照片")
    private List<String> images;
}
