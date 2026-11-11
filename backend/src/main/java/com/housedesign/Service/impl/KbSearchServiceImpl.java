package com.housedesign.Service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.Service.KbSearchService;
import com.housedesign.Utils.UserContext;
import com.housedesign.common.kb.KbReranker;
import com.housedesign.common.kb.KbVectorSearcher;
import com.housedesign.common.kb.KbVectorSearcher.Candidate;
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

/**
 * 知识库搜索服务实现类
 * 提供知识库文档的搜索功能，包括向量召回、关键词召回、融合排序、rerank等
 * 
 * @author lzh
 */
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
    // RRF平滑常数：经验值 60，用来压平头部单条的优势
    private static final int RRF_K = 60;
    @Value("${app.rag.search.min-score}")
    private double defaultMinScore;
    /* 关键词召回条数 */
    @Value("${app.rag.search.keyword-top-k}")
    private int keywordTopK;
    /* 关键词候选余弦下限 */
    @Value("${app.rag.search.keyword-cosine-floor}")
    private double keywordCosineFloor;
    /** 向量召回条数 */
    @Value("${app.rag.search.vector-top-k}")
    private int vectorTopK;
    @Value("${app.rag.search.rerank-min-score}")
    private double rerankMinscore;
    /** 进入RerankCandidate的融合候选条数 */
    @Value("${app.rag.search.rerank-candidates}")
    private int rerankCandidates;
    // Rerank开关：关闭时直接走纯向量降级路径
    @Value("${app.rag.rerank.enabled}")
    private boolean rerankEnabled;
    private final KbVectorSearcher kbVectorSearcher;
    private final KbChunkMapper kbChunkMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbReranker kbReranker;

    /**
     * 主业务方法
     * 
     * 1. 从请求参数中解析搜索关键词、知识库ID、返回结果数量、相似度下限
     * 2. 解析用户可见的文档范围
     * 3. 调用向量搜索器和关键词召回器获取候选文档
     * 4. 合并候选文档，按RRF倒序排序
     * 5. 精排：成功由rerank分数决定拒答，失败才降级
     * 6. 降级：纯向量+余弦阈值，保持阶段一的拒答口径
     * 
     * @param request 搜索请求参数
     * @return 搜索响应结果
     */
    @Override
    public KbSearchResponse search(KbSearchRequest request) {
        Long userId = UserContext.getUserId();
        int topK = clampTopK(request.getTopK());
        double minScore = request.getMinScore() == null ? defaultMinScore : request.getMinScore();

        List<Long> scope = resolveScope(userId, request.getDocumentIds());// 限定文档范围(documentIds)
        if (scope.isEmpty()) {
            return new KbSearchResponse(request.getQuery(), false, List.of());
        }
        // 拿问题向量，用于向量召回和关键词召回
        float[] queryVector = kbVectorSearcher.embedQuery(request.getQuery());
        // 1.两路召回：向量看语义，关键词看精确词
        int prefetch = Math.min(MAX_CANDIDATES, Math.max(vectorTopK, topK * PREFETCH_FACTOR));
        List<Candidate> vectorList = kbVectorSearcher.topK(queryVector, scope, prefetch);
        List<Candidate> keywordList = keywordCandidates(request.getQuery(), scope, queryVector);
        // 2.RRF融合：两路排名合并，任一路为空都不影响
        List<FusionItem> fused = fuse(vectorList, keywordList);
        if (fused.isEmpty()) {
            return new KbSearchResponse(request.getQuery(), false, List.of());
        }
        // 3.取前N条精排，N至少覆盖topK
        List<FusionItem> pool = fused.stream().limit(Math.max(rerankCandidates, topK)).toList();
        // 4.精排：成功由rerank分数决定拒答，失败才降级
        if (rerankEnabled) {
            RerankOutcome outcome = rerankAndBuild(request.getQuery(), pool, topK);
            if (outcome.applied()) {
                return new KbSearchResponse(request.getQuery(), !outcome.hits().isEmpty(), outcome.hits());
            }
        }
        // 5.降级：纯向量+余弦阈值，保持阶段一的拒答口径
        List<Candidate> passed = vectorList.stream()
                .filter(c -> c.score() >= minScore)
                .limit(topK)
                .toList();
        List<KbSearchHit> hits = toHits(passed);
        return new KbSearchResponse(request.getQuery(), !hits.isEmpty(), hits);
    }

    /**
     * 融合候选：同一条 chunk 可能同时出现在两路召回里，RRF 要累加，所以用可变类而不是 record
     * FusionItem
     * 
     * @param chunkId    分块id
     * @param documentId 文档id
     * @param cosine     余弦分
     * @param rrf        RRF累加分
     */
    private static final class FusionItem {
        private final Long chunkId;
        private final Long documentId;
        private double cosine;// 两路里取最大的余弦分
        private double rrf; // RRF累加分

        private FusionItem(Long chunkId, Long documentId, double cosine) {
            this.chunkId = chunkId;
            this.cosine = cosine;
            this.documentId = documentId;
        }
    }

    /**
     * Rerank执行结果，applied=false表示启动失败或未启动，需要降级
     * RerankOutcome
     * 
     * @param applied 是否成功应用rerank，false表示启动失败或未启动，需要降级
     * @param hits    精排结果列表，包含搜索召回的候选文档，经过转换后用于返回给上层调用方
     */
    private record RerankOutcome(boolean applied, List<KbSearchHit> hits) {
    }

    /** 私有方法 */

    /**
     * 限制TopK的范围
     * 
     * @param topK 搜索结果数量参数
     * @return 限制后的TopK值
     */
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

    /**
     * （ 把用户可见+ready+可选documentIds ）收敛成PG的in列表
     * 
     * @param userId     当前用户id
     * @param requestIds 可选文档id列表，用于筛选可见文档
     * @return 筛选后的文档id列表，用于PG的in查询
     */
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

    /**
     * 将候选结果列表转换为知识库搜索命中结果列表
     *
     * @param candidates 候选结果列表，包含搜索召回的候选文档
     * @return 知识库搜索命中结果列表，经过转换后用于返回给上层调用方
     */
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
                    chunk.getHeading(), truncate(chunk.getContent()), roundScore(c.score()), null);
        }).filter(Objects::nonNull).toList();
    }

    /**
     * 保留两位小数
     * 
     * @param score 待保留两位小数的分数
     * @return 保留两位小数后的分数
     */
    private double roundScore(double score) {
        return Math.round(score * 100) / 100.0;
    }

    /**
     * 正文按字符数截断，避免整块正文塞给前端/模型
     * 
     * @param content 待截断的正文内容
     * @return 截断后的正文内容
     */
    private String truncate(String content) {
        if (content == null || content.length() <= MAX_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_CHARS);
    }

    /**
     * 关键词召回：先查FULLTEXT 拿排名，再补余弦
     * 
     * @param query       搜索关键词
     * @param documentIds 限定文档范围
     * @param queryVector 查询向量
     * @return 候选文档列表
     */
    private List<Candidate> keywordCandidates(String query, List<Long> documentIds, float[] queryVector) {
        if (query.trim().length() < 2) {
            return List.of();
        }
        try {
            List<KbChunk> chunks = kbChunkMapper.keywordSearch(query, documentIds, keywordTopK);
            if (chunks.isEmpty()) {
                return List.of();
            }
            List<Long> ids = chunks.stream().map(KbChunk::getId).toList();
            Map<Long, Double> scores = kbVectorSearcher.scoresByChunkIds(queryVector, ids);
            return chunks.stream()
                    .map(c -> new Candidate(c.getId(), c.getDocumentId(),
                            scores.getOrDefault(c.getId(), -1.0)))
                    .filter(c -> c.score() >= keywordCosineFloor)
                    .toList();
        } catch (Exception e) {
            log.warn("关键词召回失败，降级为纯向量检索 query = {}", query, e);
            return List.of();
        }
    }

    /**
     * 按排名累加RRF:rank从1开始，k=60 抑制头部单条的绝对优势
     * 
     * @param table 累加表，key=分块id，value=累加项，包含余弦分和RRF累加分
     * @param list  候选文档列表
     */
    private void accumulate(Map<Long, FusionItem> table, List<Candidate> list) {
        for (int i = 0; i < list.size(); i++) {
            Candidate c = list.get(i);
            double inc = 1.0 / (RRF_K + i + 1);
            FusionItem item = table.computeIfAbsent(c.chunkId(),
                    id -> new FusionItem(id, c.documentId(), c.score()));
            item.rrf += inc;
            item.cosine = Math.max(item.cosine, c.score());
        }
    }

    /**
     * 两路合并后按RRF倒序；只把前N条送去rerank，控制精排成本
     * 
     * @param vectorList  向量召回候选文档列表
     * @param keywordList 关键词召回候选文档列表
     * @return 合并后的候选文档列表
     */
    private List<FusionItem> fuse(List<Candidate> vectorList, List<Candidate> keywordList) {
        Map<Long, FusionItem> table = new LinkedHashMap<>();
        accumulate(table, vectorList);
        accumulate(table, keywordList);
        return table.values().stream()
                .sorted(Comparator.comparingDouble((FusionItem it) -> it.rrf).reversed())
                .toList();
    }

    /**
     * 精排输入 = 文档标题+章节标题+正文，标题帮模型区分同质风格卡
     * 
     * @param kbChunk 分块
     * @param doc     所属文档
     * @return 送精排的文本
     */
    private String chunkText(KbChunk kbChunk, KbDocument doc) {
        StringBuilder sb = new StringBuilder();
        if (doc != null && doc.getTitle() != null) {
            sb.append(doc.getTitle()).append('\n');
        }
        if (kbChunk.getHeading() != null) {
            sb.append(kbChunk.getHeading()).append('\n');
        }
        return sb.append(kbChunk.getContent()).toString();
    }

    /**
     * 精排并构建结果
     * 
     * @param query 搜索关键词
     * @param pool  候选文档列表
     * @param topK  精排结果条数
     * @return 精排结果
     */
    private RerankOutcome rerankAndBuild(String query, List<FusionItem> pool, int topK) {
        // 1.回查分块，文档，拼候选文本
        List<Long> ids = pool.stream().map(it -> it.chunkId).toList();
        Map<Long, KbChunk> chunkMap = kbChunkMapper.selectBatchIds(ids).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toMap(KbChunk::getId, c -> c));
        List<FusionItem> valid = pool.stream().filter(it -> chunkMap.containsKey(it.chunkId)).toList();
        if (valid.isEmpty()) {
            return new RerankOutcome(false, List.of());
        }
        Map<Long, KbDocument> docMap = kbDocumentMapper.selectBatchIds(
                chunkMap.values().stream().map(KbChunk::getDocumentId).distinct().toList()).stream()
                .collect(Collectors.toMap(KbDocument::getId, d -> d));
        // 2.调用模型，阈值过滤
        List<String> documents = valid.stream()
                .map(it -> chunkText(chunkMap.get(it.chunkId),
                        docMap.get(chunkMap.get(it.chunkId).getDocumentId())))
                .toList();
        List<KbReranker.Result> ranked = kbReranker.rerank(query, documents);
        if (ranked.isEmpty()) {
            return new RerankOutcome(false, List.of());
        }
        List<KbSearchHit> hits = new ArrayList<>(topK);
        for (KbReranker.Result r : ranked) {
            FusionItem item = valid.get(r.index());
            // 双信号拒答：rerank低只说明表述差异大，余弦高说明语义相近，不能一刀切
            // rerank高但余弦低，是关键词精确命中补足了语义鸿沟
            if (r.score() < rerankMinscore && item.cosine < defaultMinScore) {
                continue;
            }
            KbChunk chunk = chunkMap.get(item.chunkId);
            KbDocument doc = docMap.get(chunk.getDocumentId());
            String title = doc == null ? null : doc.getTitle();
            hits.add(new KbSearchHit(chunk.getId(), chunk.getDocumentId(), title, chunk.getHeading(),
                    truncate(chunk.getContent()), roundScore(item.cosine), roundScore(r.score())));
            if (hits.size() >= topK) {
                break;
            }
        }
        return new RerankOutcome(true, hits);
    }

}
