package com.housedesign.Service;

import org.springframework.web.multipart.MultipartFile;

import com.housedesign.dto.response.KbDocumentCreateResponse;
import com.housedesign.dto.response.KbDocumentResponse;
import com.housedesign.dto.response.PageResult;

/**
 * KbDocumentService
 */
public interface KbDocumentService {

    // 创建知识库
    KbDocumentCreateResponse createKbDocument(MultipartFile file, String text, String title, String visibility);

    KbDocumentResponse getKbDetail(Long id);

    PageResult<KbDocumentResponse> getKbList(int pageNum, int pageSize, String status);

}
