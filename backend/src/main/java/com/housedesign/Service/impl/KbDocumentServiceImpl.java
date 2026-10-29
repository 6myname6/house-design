package com.housedesign.Service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.housedesign.Service.KbDocumentService;
import com.housedesign.Utils.UserContext;
import com.housedesign.common.BusinessException;
import com.housedesign.dto.response.KbDocumentCreateResponse;
import com.housedesign.dto.response.KbDocumentResponse;
import com.housedesign.dto.response.PageResult;
import com.housedesign.entity.KbDocument;
import com.housedesign.entity.KbDocumentStatus;
import com.housedesign.entity.KbSourceType;
import com.housedesign.entity.KbVisibility;
import com.housedesign.mapper.KbDocumentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知识库文档管理实现。
 *
 * <p>只负责「建记录 + 查状态」，不碰解析、切块与向量化：
 * 那三步由异步入库链路推进，避免占住 HTTP 请求线程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl implements KbDocumentService {

    /** 单文件大小上限 20MB，与图片上传口径一致 */
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private final KbDocumentMapper kbDocumentMapper;

    // ==================== 私有方法 ====================

    // 判空
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 计算哈希：SHA-256 转 64 位小写十六进制，作为文档内容指纹
    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("jdk不支持SHA-256", e);
        }
    }

    // 查重：只看当前用户可见范围，别人的私有文档不算重复；失败态不阻塞重传
    private void assertNotDuplicate(String checksum, Long userId) {
        Long dup = kbDocumentMapper.selectCount(
                new LambdaQueryWrapper<KbDocument>()
                        .eq(KbDocument::getChecksum, checksum)
                        .ne(KbDocument::getStatus, KbDocumentStatus.FAILED)
                        .and(w -> w.eq(KbDocument::getVisibility, KbVisibility.PUBLIC)
                                .or()
                                .eq(KbDocument::getOwnerUserId, userId)));
        if (dup != null && dup > 0) {
            throw new BusinessException(409, "已存在内容相同的文档，无需重复上传");
        }
    }

    // 文档权限转换
    private KbVisibility parseVisibility(String visibility) {
        if (isBlank(visibility)) {
            return KbVisibility.PUBLIC;
        }
        try {
            return KbVisibility.valueOf(visibility.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "可见性参数不合法，仅支持 PUBLIC 或 PRIVATE");
        }
    }

    // 列表状态过滤参数转换
    private KbDocumentStatus parseStatus(String status) {
        try {
            return KbDocumentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "状态参数不合法");
        }
    }

    private String extOf(String fileName) {
        if (isBlank(fileName)) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return (dot < 0 || dot == fileName.length() - 1) ? null : fileName.substring(dot + 1).toLowerCase();
    }

    private String stripExt(String fileName) {
        if (isBlank(fileName)) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // 实体转响应体：刻意不返回 checksum 与 ossUrl，前端无使用场景
    private KbDocumentResponse toResponse(KbDocument doc) {
        KbDocumentResponse response = new KbDocumentResponse();
        response.setId(doc.getId());
        response.setTitle(doc.getTitle());
        response.setSourceType(doc.getSourceType());
        response.setFileName(doc.getFileName());
        response.setVisibility(doc.getVisibility());
        response.setStatus(doc.getStatus());
        response.setErrorMessage(doc.getErrorMessage());
        response.setVersion(doc.getVersion());
        response.setChunkCount(doc.getChunkCount());
        response.setCreatedAt(doc.getCreatedAt());
        return response;
    }

    // ==================== 接口实现 ====================

    @Override
    @Transactional
    public KbDocumentCreateResponse createKbDocument(MultipartFile file, String text, String title, String visibility) {
        // 文档/文本校验：Service 也可能被其他调用方直接调用，不能只靠 Controller 把关
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = text != null && !text.isBlank();

        if (!hasFile && !hasText) {
            throw new BusinessException(400, "请上传文件或填写文本内容");
        }
        if (hasFile && hasText) {
            throw new BusinessException(400, "文件与文本只能选择一种录入方式");
        }

        Long userId = UserContext.getUserId();
        KbVisibility resolvedVisibility = parseVisibility(visibility);

        String checksum;
        KbSourceType sourceType;
        String fileName = null;
        String fileExt = null;
        String resolvedTitle;

        if (hasFile) {
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new BusinessException(400, "文件大小不能超过 20MB");
            }
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
            } catch (IOException e) {
                throw new BusinessException(500, "读取上传文件失败");
            }
            if (fileBytes.length == 0) {
                throw new BusinessException(400, "上传文件内容为空");
            }
            // 内容算哈希
            checksum = sha256Hex(fileBytes);
            fileName = file.getOriginalFilename();
            fileExt = extOf(fileName);
            sourceType = KbSourceType.FILE;
            resolvedTitle = isBlank(title) ? stripExt(fileName) : title.trim();
            if (isBlank(resolvedTitle)) {
                resolvedTitle = "未命名文档";
            }
        } else {
            checksum = sha256Hex(text.trim().getBytes(StandardCharsets.UTF_8));
            sourceType = KbSourceType.TEXT;
            resolvedTitle = isBlank(title) ? "未命名文本" : title.trim();
        }

        // 查重
        assertNotDuplicate(checksum, userId);

        // 组装实体
        KbDocument doc = new KbDocument();
        doc.setTitle(resolvedTitle);
        doc.setSourceType(sourceType);
        doc.setFileName(fileName);
        doc.setFileExt(fileExt);
        doc.setChecksum(checksum);
        doc.setVisibility(resolvedVisibility);
        // PUBLIC 文档不属于任何个人，owner 必须为 null，否则权限过滤会把它当私有文档
        doc.setOwnerUserId(resolvedVisibility == KbVisibility.PRIVATE ? userId : null);
        doc.setStatus(KbDocumentStatus.PENDING);
        doc.setVersion(1);
        doc.setChunkCount(0);

        kbDocumentMapper.insert(doc);

        log.info("知识库文档已创建 id={}, title={}, sourceType={}, visibility={}",
                doc.getId(), doc.getTitle(), sourceType, resolvedVisibility);
        return new KbDocumentCreateResponse(doc.getId(), doc.getStatus());
    }

    @Override
    public KbDocumentResponse getKbDetail(Long id) {
        Long userId = UserContext.getUserId();
        KbDocument doc = kbDocumentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        // 无权访问私有文档时同样返回 404，不暴露该文档是否存在
        if (doc.getVisibility() == KbVisibility.PRIVATE && !userId.equals(doc.getOwnerUserId())) {
            throw new BusinessException(404, "文档不存在");
        }
        return toResponse(doc);
    }

    @Override
    public PageResult<KbDocumentResponse> getKbList(int pageNum, int pageSize, String status) {
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(KbDocument::getVisibility, KbVisibility.PUBLIC)
                .or()
                .eq(KbDocument::getOwnerUserId, userId));
        if (!isBlank(status)) {
            wrapper.eq(KbDocument::getStatus, parseStatus(status));
        }
        wrapper.orderByDesc(KbDocument::getId);

        Page<KbDocument> page = new Page<>(pageNum, pageSize);
        Page<KbDocument> result = kbDocumentMapper.selectPage(page, wrapper);

        List<KbDocumentResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(result.getTotal(), records);
    }
}
