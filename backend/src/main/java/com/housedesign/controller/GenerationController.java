package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.GenerationService;
import com.housedesign.common.Result;
import com.housedesign.dto.response.GenerationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RequestMapping("/api/projects")
@RestController
@Slf4j
@RequiredArgsConstructor
public class GenerationController {
    private final GenerationService generationService;

    // 发起生成
    @PostMapping("/{projectId}/generate")
    public Result<GenerationResponse> generateRequest(@PathVariable(value = "projectId") Long projectId) {
        log.info("发起生成，项目id：{}", projectId);
        return Result.success(generationService.generateRequest(projectId));
    }

}
