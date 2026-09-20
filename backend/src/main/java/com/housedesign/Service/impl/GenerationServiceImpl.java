package com.housedesign.Service.impl;

import java.util.List;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.FileStorageService;
import com.housedesign.Service.GenerationService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.UserContext;
import com.housedesign.dto.response.GenerationResponse;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
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
    private final ThreadPoolTaskExecutor generationExecutor;
    private final GeneratedModelMapper generatedModelMapper;
    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;
    private final AIimageService aIimageService;

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

    // 组装AI提示词
    private String buildPrompt(DesignProject designProject) {
        DesignStyle designStyle = DesignStyle.fromCode(designProject.getStyleLabel());
        StringBuilder sb = new StringBuilder("请生成一套房屋室内装修效果图");
        if (designStyle != null) {
            sb.append(",风格为:").append(designStyle.getLabel())
                    .append(".设计要求:").append(designStyle.getPrompt());
        }
        if (designProject.getStyle() != null && !designProject.getStyle().isBlank())
            sb.append(".用户补充要求:").append(designProject.getStyle());
        return sb.append(".画面写实,光线自然").toString();
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
        generationExecutor.execute(() -> runGeneration(record, designProject));
        return toResponse(record);
    }

    // 启动生成
    private void runGeneration(GeneratedModel record, DesignProject designProject) {

        // 1.状态改PROCESSING,落库
        record.setStatus(GenerationStatus.PROCESSING);
        generatedModelMapper.updateById(record);
        try {
            // 2.组装prompt(风格枚举prompt+用户自由文本)
            String prompt = buildPrompt(designProject);
            // 3.AI生图->拿到临时url
            String remoteUrl = aIimageService.generateImageUrl(prompt);
            // 4.下载落盘到本地,拿到本地永久url
            String localUrl = fileStorageService.downloadFromUrl(remoteUrl, "designs");
            // 5.写成功结果
            record.setProvider("zhipu");
            record.setPanoramaUrl(localUrl);
            record.setPreviewImageUrl(localUrl);
            record.setSceneConfig("{\"type\":\"photo-tour\"}");
            record.setStatus(GenerationStatus.SUCCESS);
            log.info("生成成功:taskId={},url={}", record.getId(), localUrl);
        } catch (Exception e) {
            // 6. 任何一步失败->FAILED+记录原因
            record.setStatus(GenerationStatus.FAILED);
            record.setErrorMessage(e.getMessage() == null ? "未知错误"
                    : e.getMessage().substring(0, Math.min(1024, e.getMessage().length())));
            generatedModelMapper.updateById(record);
            log.error("生成失败：taskId={}", record.getId(), e);
        }
    }

    // 查询单个任务,轮询
    @Override
    public GenerationResponse querySingleTask(Long id) {
        Long userId = UserContext.getUserId();
        GeneratedModel generatedModel = generatedModelMapper.selectById(id);
        if (generatedModel == null || !generatedModel.getUserId().equals(userId)) {
            log.warn("查询生成任务失败-不存在或越权：taskId={}, userId={}", id, userId);
            throw new BusinessException(404, "生成任务不存在");
        }
        return toResponse(generatedModel);
    }

    // 项目下的生成记录（倒序）
    @Override
    public List<GenerationResponse> listByProject(Long projectId) {
        Long userId = UserContext.getUserId();
        log.info("查询项目生成记录：projectId={}, userId={}", projectId, userId);
        // 先校验项目归属，防越权列举他人项目的生成记录
        DesignProject project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            log.warn("查询项目生成记录失败-项目不存在或越权：projectId={}, userId={}", projectId, userId);
            throw new BusinessException(404, "项目不存在");
        }
        return generatedModelMapper.selectList(
                new LambdaQueryWrapper<GeneratedModel>()
                        .eq(GeneratedModel::getProjectId, projectId)
                        .orderByDesc(GeneratedModel::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }

    // 当前用户所有生成记录（倒序）
    @Override
    public List<GenerationResponse> listAll() {
        Long userId = UserContext.getUserId();
        log.info("查询我的生成记录：userId={}", userId);
        return generatedModelMapper.selectList(
                new LambdaQueryWrapper<GeneratedModel>()
                        .eq(GeneratedModel::getUserId, userId)
                        .orderByDesc(GeneratedModel::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }
}
