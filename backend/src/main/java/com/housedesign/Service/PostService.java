package com.housedesign.Service;

import java.util.List;

import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.PostResponse;

/**
 * PostService
 */
public interface PostService {
    // 发布帖子
    PostResponse postApost(PostRequest postRequest);

    List<PostResponse> postList(boolean mine);

    void deletePost(Long id);

}
