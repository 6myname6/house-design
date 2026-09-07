package com.housedesign.Service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.housedesign.dto.response.ProjectResponse;

public interface ProjectService {

    /**
     * 创建项目并上传设计图。
     * 内部复用 FileStorageService.upload() 将设计图落盘到 designs 目录，
     * 然后把 URL 存进 t_design_project.design_image_url。
     *
     * @param name        项目名称
     * @param description 项目描述（可为空）
     * @param style       用户自定义风格要求（自由文本，如「原木色、温馨」），可为空
     * @param styleLabel  预设风格 code（如 "modern-minimalist"），可为空
     *                    <p>
     *                    style 与 styleLabel 至少填一个，不可同时为空
     * @param designImage 设计图文件
     */
    ProjectResponse createProject(String name, String description, String style, String styleLabel,
            MultipartFile designImage);

    /**
     * 当前用户的全部项目列表（按创建时间倒序）。
     * 仅返回自己名下的项目。
     */
    List<ProjectResponse> listMyProjects();
}
