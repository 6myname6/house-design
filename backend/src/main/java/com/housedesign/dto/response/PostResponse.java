package com.housedesign.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private List<String> images;
    private Integer likeCount;
    private Integer commentCount;
    private boolean likedByMe;
    @JsonFormat
    private LocalDateTime createdAt;
    private List<CommentResponse> comments;
}
