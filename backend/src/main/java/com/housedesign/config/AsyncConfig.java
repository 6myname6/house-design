package com.housedesign.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    @Bean(name = "generationExecutor")
    public ThreadPoolTaskExecutor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);// 同时1个生成任务
        executor.setMaxPoolSize(2);// 排队多了最多扩到2
        executor.setQueueCapacity(100);// 等待队列
        executor.setThreadNamePrefix("gen-");// 线程名前缀
        // 队列满时由调用线程抛 TaskRejectedException（默认即 AbortPolicy，这里显式声明：
        // 业务层据此把 PENDING 任务置 FAILED 并返回 503，避免任务永久卡 PENDING）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 优雅停机：应用关闭时最多等待 60s 让在跑的生成任务走完状态机，不暴力杀线程
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 知识库入库专用线程池。
     *
     * <p>与出图线程池隔离：入库包含 HTTP 调用（Embedding）和双库写入，
     * 与生成任务抢线程会导致两边都变慢。</p>
     *
     * @return 入库线程池，Bean 名 kbExecutor（按字段名注入）
     */
    @Bean(name = "kbExecutor")
    public ThreadPoolTaskExecutor kbExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);// 同时只跑1个入库任务
        executor.setMaxPoolSize(2);// 积压时最多扩容到2
        executor.setQueueCapacity(100);// 排队上限
        executor.setThreadNamePrefix("kb-");// 线程名前缀，日志里可区分
        // 队列满了抛TaskRejectedException，业务层兜底置 FAILED，避免任务静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 优雅停机：应用关闭时最多等待 30s，让在跑的入库任务收尾
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
