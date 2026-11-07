package com.housedesign.Service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.housedesign.entity.KbDocument;
import com.housedesign.entity.KbDocumentStatus;
import com.housedesign.mapper.KbDocumentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知识库入库超时对账。
 *
 * <p>
 * 进程崩溃 / 线程被杀时，PENDING、PARSING、INDEXING 的文档会永远停在中间态，
 * 既不能被检索，也不会再有线程推进它。本组件扫描 updated_at 早于
 * now-{staleSeconds} 的中间态文档置为 FAILED，让用户可以在列表里看到原因并重新触发入库。
 * </p>
 *
 * <p>
 * staleSeconds 必须显著大于「单批 Embedding 的耗时」：入库链路每写完一批向量都会刷新
 * updated_at（心跳），只有心跳停了才算卡死。阈值取默认 30 分钟，避免误杀大文档。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KbIngestReconciler {

    /** 超时收口文案：必须让用户知道可以去重建索引重试 */
    private static final String STALE_MESSAGE = "入库超时（任务长时间无进展），请重新入库";

    /** 单轮最多处理条数，避免一次启动把大表全拉出来 */
    private static final int BATCH_LIMIT = 100;

    /** 文档 Mapper：扫描中间态并回写 FAILED */
    private final KbDocumentMapper kbDocumentMapper;

    /** 中间态判定阈值（秒），配置项 app.rag.ingest.reconcile-stale-seconds */
    @Value("${app.rag.ingest.reconcile-stale-seconds:1800}")
    private long staleSeconds;

    /**
     * 定时对账：每 60s 一轮，启动 10s 后首轮。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 10_000L)
    public void reconcileStaleTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(staleSeconds);
        List<KbDocument> stale = kbDocumentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>()
                        .in(KbDocument::getStatus,
                                List.of(KbDocumentStatus.PENDING,
                                        KbDocumentStatus.PARSING,
                                        KbDocumentStatus.INDEXING))
                        .lt(KbDocument::getUpdatedAt, threshold)
                        .last("LIMIT " + BATCH_LIMIT));
        if (stale.isEmpty()) {
            return;
        }
        int done = 0;
        for (KbDocument doc : stale) {
            // 条件更新：只对仍是中间态的文档收口，避免把刚好跑完置 READY 的文档覆盖成 FAILED
            int updated = kbDocumentMapper.update(null,
                    new LambdaUpdateWrapper<KbDocument>()
                            .eq(KbDocument::getId, doc.getId())
                            .in(KbDocument::getStatus,
                                    List.of(KbDocumentStatus.PENDING,
                                            KbDocumentStatus.PARSING,
                                            KbDocumentStatus.INDEXING))
                            .set(KbDocument::getStatus, KbDocumentStatus.FAILED)
                            .set(KbDocument::getErrorMessage, STALE_MESSAGE));
            if (updated > 0) {
                done++;
                log.warn("知识库入库对账：置 FAILED id={}, lastStatus={}, updatedAt={}",
                        doc.getId(), doc.getStatus(), doc.getUpdatedAt());
            }
        }
        log.warn("知识库入库对账完成，本轮置 FAILED 文档数={}", done);
    }

    /**
     * 应用启动就绪后先扫一次，把上次进程遗留的中间态任务立即收口，
     * 不必等首轮定时任务。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconcileStaleTasks();
    }
}
