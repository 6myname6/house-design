package com.housedesign.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.GenerationService;
import com.housedesign.common.Result;
import com.housedesign.dto.response.GenerationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RequestMapping("/api")
@RestController
@Slf4j
@RequiredArgsConstructor
public class GenerationController {
    private final GenerationService generationService;

    // 发起生成
    @PostMapping("/projects/{projectId}/generate")
    public Result<GenerationResponse> generateRequest(@PathVariable(value = "projectId") Long projectId) {
        log.info("发起生成，项目id：{}", projectId);
        return Result.success(generationService.generateRequest(projectId));
    }

    // 查询单个生成任务
    @GetMapping("/generations/{id}")
    public Result<GenerationResponse> querySingleTask(@PathVariable(value = "id") Long id) {
        log.info("开始查询单个任务:{}", id);
        return Result.success(generationService.querySingleTask(id));
    }

    // 项目下的生成记录
    @GetMapping("/projects/{projectId}/generations")
    public Result<List<GenerationResponse>> listByProject(@PathVariable(value = "projectId") Long projectId) {
        log.info("查询项目生成记录：{}", projectId);
        return Result.success(generationService.listByProject(projectId));
    }

    // 当前用户所有生成记录
    @GetMapping("/generations")
    public Result<List<GenerationResponse>> listAll() {
        log.info("查询我的所有生成记录");
        return Result.success(generationService.listAll());
    }

}
