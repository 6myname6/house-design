package com.housedesign.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.housedesign.common.Result;
import com.housedesign.entity.DesignStyle;

/**
 * 风格列表接口。
 * 数据源为 DesignStyle 枚举，是风格列表的单一数据源。
 * DesignStyle 已用 @JsonFormat(shape=OBJECT) + @JsonIgnore(prompt) 控制序列化，
 * 直接返回 List<DesignStyle> 即可，无需额外 DTO。
 */
@RestController
@RequestMapping("/api/styles")
public class StyleController {

    @GetMapping
    public Result<List<DesignStyle>> list() {
        return Result.success(Arrays.asList(DesignStyle.values()));
    }
}
