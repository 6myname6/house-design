package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.PostService;
import com.housedesign.common.Result;
import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.LikeResult;
import com.housedesign.dto.response.PageResult;
import com.housedesign.dto.response.PostResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/posts")
@RequiredArgsConstructor
@RestController
@Slf4j
// 装修小圈Controller
public class PostController {
    private final PostService postService;

    // 发表帖子
    @PostMapping()
    public Result<PostResponse> postApost(@RequestBody PostRequest postRequest) {
        // 防止内容图片都空
        if (postRequest.getContent() == null && postRequest.getImages() == null) {
            return Result.error("请确认您要发表的内容！");
        }
        log.info("开始发表帖子：{}", postRequest);
        return Result.success(postService.postApost(postRequest));
    }

    // 帖子列表
    @GetMapping()
    public Result<PageResult<PostResponse>> postList(@RequestParam(value = "mine", defaultValue = "false") boolean mine,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        log.info("显示帖子");
        return Result.success(postService.postList(mine, pageNum, pageSize));
    }

    // 删除帖子
    @DeleteMapping("/{id}")
    public Result<String> deletePost(@PathVariable Long id) {
        log.info("删除帖子：{}", id);
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    // 点赞帖子
    @PostMapping("/{id}/like")
    public Result<LikeResult> likeApost(@PathVariable(value = "postId") Long postId) {

        return Result.success(postService.togglePostLike(postId));

    }

}
