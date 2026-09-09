package com.housedesign.Service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.housedesign.Service.PostService;
import com.housedesign.common.UserContext;
import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.PostResponse;
import com.housedesign.entity.Post;
import com.housedesign.entity.User;
import com.housedesign.mapper.PostMapper;
import com.housedesign.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j

public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @Override
    public PostResponse postApost(PostRequest postRequest) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        // 创建post实例
        Post post = new Post();
        post.setUserId(userId);
        post.setImages(postRequest.getImages());
        post.setContent(postRequest.getContent());

        // 插入数据库
        postMapper.insert(post);

        // 包装响应数据
        // PostResponse postResponse = new PostResponse();
        // postResponse.setAuthorAvatar(user.getAvatar());
        // postResponse.setAuthorName(user.getNickname());
        // postResponse.setCommentCount(post.getCommentCount());
        // postResponse.setCreatedAt(post.getCreatedAt());
        // postResponse.setImages(postRequest.getImages());
        // postResponse.setId(post.getId());
        // postResponse.setUserId(userId);
        return toResponse(post, user);
    }

    private PostResponse toResponse(Post post, User user) {
        PostResponse postResponse = new PostResponse();
        postResponse.setAuthorAvatar(user.getAvatar());
        postResponse.setAuthorName(user.getNickname());
        postResponse.setCommentCount(post.getCommentCount());
        postResponse.setComments(List.of());
        postResponse.setContent(post.getContent());
        postResponse.setCreatedAt(post.getCreatedAt());
        postResponse.setId(post.getId());
        postResponse.setImages(post.getImages());
        postResponse.setLikeCount(post.getLikeCount());
        postResponse.setLikedByMe(false);
        postResponse.setUserId(user.getId());
        return postResponse;
    }
}
