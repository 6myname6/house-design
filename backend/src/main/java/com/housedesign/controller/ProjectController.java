package com.housedesign.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.housedesign.common.BusinessException;
import com.housedesign.common.Result;

import com.housedesign.Service.ProjectService;
import com.housedesign.dto.response.ProjectResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@RestController
@Validated
// 项目设计Controller
public class ProjectController {
    private final ProjectService projectService;

    // 创建项目并上传设计图
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ProjectResponse> createProject(
            @RequestParam("name") @NotBlank(message = "项目名称不能为空") @Size(max = 128, message = "项目名称最长128个字符") String name,
            @RequestParam(value = "description", required = false) @Size(max = 512, message = "描述最多512个字符") String description,
            @RequestParam(value = "style", required = false) @Size(max = 512, message = "样式最多512个字符") String style,
            @RequestParam(value = "styleLabel", required = false) @Size(max = 32, message = "风格标签不合法") String styleLabel,
            @RequestParam("designImage") MultipartFile designImage) {

        // 校验：style（自定义要求）与 styleLabel（预设 code）至少填一个
        boolean hasStyle = style != null && !style.isBlank();
        boolean hasStyleLabel = styleLabel != null && !styleLabel.isBlank();
        if (!hasStyle && !hasStyleLabel) {
            throw new BusinessException(400, "请至少填写风格要求或选择风格标签");
        }
        if (designImage == null || designImage.isEmpty()) {
            throw new BusinessException(400, "上传图片不能为空，请上传图片");
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

    // 项目详情
    @GetMapping("/{id}")
    public Result<ProjectResponse> getProjectDetail(@PathVariable(value = "id") Long id) {

        return Result.success(projectService.getProjectDetail(id));
    }

    // 删除任务
    @DeleteMapping("/{id}")
    public Result<String> delProject(@PathVariable(value = "id") Long id) {
        projectService.delProject(id);
        return Result.success("删除成功");
    }

}
