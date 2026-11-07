package com.housedesign.common;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.housedesign.entity.KbChunk;

import dev.langchain4j.data.embedding.Embedding;
import lombok.RequiredArgsConstructor;

/**
 * 向量库写入组件：负责把分块向量落到 PostgreSQL 的 kb_chunk_vector 表。
 *
 * <p>
 * 与 MySQL 的分块表通过 chunk_id 一一对应：
 * t_kb_chunk 是权威正文，kb_chunk_vector 只是可随时重建的语义索引。
 * </p>
 *
 * <p>
 * 注意：这里不走 Spring 事务。MySQL 与 PG 是两个数据源，
 * 一致性通过「先删同版本、再插入」保证幂等，失败由上层状态机置 FAILED。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class KbVectorStore {

    /** PG 向量库专用 JdbcTemplate；字段名必须与 Bean 名一致才能注入正确数据源 */
    private final JdbcTemplate pgVectorJdbcTemplate;

    /**
     * 覆盖写入某文档某版本的全部向量。
     *
     * @param documentId 文档 ID
     * @param version    文档版本号，用于新旧隔离
     * @param chunks     分块列表（必须已带自增 id）
     * @param embeddings 与 chunks 顺序一致的向量列表
     * @param model      生成向量的模型名，写入 varchar(64) 列
     */
    public void replace(Long documentId, int version, List<KbChunk> chunks,
            List<Embedding> embeddings, String model) {
        // 防御：model 列是 varchar(64)，传错值（比如误传原文）会在 PG 侧报晦涩的长度错误
        if (model == null || model.length() > 64) {
            throw new IllegalArgumentException("向量 model 名长度非法: " + model);
        }
        // 先删同版本旧向量，再插：失败重跑不会留下脏行，也不会撞 chunk_id 主键
        pgVectorJdbcTemplate.update(
                "DELETE FROM kb_chunk_vector WHERE document_id = ? and version = ?",
                documentId, version);
        for (int i = 0; i < chunks.size(); i++) {
            // 参数顺序：chunk_id / document_id / version / 向量文本 / 模型名
            pgVectorJdbcTemplate.update(
                    "INSERT INTO kb_chunk_vector (chunk_id,document_id,version,embedding,model)" +
                            "VALUES(?,?,?,CAST(? AS VECTOR),?)",
                    chunks.get(i).getId(), documentId, version,
                    // JDBC 只能传字符串，因此把 float[] 拼成 pgvector 的文本格式，再 CAST 成 vector
                    toLiteral(embeddings.get(i).vector()), model);
        }
    }

    /**
     * 删除某文档的全部向量（文档删除时调用）。
     *
     * @param documentId 文档 ID
     */
    public void deleteByDocument(Long documentId) {
        pgVectorJdbcTemplate.update(
                "DELETE FROM kb_chunk_vector WHERE document_id = ?", documentId);
    }

    /**
     * 删除某文档除指定版本外的历史向量（重建索引成功后调用）。
     *
     * @param documentId  文档 ID
     * @param keepVersion 需要保留的版本号（当前生效版本）
     */
    public void deleteVersionsExcept(Long documentId, int keepVersion) {
        pgVectorJdbcTemplate.update(
                "DELETE FROM kb_chunk_vector WHERE document_id = ? and version <> ?",
                documentId, keepVersion);
    }

    /**
     * 把 float[] 转成 pgvector 的文本输入格式，例如 {@code [0.1,0.2,0.3]}。
     *
     * @param vector 1024 维浮点数组
     * @return 可直接 CAST 成 vector 的字符串
     */
    public static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 6).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
