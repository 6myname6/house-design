package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.PostService;
import com.housedesign.common.Result;
import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.PostResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/api/posts")
@RequiredArgsConstructor
@RestController
@Slf4j
// 发帖Controller
public class PostController {
    private final PostService postService;

    @PostMapping()
    public Result<PostResponse> postApost(@RequestBody PostRequest postRequest) {
        // 防止内容图片都空
        if (postRequest.getContent() == null && postRequest.getImages() == null) {
            return Result.error("请确认您要发表的内容！");
        }
        log.info("开始发表：{}", postRequest);
        return Result.success(postService.postApost(postRequest));
    }

}
