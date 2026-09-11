package com.housedesign.Service;

import com.housedesign.dto.request.CommentRequest;
import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.CommentResponse;
import com.housedesign.dto.response.LikeResult;
import com.housedesign.dto.response.PageResult;
import com.housedesign.dto.response.PostResponse;

/**
 * PostService
 */
public interface PostService {
    // 发布帖子
    PostResponse postApost(PostRequest postRequest);

    // 帖子列表
    PageResult<PostResponse> postList(boolean mine, int pageNum, int pageSize);

    // 删除帖子
    void deletePost(Long id);

    // 评论点赞 / 取消点赞（切换）
    LikeResult toggleCommentLike(Long commentId);

    // 回复评论（评论的评论，parent_id 指向被回复评论）
    CommentResponse replyComment(Long commentId, CommentRequest request);

    LikeResult togglePostLike(Long postId);

}
