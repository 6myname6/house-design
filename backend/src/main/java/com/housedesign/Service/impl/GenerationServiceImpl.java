package com.housedesign.Service.impl;

import org.springframework.stereotype.Service;

import com.housedesign.Service.GenerationService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.UserContext;
import com.housedesign.dto.response.GenerationResponse;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.GeneratedModel;
import com.housedesign.entity.GenerationStatus;
import com.housedesign.mapper.GeneratedModelMapper;
import com.housedesign.mapper.ProjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {
    private final GeneratedModelMapper generatedModelMapper;
    private final ProjectMapper projectMapper;

    // 工具方法

    // 实体->响应体
    private GenerationResponse toResponse(GeneratedModel model) {
        GenerationResponse resp = new GenerationResponse();
        resp.setId(model.getId());
        resp.setProjectId(model.getProjectId());
        resp.setStatus(model.getStatus());
        resp.setProvider(model.getProvider());
        resp.setModelUrl(model.getModelUrl());
        resp.setPreviewImageUrl(model.getPreviewImageUrl());
        resp.setPanoramaUrl(model.getPanoramaUrl());
        resp.setSceneConfig(model.getSceneConfig());
        resp.setErrorMessage(model.getErrorMessage());
        resp.setCreatedAt(model.getCreatedAt());
        resp.setUpdatedAt(model.getUpdatedAt());
        return resp;
    }

    // 发起生成
    @Override
    public GenerationResponse generateRequest(Long projectId) {
        // 当前用户id
        Long userId = UserContext.getUserId();
        log.info("发起生成：projectId={}，userId={}", projectId, userId);

        // 1.校验项目是否属于本人
        DesignProject designProject = projectMapper.selectById(projectId);
        if (designProject == null || !designProject.getUserId().equals(userId)) {
            log.warn("发起生成失败或不是你的项目！:projectId = {},userId = {}", projectId, userId);
            throw new BusinessException(404, "项目不存在！");
        }
        // 2.校验项目是否有设计图
        if (designProject.getDesignImageUrl() == null || designProject.getDesignImageUrl().isBlank()) {
            log.warn("发起生成失败，暂无设计图：projectId={},userId={}", projectId, userId);
            throw new BusinessException(404, "设计图纸不存在！");
        }
        // 3.建PENDING任务落库（状态机起点）
        GeneratedModel record = new GeneratedModel();
        record.setProjectId(projectId);
        record.setUserId(userId);
        record.setStatus(GenerationStatus.PENDING);
        generatedModelMapper.insert(record);

        log.info("发起生成成功，任务已创建：taskId={},projectId={}", record.getId(), projectId);

        return toResponse(record);
    }
}
