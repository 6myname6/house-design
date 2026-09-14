package com.housedesign.Service;

import java.util.List;

import com.housedesign.dto.response.GenerationResponse;

public interface GenerationService {

    GenerationResponse generateRequest(Long projectId);

    GenerationResponse querySingleTask(Long id);

    /** 某项目下的生成记录（按创建时间倒序），仅限本人 */
    List<GenerationResponse> listByProject(Long projectId);

    /** 当前用户所有生成记录（按创建时间倒序） */
    List<GenerationResponse> listAll();

}
