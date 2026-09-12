package com.housedesign.Service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.housedesign.Service.PostService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.UserContext;
import com.housedesign.dto.request.CommentRequest;
import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.CommentResponse;
import com.housedesign.dto.response.LikeResult;
import com.housedesign.dto.response.PageResult;
import com.housedesign.dto.response.PostResponse;
import com.housedesign.entity.CommentLike;
import com.housedesign.entity.Post;
import com.housedesign.entity.PostComment;
import com.housedesign.entity.PostLike;
import com.housedesign.entity.User;
import com.housedesign.mapper.CommentLikeMapper;
import com.housedesign.mapper.PostCommentMapper;
import com.housedesign.mapper.PostLikeMapper;
import com.housedesign.mapper.PostMapper;
import com.housedesign.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j

public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCommentMapper postCommentMapper;
    private final CommentLikeMapper commentLikeMapper;

    // 类内公共工具方法

    // 帖子实体->帖子响应体
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

    /** 评论实体 → 响应体（补作者 + 点赞信息） */
    private CommentResponse toCommentResponse(PostComment comment) {
        User author = userMapper.selectById(comment.getUserId());
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setPostId(comment.getPostId());
        resp.setUserId(comment.getUserId());
        resp.setParentId(comment.getParentId());
        resp.setAuthorName(author.getNickname());
        resp.setAuthorAvatar(author.getAvatar());
        resp.setContent(comment.getContent());
        resp.setImages(comment.getImages());
        resp.setLikeCount(countCommentLikes(comment.getId()));
        resp.setLikedByMe(isCommentLiked(comment.getId()));
        resp.setCreatedAt(comment.getCreatedAt());
        return resp;
    }

    // 统计帖子点赞数
    private int countPostLikes(Long postId) {
        return Math.toIntExact(postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)));
    }

    /** 统计评论点赞数 */
    private int countCommentLikes(Long commentId) {
        return Math.toIntExact(commentLikeMapper.selectCount(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, commentId)));
    }

    /** 当前用户是否赞过该评论 */
    private boolean isCommentLiked(Long commentId) {
        Long userId = UserContext.getUserId();
        return commentLikeMapper.selectCount(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, commentId)
                        .eq(CommentLike::getUserId, userId)) > 0;
    }

    // 业务方法

    // 发布帖子
    @Override
    public PostResponse postApost(PostRequest postRequest) {
        Long userId = UserContext.getUserId();
        log.info("发帖：userId={}, content={}, imageCount={}",
                userId, postRequest.getContent(), postRequest.getImages() == null ? 0 : postRequest.getImages().size());
        User user = userMapper.selectById(userId);
        // 创建post实例
        Post post = new Post();
        post.setUserId(userId);
        post.setImages(postRequest.getImages());
        post.setContent(postRequest.getContent());

        // 插入数据库
        postMapper.insert(post);
        log.info("发帖成功：postId={}, userId={}", post.getId(), userId);

        // 包装响应数据
        return toResponse(post, user);
    }

    // 帖子列表
    @Override
    public PageResult<PostResponse> postList(boolean mine, int pageNum, int pageSize) {
        Long userId = UserContext.getUserId();
        log.info("帖子列表查询：userId={}, mine={}, pageNum={}, pageSize={}", userId, mine, pageNum, pageSize);
        // 1. 分页查询：mine=true 才过滤当前用户，否则查全部
        Page<Post> page = postMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Post>()
                        .eq(mine, Post::getUserId, userId)
                        .orderByDesc(Post::getCreatedAt));
        // 2. 当前页帖子转响应体（补作者信息，N+1 朴素版）
        List<PostResponse> records = page.getRecords().stream()
                .map(post -> toResponse(post, userMapper.selectById(post.getUserId())))
                .toList();
        // 3. 返回总数 + 当前页数据
        log.info("帖子列表查询完成：userId={}, total={}, pageCount={}", userId, page.getTotal(), records.size());
        return new PageResult<>(page.getTotal(), records);
    }

    // 删除帖子
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        Long userId = UserContext.getUserId();
        log.info("删除帖子：postId={}, userId={}", id, userId);
        // 检查帖子是否存在
        Post post = postMapper.selectById(id);
        if (post == null) {
            log.warn("删除帖子失败-帖子不存在：postId={}, userId={}", id, userId);
            throw new IllegalArgumentException("帖子不存在");
        }
        // 检查帖子是否属于当前用户
        if (!post.getUserId().equals(userId)) {
            log.warn("删除帖子失败-越权访问：postId={}, userId={}, ownerId={}", id, userId, post.getUserId());
            throw new IllegalArgumentException("只能删除自己的帖子！");
        }
        // 按 post_id 删除该帖子的评论与点赞（deleteById 是按主键删，这里必须按外键 post_id 删）
        postCommentMapper.delete(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, id));
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, id));
        // 最后删帖子本体
        postMapper.deleteById(id);
        log.info("删除帖子成功：postId={}, userId={}", id, userId);
    }

    // 点赞或取消点赞
    @Override
    public LikeResult toggleCommentLike(Long commentId) {
        Long userId = UserContext.getUserId();
        log.info("评论点赞切换：commentId={}, userId={}", commentId, userId);
        // 校验评论存在
        if (postCommentMapper.selectById(commentId) == null) {
            log.warn("评论点赞失败-评论不存在：commentId={}, userId={}", commentId, userId);
            throw new BusinessException(404, "评论不存在");
        }
        // 查当前点赞状态
        CommentLike existing = commentLikeMapper.selectOne(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, commentId)
                        .eq(CommentLike::getUserId, userId));
        if (existing != null) {
            // 已赞 → 取消
            commentLikeMapper.deleteById(existing.getId());
            log.info("取消评论点赞：commentId={}, userId={}", commentId, userId);
            return new LikeResult(countCommentLikes(commentId), false);
        } else {
            // 未赞 → 点赞（UNIQUE(comment_id,user_id) 兜底防并发重复）
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            log.info("评论点赞成功：commentId={}, userId={}", commentId, userId);
            return new LikeResult(countCommentLikes(commentId), true);
        }
    }

    // 回复评论
    @Override
    public CommentResponse replyComment(Long commentId, CommentRequest request) {
        Long userId = UserContext.getUserId();
        log.info("回复评论：commentId={}, userId={}", commentId, userId);
        // 校验：文字与图片至少填一个
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasImages = request.getImages() != null && !request.getImages().isEmpty();
        if (!hasContent && !hasImages) {
            log.warn("回复评论失败-内容为空：commentId={}, userId={}", commentId, userId);
            throw new IllegalArgumentException("请填写内容或上传图片，至少填一个");
        }
        // 校验被回复的评论存在
        PostComment parent = postCommentMapper.selectById(commentId);
        if (parent == null) {
            log.warn("回复评论失败-被回复评论不存在：commentId={}, userId={}", commentId, userId);
            throw new BusinessException(404, "评论不存在");
        }
        // 新建回复：parent_id 指向被回复评论，post_id 沿用父评论所属帖子
        PostComment reply = new PostComment();
        reply.setPostId(parent.getPostId());
        reply.setUserId(userId);
        reply.setParentId(commentId);
        reply.setContent(request.getContent());
        reply.setImages(request.getImages());
        postCommentMapper.insert(reply);
        log.info("回复评论成功：replyId={}, commentId={}, userId={}", reply.getId(), commentId, userId);
        return toCommentResponse(reply);
    }

    // 点赞或取消点赞帖子
    @Override
    public LikeResult togglePostLike(Long postId) {
        Long userId = UserContext.getUserId();// 当前用户id
        log.info("帖子点赞切换：postId={}, userId={}", postId, userId);
        Post post = postMapper.selectById(postId);// 将要操作的帖子
        // 1.校验帖子是否存在
        if (post == null) {
            log.warn("帖子点赞失败-帖子不存在：postId={}, userId={}", postId, userId);
            throw new BusinessException(404, "点赞帖子不存在或已被删除");
        }
        // 2.查看当前点赞状态
        PostLike isLike = postLikeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId));
        if (isLike == null) {
            // 如果没有点赞，则点赞
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeMapper.insert(like);
            log.info("帖子点赞成功：postId={}, userId={}", postId, userId);
            return new LikeResult(countPostLikes(postId), true);
        } else {
            // 点赞了，取消点赞
            postLikeMapper.deleteById(isLike.getId());
            log.info("取消帖子点赞：postId={}, userId={}", postId, userId);
            return new LikeResult(countPostLikes(postId), false);
        }

    }

    // 评论帖子
    @Override
    public CommentResponse commentApost(Long postId, CommentRequest commentRequest) {
        Long userId = UserContext.getUserId();
        log.info("评论帖子：postId={}, userId={}", postId, userId);
        // 校验帖子是否存在
        if (postMapper.selectById(postId) == null) {
            log.warn("评论帖子失败-帖子不存在：postId={}, userId={}", postId, userId);
            throw new BusinessException(404, "抱歉，帖子不存在了~");
        }
        PostComment postComment = new PostComment();
        postComment.setContent(commentRequest.getContent());
        postComment.setPostId(postId);
        postComment.setUserId(userId);
        postComment.setImages(commentRequest.getImages());
        postCommentMapper.insert(postComment);
        log.info("评论帖子成功：commentId={}, postId={}, userId={}", postComment.getId(), postId, userId);
        return toCommentResponse(postComment);

    }

    @Override
    public void delComment(Long commentId) {
        PostComment postComment = postCommentMapper.selectById(commentId);
        if (postComment == null) {// 检查评论是否存在
            log.error("不存在评论：{}", commentId);
            throw new BusinessException(404, "抱歉，评论不存在");
        }
        // 权限校验，检查是否为自己的评论
        Long userId = UserContext.getUserId();
        if (userId != postComment.getUserId()) {
            log.warn("只能删除自己的评论！");
            throw new BusinessException(404, "只能删除自己的评论");
        }
        commentLikeMapper.deleteById(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, commentId));
        postCommentMapper.delete(
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getParentId, commentId));
        postCommentMapper.deleteById(commentId);
        log.info("删除评论成功：commentId={}, userId={}", commentId, userId);
    }

}
