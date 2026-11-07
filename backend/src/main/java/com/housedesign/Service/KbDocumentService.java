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

    // 重建索引：版本号 +1 后重新走解析/切块/向量化，用于失败重试或分块策略调整后刷新
    KbDocumentResponse reindexKbDocument(Long id);

    // 删除文档：同步清理 MySQL 分块与 PG 向量，仅上传者本人可操作
    void deleteKbDocument(Long id);

}
