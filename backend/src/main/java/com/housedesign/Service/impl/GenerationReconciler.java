package com.housedesign.Service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.housedesign.entity.GeneratedModel;
import com.housedesign.entity.GenerationStatus;
import com.housedesign.mapper.GeneratedModelMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成任务超时对账：进程崩溃/线程被杀时，PENDING/PROCESSING 任务可能永远停在中间态。
 *
 * 扫描 updated_at 早于 now-{staleSeconds} 的中间态任务置 FAILED。
 * staleSeconds 必须大于生成锁 TTL（默认 35min > 30min）：锁已自然过期的任务才对账，
 * 不盲删 Redis key，避免误杀"仍在跑、刚续过锁"的新任务。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GenerationReconciler {

    private static final String STALE_MESSAGE = "生成超时（任务长时间无进展），请重新生成";

    /** 生成中任务的 Redis 占位 key 前缀——与 GenerationServiceImpl.ACTIVE_KEY_PREFIX 保持一致 */
    private static final String ACTIVE_KEY_PREFIX = "generation:active:";

    private final GeneratedModelMapper generatedModelMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.generation.reconcile-stale-seconds:2100}")
    private long staleSeconds;

    /** 每 60s 对账一次（启动 10s 后首轮），单次最多处理 100 条 */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 10_000L)
    public void reconcileStaleTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(staleSeconds);
        List<GeneratedModel> stale = generatedModelMapper.selectList(
                new LambdaQueryWrapper<GeneratedModel>()
                        .in(GeneratedModel::getStatus,
                                List.of(GenerationStatus.PENDING, GenerationStatus.PROCESSING))
                        .lt(GeneratedModel::getUpdatedAt, threshold)
                        .last("LIMIT 100"));
        if (stale.isEmpty()) {
            return;
        }
        for (GeneratedModel task : stale) {
            // 活任务跳过：占位 key 在 TTL 内说明任务正在跑或刚提交（每完成一个房间续期一次），
            // 仅凭 updated_at 过旧就置 FAILED 会误杀长任务，随后任务跑完覆盖成 SUCCESS
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(ACTIVE_KEY_PREFIX + task.getProjectId()))) {
                log.info("超时对账跳过活任务：taskId={}, projectId={}（Redis 占位仍存在）",
                        task.getId(), task.getProjectId());
                continue;
            }
            GenerationStatus lastStatus = task.getStatus();
            task.setStatus(GenerationStatus.FAILED);
            task.setErrorMessage(STALE_MESSAGE);
            generatedModelMapper.updateById(task);
            log.warn("超时对账：任务置 FAILED，taskId={}, projectId={}, lastStatus={}, updatedAt={}",
                    task.getId(), task.getProjectId(), lastStatus, task.getUpdatedAt());
        }
        log.warn("超时对账完成，本轮置 FAILED 任务数={}", stale.size());
    }

    /** 应用启动就绪后先扫一次：把上次进程遗留的中间态任务收口（不依赖等首轮定时） */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconcileStaleTasks();
    }
}
