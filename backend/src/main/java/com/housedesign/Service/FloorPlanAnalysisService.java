package com.housedesign.Service;

import com.housedesign.dto.ai.FloorPlanResult;
import com.housedesign.entity.DesignProject;

/**
 * 户型图识别服务：用视觉大模型读取用户户型图，
 * 输出结构化的房间清单 / 相邻关系 / 面积量级 / 户型图 bbox。
 *
 * 输出作为逐房间生图与 sceneConfig（photo-tour v2）组装的唯一依据。
 */
public interface FloorPlanAnalysisService {

    /**
     * 识别项目户型图。
     *
     * @param project 必须带可访问的 designImageUrl（本地存储或公网 URL）
     * @return 校验并规范化后的户型结构
     * @throws IllegalStateException 模型调用失败、JSON 无法解析或结构校验不通过
     */
    FloorPlanResult analyze(DesignProject project);
}
