package com.housedesign.config;

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
        executor.initialize();
        return executor;
    }

}
