package com.housedesign.common.kb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;

/**
 * Pg向量检索：问题向量->余弦topK
 * KbVectorSearcher
 */
@Component
@RequiredArgsConstructor
public class KbVectorSearcher {
    private final JdbcTemplate pgVectorJdbcTemplate;
    private final EmbeddingModel embeddingModel;

    /** 检索候选：分块id +文档id +相似度 */
    public record Candidate(Long chunkId, Long documentId, double score) {
    }

    /** 把问题算成向量 */
    public float[] embedQuery(String query) {
        return embeddingModel.embed(query).content().vector();
    }

    /**
     * 指定文档范围选取余弦topK
     * 
     * @param vector     问题向量
     * @param documentId 文档id
     * @param limit      候选条数
     */
    public List<Candidate> topK(float[] vector, List<Long> documentIds, int limit) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        // pgvector不支持动态个数参数，文档id用in拼占位符，数量可控
        String placeHolders = String.join(",", Collections.nCopies(documentIds.size(), "?"));

        String sql = """
                select chunk_id ,document_id,1-(embedding<=>cast(? as vector)) as score
                from kb_chunk_vector
                where document_id in(%s)
                order by embedding<=> cast(? as vector)
                limit ?
                """.formatted(placeHolders);

        String literal = KbVectorStore.toLiteral(vector);
        List<Object> args = new ArrayList<>();
        args.add(literal);// 对应占位符 score
        args.addAll(documentIds);// 对应占位符IN
        args.add(literal);// 对应占位符order by
        args.add(limit);// 对应占位符limit
        return pgVectorJdbcTemplate.query(sql, args.toArray(),
                (rs, rowNum) -> new Candidate(rs.getLong("chunk_id"),
                        rs.getLong("document_id"), rs.getDouble("score")));
    }

    // 关键词候选没有向量分：按chunk_id批量回查余弦，供余弦下限过滤和统一
    public Map<Long, Double> scoresByChunkIds(float[] vector, List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Map.of();
        }
        String ph = String.join(",", Collections.nCopies(chunkIds.size(), "?"));
        String sql = "select chunk_id,1-(embedding<=>cast(? as vector)) as score " +
                "from kb_chunk_vector where chunk_id in(" + ph + ")";
        String literal = KbVectorStore.toLiteral(vector);
        List<Object> args = new ArrayList<>();
        args.add(literal);
        args.addAll(chunkIds);
        Map<Long, Double> scores = new HashMap<>(chunkIds.size());
        pgVectorJdbcTemplate.query(sql, args.toArray(), (rs) -> {
            scores.put(rs.getLong("chunk_id"), rs.getDouble("score"));
        });
        return scores;

    }

}
