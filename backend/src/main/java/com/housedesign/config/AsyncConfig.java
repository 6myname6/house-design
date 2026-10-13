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

}
