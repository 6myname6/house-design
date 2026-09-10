package com.housedesign.Service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public List<PostResponse> postList(boolean mine) {

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (mine) {
            Long userId = UserContext.getUserId();
            wrapper.eq(Post::getUserId, userId)
                    .orderByDesc(Post::getCreatedAt);
            List<Post> posts = postMapper.selectList(wrapper);
            return posts.stream().map(post -> toResponse(post, userMapper.selectById(post.getUserId()))).toList();
        } else {
            wrapper.orderByDesc(Post::getCreatedAt);
            List<Post> posts = postMapper.selectList(wrapper);
            return posts.stream().map(post -> toResponse(post, userMapper.selectById(post.getUserId()))).toList();
        }

    }

    @Override
    public void deletePost(Long id) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        // 检查帖子是否属于当前用户
        if (!post.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("只能删除自己的帖子！");
        }
        // 删除帖子
        postMapper.deleteById(id);
    }
}
