package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.Service.KbSearchService;
import com.housedesign.Utils.Result;
import com.housedesign.dto.request.KbSearchRequest;
import com.housedesign.dto.response.KbSearchResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kb")
public class KbSearchController {
    private final KbSearchService kbSearchService;

    @PostMapping("/search")
    public Result<KbSearchResponse> search(@RequestBody @Valid KbSearchRequest request) {
        return Result.success(kbSearchService.search(request));
    }

}
