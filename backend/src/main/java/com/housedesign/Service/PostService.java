package com.housedesign.Service;

import com.housedesign.dto.request.PostRequest;
import com.housedesign.dto.response.PostResponse;

/**
 * PostService
 */
public interface PostService {
    // 发布帖子
    PostResponse postApost(PostRequest postRequest);

}
