package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.PostService;
import com.housedesign.common.Result;
import com.housedesign.dto.request.CommentRequest;
import com.housedesign.dto.response.CommentResponse;
import com.housedesign.dto.response.LikeResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/comments")
@RequiredArgsConstructor
@RestController
@Slf4j
// 评论Controller（评论点赞 / 回复评论）
public class CommentController {
    private final PostService postService;

    // 评论点赞 / 取消点赞（切换）
    @PostMapping("/{id}/like")
    public Result<LikeResult> toggleLike(@PathVariable("id") Long commentId) {
        return Result.success(postService.toggleCommentLike(commentId));
    }

    // 回复评论（评论的评论）
    @PostMapping("/{id}/replies")
    public Result<CommentResponse> reply(@PathVariable("id") Long commentId,
            @RequestBody CommentRequest request) {
        return Result.success(postService.replyComment(commentId, request));
    }

}
