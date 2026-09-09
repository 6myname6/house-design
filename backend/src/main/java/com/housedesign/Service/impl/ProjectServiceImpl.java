package com.housedesign.Service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.FileStorageService;
import com.housedesign.Service.ProjectService;
import com.housedesign.common.BusinessException;
import com.housedesign.common.Result;
import com.housedesign.common.UserContext;
import com.housedesign.dto.response.ProjectResponse;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
import com.housedesign.mapper.ProjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;

    @Override
    public ProjectResponse createProject(String name, String description,
            String style, String styleLabel,
            MultipartFile designImage) {
        // 获取用户
        Long userId = UserContext.getUserId();
        // 校验：style（自定义要求）与 styleLabel（预设 code）至少填一个
        boolean hasStyle = style != null && !style.isBlank();
        boolean hasStyleLabel = styleLabel != null && !styleLabel.isBlank();
        if (!hasStyle && !hasStyleLabel) {
            throw new IllegalArgumentException("请填写风格要求或选择风格标签！");
        }
        // 若传了 styleLabel，校验 code 合法性并反查中文标签
        DesignStyle resolvedStyle = hasStyleLabel ? DesignStyle.fromCode(styleLabel) : null;
        if (hasStyleLabel && resolvedStyle == null) {
            throw new IllegalArgumentException("不支持的装修风格标签：" + styleLabel);
        }
        // 调用FileStorageService接口,上传图片,获取url
        String designImageUrl = fileStorageService.upload(designImage, "designs");
        // 新建项目对象
        DesignProject designProject = new DesignProject();
        designProject.setUserId(userId);
        designProject.setDescription(description);
        designProject.setDesignImageUrl(designImageUrl);
        designProject.setName(name);
        designProject.setStyle(style);
        designProject.setStyleLabel(hasStyleLabel ? resolvedStyle.getCode() : null);
        // 新增入数据库
        projectMapper.insert(designProject);
        log.info("新增入库:id:{},name:{},style:{},styleLabel:{}",
                designProject.getId(), name, style, designProject.getStyleLabel());
        // 填入响应实体
        ProjectResponse projectResponse = new ProjectResponse();
        projectResponse.setId(designProject.getId());
        projectResponse.setName(designProject.getName());
        projectResponse.setDescription(designProject.getDescription());
        projectResponse.setStyle(designProject.getStyle());
        projectResponse.setStyleLabel(hasStyleLabel ? resolvedStyle.getLabel() : null);
        projectResponse.setDesignImageUrl(designProject.getDesignImageUrl());
        return projectResponse;
    }

    @Override
    public List<ProjectResponse> listMyProjects() {
        Long userId = UserContext.getUserId();
        // 仅查当前用户的项目，按创建时间倒序
        List<DesignProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<DesignProject>()
                        .eq(DesignProject::getUserId, userId)
                        .orderByDesc(DesignProject::getCreatedAt));
        return projects.stream()
                .map(this::toResponse)
                .toList();
    }

    /** 实体 → 响应体（隐藏 userId、designImagePath 等内部字段） */
    private ProjectResponse toResponse(DesignProject project) {
        ProjectResponse resp = new ProjectResponse();
        resp.setId(project.getId());
        resp.setName(project.getName());
        resp.setDescription(project.getDescription());
        resp.setStyle(project.getStyle());
        // styleLabel 存的是 code，反查中文标签给前端展示
        DesignStyle resolvedStyle = DesignStyle.fromCode(project.getStyleLabel());
        resp.setStyleLabel(resolvedStyle != null ? resolvedStyle.getLabel() : null);
        resp.setDesignImageUrl(project.getDesignImageUrl());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        return resp;
    }

    @Override
    public ProjectResponse getProjectDetail(Long id) {
        Long userId = UserContext.getUserId();
        DesignProject project = projectMapper.selectById(id);
        // 不存在或不属于当前用户，统一 404（防越权访问与探测他人项目）
        if (project == null || !project.getUserId().equals(userId)) {
            throw new BusinessException(404, "项目不存在");
        }
        return toResponse(project);
    }

    @Override
    public void delProject(Long id) {
        Long userId = UserContext.getUserId();
        DesignProject designProject = projectMapper.selectById(id);
        if (designProject == null || !userId.equals(designProject.getUserId())) {
            throw new BusinessException(404, "项目不存在!");
        }
        projectMapper.deleteById(id);
    }

}
