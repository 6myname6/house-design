package com.housedesign.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.housedesign.common.Result;

import com.housedesign.Service.ProjectService;
import com.housedesign.dto.response.ProjectResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@RestController
// 项目设计Controller
public class ProjectController {
    private final ProjectService projectService;

    // 创建项目并上传设计图
    @PostMapping()
    public Result<ProjectResponse> createProject(@RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "styleLabel", required = false) String styleLabel,
            @RequestParam("designImage") MultipartFile designImage) {

        // 校验：style（自定义要求）与 styleLabel（预设 code）至少填一个
        boolean hasStyle = style != null && !style.isBlank();
        boolean hasStyleLabel = styleLabel != null && !styleLabel.isBlank();
        if (!hasStyle && !hasStyleLabel) {
            return Result.error("请至少填写风格要求或选择风格标签");
        }
        if (designImage == null || designImage.isEmpty()) {
            return Result.error("上传图片不能为空，请上传图片");
        }
        ProjectResponse projectResponse = projectService.createProject(name, description, style, styleLabel,
                designImage);
        return Result.success(projectResponse);
    }

    // 项目列表（仅当前用户自己的项目）
    @GetMapping()
    public Result<List<ProjectResponse>> list() {
        return Result.success(projectService.listMyProjects());
    }

}
