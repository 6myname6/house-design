package com.housedesign.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.housedesign.Service.KbDocumentService;
import com.housedesign.Utils.Result;
import com.housedesign.common.BusinessException;
import com.housedesign.dto.response.KbDocumentCreateResponse;
import com.housedesign.dto.response.KbDocumentResponse;
import com.housedesign.dto.response.PageResult;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/kb/documents")
public class KbDocumentController {
    private final KbDocumentService kbDocumentService;

    // 创建知识库
    @PostMapping(consumes = "multipart/form-data")
    public Result<KbDocumentCreateResponse> createKbDocument(@RequestParam(required = false, name = "file") MultipartFile file,
            @RequestParam(required = false, name = "text") @Size(max = 100000, message = "文本内容最长 10 万字符") String text,
            @RequestParam(required = false, name = "title") @Size(max = 255, message = "标题最长 255 字符") String title,
            @RequestParam(required = false, defaultValue = "PUBLIC", name = "visibility") String visibility) {
        boolean hasFile = (file != null && !file.isEmpty());
        boolean hasText = (text != null && !text.isBlank());
        if (hasText == false && hasFile == false) {
            throw new BusinessException(400, "请上传文件或填写文本内容");
        }
        if (hasText && hasFile) {
            log.warn("文件和文本只能填一个");
            throw new BusinessException(400, "文件与文本只能选择一种录入方式");
        }
        log.info("开始创建知识库文档");
        return Result.success(kbDocumentService.createKbDocument(file, text, title, visibility));
    }

    // 获取知识库详情
    @GetMapping("/{id}")
    public Result<KbDocumentResponse> getkbDetail(@PathVariable("id") Long id) {
        log.info("查看id={}的知识库详情", id);
        return Result.success(kbDocumentService.getKbDetail(id));
    }

    // 查询文档列表
    @GetMapping()
    public Result<PageResult<KbDocumentResponse>> getKbList(
            @RequestParam(required = false, defaultValue = "1", name = "pageNum") int pageNum,
            @RequestParam(required = false, defaultValue = "20", name = "pageSize") int pageSize,
            @RequestParam(required = false, name = "status") String status) {
        if (pageNum < 1) {
            log.warn("页码不能小于1");
            pageNum = 1;
        }
        if (pageSize > 50) {
            log.warn("每页不能超过50条");
            pageSize = 50;
        }
        if (pageSize < 1) {
            log.warn("每页条数不能小于1");
            pageSize = 20;
        }
        return Result.success(kbDocumentService.getKbList(pageNum, pageSize, status));
    }

}
