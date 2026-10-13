package com.housedesign;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 筑梦家 · 房屋装修设计网站 —— 后端启动类（复现版）
 */
@SpringBootApplication
@MapperScan("com.housedesign.mapper")
@EnableScheduling // 启用定时任务（生成任务超时对账 GenerationReconciler）
public class HouseDesignApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseDesignApplication.class, args);
    }
}
