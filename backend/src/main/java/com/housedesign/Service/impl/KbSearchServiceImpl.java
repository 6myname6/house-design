package com.housedesign.Service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.KbSearchService;
import com.housedesign.Utils.UserContext;
import com.housedesign.common.KbVectorSearcher;
import com.housedesign.common.KbVectorSearcher.Candidate;
import com.housedesign.dto.request.KbSearchRequest;
import com.housedesign.dto.response.KbSearchHit;
import com.housedesign.dto.response.KbSearchResponse;
import com.housedesign.entity.KbChunk;
import com.housedesign.entity.KbDocument;
import com.housedesign.entity.KbDocumentStatus;
import com.housedesign.entity.KbVisibility;
import com.housedesign.mapper.KbChunkMapper;
import com.housedesign.mapper.KbDocumentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j

public class KbSearchServiceImpl implements KbSearchService {
    // 默认返回条数
    private static final int DEFAULT_TOP_K = 5;
    // topK上限，
    private static final int MAX_TOP_K = 20;
    // 预取倍数
    private static final int PREFETCH_FACTOR = 3;
    // 单次检索候选上限，防止IN列表很大时，limit拖慢HNSW查询
    private static final int MAX_CANDIDATES = 60;
    // 返回正文字符的上限，避免整块正文塞给前端/模型
    private static final int MAX_CONTENT_CHARS = 500;
    @Value("${app.rag.search.min-score}")
    private double defaultMinScore;

    private final KbVectorSearcher kbVectorSearcher;
    private final KbChunkMapper kbChunkMapper;
    private final KbDocumentMapper kbDocumentMapper;

    /** 私有方法 */

    // 限制TopK的范围
    private int clampTopK(Integer topK) {
        if (topK == null) {
            // 为空返回默认值
            return DEFAULT_TOP_K;
        }
        if (topK < 1) {
            // topK非法，返回1
            return 1;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    // （ 把用户可见+ready+可选documentIds ）收敛成PG的in列表
    private List<Long> resolveScope(Long userId, List<Long> requestIds) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<KbDocument>()
                .select(KbDocument::getId)
                .eq(KbDocument::getStatus, KbDocumentStatus.READY)
                .and(q -> q.eq(KbDocument::getVisibility, KbVisibility.PUBLIC)
                        .or().eq(KbDocument::getOwnerUserId, userId));
        if (requestIds != null && !requestIds.isEmpty()) {
            wrapper.in(KbDocument::getId, requestIds);
        }
        return kbDocumentMapper.selectList(wrapper).stream().map(KbDocument::getId).toList();
    }

    private List<KbSearchHit> toHits(List<Candidate> candidates) {
        List<Long> chunkIds = candidates.stream().map(KbVectorSearcher.Candidate::chunkId).toList();
        Map<Long, KbChunk> chunkMap = kbChunkMapper.selectBatchIds(chunkIds).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toMap(KbChunk::getId, c -> c));

        List<Long> docIds = chunkMap.values().stream().map(KbChunk::getDocumentId).distinct().toList();
        Map<Long, KbDocument> docMap = docIds.isEmpty() ? Map.of()
                : kbDocumentMapper.selectBatchIds(docIds).stream()
                        .collect(Collectors.toMap(KbDocument::getId, d -> d));
        return candidates.stream().map(c -> {
            KbChunk chunk = chunkMap.get(c.chunkId());
            if (chunk == null) {
                return null;
            }
            KbDocument doc = docMap.get(chunk.getDocumentId());
            return new KbSearchHit(chunk.getId(), chunk.getDocumentId(),
                    doc == null ? null : doc.getTitle(),
                    chunk.getHeading(), truncate(chunk.getContent()), roundScore(c.score()));
        }).filter(Objects::nonNull).toList();
    }

    // 保留两位小数
    private double roundScore(double score) {
        return Math.round(score * 100) / 100.0;
    }

    // 正文按字符数截断，避免整块正文塞给前端/模型
    private String truncate(String content) {
        if (content == null || content.length() <= MAX_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_CHARS);
    }

    /** 主业务方法 */
    @Override
    public KbSearchResponse search(KbSearchRequest request) {
        Long userId = UserContext.getUserId();
        int topK = clampTopK(request.getTopK());
        double minScore = request.getMinScore() == null ? defaultMinScore : request.getMinScore();

        List<Long> scope = resolveScope(userId, request.getDocumentIds());
        if (scope.isEmpty()) {
            return new KbSearchResponse(request.getQuery(), false, List.of());
        }

        float[] queryVector = kbVectorSearcher.embedQuery(request.getQuery());
        // 预取3倍：阈值过滤和失效块都会消耗候选，留余量
        int prefetch = Math.min(MAX_CANDIDATES, topK * PREFETCH_FACTOR);
        List<KbVectorSearcher.Candidate> candidates = kbVectorSearcher.topK(queryVector, scope, prefetch);
        // 用原始分数比较阈值，不先四舍五入
        List<KbVectorSearcher.Candidate> passed = candidates.stream()
                .filter(c -> c.score() >= minScore)
                .limit(topK)
                .toList();
        if (passed.isEmpty()) {
            return new KbSearchResponse(request.getQuery(), false, List.of());
        }
        List<KbSearchHit> hits = toHits(passed);
        return new KbSearchResponse(request.getQuery(), !hits.isEmpty(), hits);
    }

}
