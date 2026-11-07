package com.housedesign.Service.impl;

import com.housedesign.common.KbVectorStore;
import com.housedesign.mapper.KbChunkMapper;
import com.housedesign.mapper.KbDocumentMapper;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.housedesign.Service.FileStorageService;
import com.housedesign.Service.KbIngestService;
import com.housedesign.Utils.KbTextChunker;
import com.housedesign.Utils.KbTextExtractor;
import com.housedesign.entity.KbChunk;
import com.housedesign.entity.KbDocument;
import com.housedesign.entity.KbDocumentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知识库入库编排实现（状态机 + 分块 + 向量化）。
 *
 * <p>
 * 状态流转：PENDING → PARSING → READY / FAILED。
 * 枚举里的 INDEXING 预留给“分块+向量化”拆成独立阶段时使用，当前一并通过。
 * </p>
 *
 * <p>
 * TEXT 直读 / FILE 下载后 Tika 解析 / URL 待实现。
 * 方法刻意不加事务：内部包含耗时的 Embedding HTTP 调用，且 MySQL 与 PG 是两个数据源，
 * 无法同事务，一致性靠「状态机 + 版本号 + 先删后插」兜底。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KbIngestServiceImpl implements KbIngestService {
    /** 文档元数据：状态流转、读取原文、回写 chunk_count */
    private final KbDocumentMapper kbDocumentMapper;
    /** 知识库专用线程池（Bean 名 kbExecutor，按字段名注入） */
    private final ThreadPoolTaskExecutor kbExecutor;
    /** 分块表：批量写入分块、回查自增 id */
    private final KbChunkMapper kbChunkMapper;
    /** 向量库写入组件（PostgreSQL + pgvector） */
    private final KbVectorStore kbVectorStore;
    /** 百炼 text-embedding-v3 客户端 */
    private final EmbeddingModel embeddingModel;
    // 文档解析器
    private final KbTextExtractor kbTextExtractor;
    private final FileStorageService fileStorageService;

    /** 单块最大字符数（app.rag.chunk.max-chars） */
    @Value("${app.rag.chunk.max-chars}")
    private int maxChars;

    /** 超长段落切分时相邻块的重叠字符数（app.rag.chunk.overlap-chars） */
    @Value("${app.rag.chunk.overlap-chars}")
    private int overlapChars;

    /** 单次 Embedding 请求最多携带的分块数（app.rag.embedding.batch-size） */
    @Value("${app.rag.embedding.batch-size}")
    private int batchSize;

    /** 写入向量表时记录的模型名（app.rag.embedding.model，长度必须 ≤ 64） */
    @Value("${app.rag.embedding.model}")
    private String embeddingModelName;

    // ==================== 私有方法 ====================

    /**
     * 把文档置为 FAILED，并记录失败原因。
     *
     * @param documentId 文档 ID
     * @param message    失败原因，可为 null（兜底文案“入库失败”）
     */
    private void markFailed(Long documentId, String message) {
        // error_message 列上限 1024，必须截断，否则超长会导致二次写库失败
        String msg = message == null ? "入库失败" : message;
        if (msg.length() > 1024) {
            msg = msg.substring(0, 1024);
        }
        kbDocumentMapper.update(null,
                new LambdaUpdateWrapper<KbDocument>()
                        .eq(KbDocument::getId, documentId)
                        .set(KbDocument::getStatus, KbDocumentStatus.FAILED)
                        .set(KbDocument::getErrorMessage, msg));
    }

    /**
     * 分批调用 Embedding 模型，把每个分块转成向量。
     *
     * <p>
     * 顺序保证：按 chunks 顺序切批、按返回顺序追加，最终结果与 chunks 一一对应。
     * </p>
     * 
     * @param documentTitle 文档标题，参与入模文本
     * @param chunks        已落库并带自增 id 的分块列表
     * @return 与 chunks 顺序一致的向量列表
     */
    private List<Embedding> embedAll(String documentTitle, List<KbChunk> chunks) {
        List<Embedding> all = new ArrayList<>(chunks.size());
        // 每批最多 batchSize 条，避免单次请求体过大或触发接口限流
        for (int from = 0; from < chunks.size(); from += batchSize) {
            int to = Math.min(chunks.size(), from + batchSize);
            // 入模文本 = 标题 + 正文（标题提升召回，但不落库）
            List<TextSegment> segments = chunks.subList(from, to).stream()
                    .map(chunk -> TextSegment.from(embedText(documentTitle, chunk)))
                    .toList();
            // content() 为响应中的向量列表，顺序与 segments 一致
            all.addAll(embeddingModel.embedAll(segments).content());
        }
        return all;
    }

    /**
     * 组装送入 Embedding 模型的文本。
     *
     * <p>
     * 文档标题是“风格名“唯一强信号，带上章节标题可以提升召回；入库只保存正文，标题单独存 t_kb_chunk.heading。
     * </p>
     * 
     * @param documentTitle 文档标题
     * @param chunk         分块（标题可能为 null）
     * @return 入模文本
     */
    private String embedText(String documentTitle, KbChunk chunk) {
        // 返回：文档标题+章节标题+正文
        return documentTitle + "\n" + (chunk.getHeading() == null ? "" : chunk.getHeading() + "\n")
                + chunk.getContent();
    }

    /**
     * 计算分块内容指纹（SHA-256 小写十六进制），用于块级去重与内容校验。
     *
     * @param text 分块正文
     * @return 64 位十六进制字符串
     */
    private String sha256Hex(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("不支持SHA-256", e);
        }
    }

    /**
     * 把入库任务提交到线程池。
     *
     * <p>
     * 队列满时抛出 TaskRejectedException：这里捕获后直接把文档置 FAILED。
     * 不能静默丢弃，否则文档会永远停在 PENDING。
     * </p>
     *
     * @param documentId 文档 ID
     */
    @Override
    public void submit(Long documentId) {
        try {
            kbExecutor.execute(() -> ingest(documentId));
        } catch (TaskRejectedException e) {
            // 队列满：不要静默丢弃，否则永远卡PENDING
            log.warn("入库队列已满，documentId={}", documentId);
            markFailed(documentId, "入库任务队列已满，请稍后重试");
        }
    }

    /**
     * 执行一次入库：领取任务 → 分块写 MySQL → 批量向量化 → 写 PG → 置 READY。
     *
     * <p>
     * 任何一步异常都会落到 FAILED 并记录 error_message；已写入的半套数据
     * 由后续同版本重跑（先删后插）清理。
     * </p>
     *
     * @param documentId 文档 ID
     */
    @Override
    public void ingest(Long documentId) {
        // 1. CAS 领取任务：只有状态仍是 PENDING 才能改成 PARSING，防止并发重复处理
        boolean claimed = kbDocumentMapper.update(null,
                new LambdaUpdateWrapper<KbDocument>()
                        .eq(KbDocument::getId, documentId)
                        .eq(KbDocument::getStatus, KbDocumentStatus.PENDING)
                        .set(KbDocument::getStatus, KbDocumentStatus.PARSING)) > 0;
        if (!claimed) {
            log.info("文档{}已被处理或状态不符，跳过", documentId);
            return;
        }
        try {
            // 读取文档元数据（此时状态已被置为 PARSING）
            KbDocument doc = kbDocumentMapper.selectById(documentId);
            if (doc == null) {
                throw new IllegalStateException("文档不存在：" + documentId);
            }
            // TEXT直接取落库文档；FILE从oss拉回源文件，再Tika解析
            String rawContent = null;
            switch (doc.getSourceType()) {
                case TEXT:
                    rawContent = doc.getRawContent();
                    break;
                case FILE:
                    rawContent = kbTextExtractor.extract(doc.getFileExt(),
                            fileStorageService.downloadDocument(doc.getOssUrl()));
                    break;
                case URL:
                    throw new IllegalStateException("URL来源解析待实现");
            }

            if (rawContent == null || rawContent.isBlank()) {
                throw new IllegalStateException("无法切块");
            }
            // 版本号决定分块与向量的分组，重建索引时会 +1
            int version = doc.getVersion() == null ? 1 : doc.getVersion();
            List<KbTextChunker.ChunkDraft> drafts = KbTextChunker.split(rawContent, maxChars, overlapChars);
            if (drafts.isEmpty()) {
                throw new IllegalStateException("切块结果为空");
            }
            // 2. 草稿转实体并批量写入 MySQL 分块表（chunk_no 决定检索时的顺序）
            List<KbChunk> chunks = new ArrayList<>(drafts.size());
            for (int i = 0; i < drafts.size(); i++) {
                KbTextChunker.ChunkDraft draft = drafts.get(i);
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(documentId);
                chunk.setVersion(version);
                chunk.setChunkNo(i);
                chunk.setHeading(draft.heading());
                chunk.setContent(draft.content());
                chunk.setContentHash(sha256Hex(draft.content()));
                chunk.setCharCount(draft.content().length());
                // token 数为估算值（中文约 0.7 token/字），仅用于上下文预算参考
                chunk.setTokenCount((int) Math.ceil(draft.content().length() * 0.7));
                chunk.setIsActive(true);
                chunks.add(chunk);
            }
            kbChunkMapper.insertBatch(chunks);
            // 3. 回查刚写入的分块（insertBatch 不回填自增 id，而向量表需要 chunk_id）
            List<KbChunk> saved = kbChunkMapper.selectList(
                    new LambdaQueryWrapper<KbChunk>()
                            .eq(KbChunk::getDocumentId, documentId)
                            .eq(KbChunk::getVersion, version)
                            .eq(KbChunk::getIsActive, true)
                            .orderByAsc(KbChunk::getChunkNo));
            // 4. 批量计算向量（按批调用 Embedding 接口，顺序与 saved 一致）
            List<Embedding> embeddings = embedAll(doc.getTitle(), saved);
            // 5. 写 PG：先删同版本旧向量再插，保证同版本重跑幂等
            kbVectorStore.replace(documentId, version, saved, embeddings, embeddingModelName);
            // 6. 所有数据就绪后才置 READY（READY 表示已可被检索）
            kbDocumentMapper.update(
                    new LambdaUpdateWrapper<KbDocument>()
                            .eq(KbDocument::getId, documentId)
                            .set(KbDocument::getStatus, KbDocumentStatus.READY)
                            .set(KbDocument::getChunkCount, saved.size()));
            log.info("文档{}入库完成，chunkCount={}", documentId, saved.size());

        } catch (Exception e) {
            // 任何异常都要落 FAILED，否则文档会永久停在 PARSING / INDEXING
            log.error("文档{}入库失败", documentId, e);
            markFailed(documentId, e.getMessage());
        }
    }

}
