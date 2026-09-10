package com.housedesign.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// 配置MybatisPlus分页插件
@Configuration
public class MybatisPlusConfig {

    @Bean
    
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 配置MybatisPlus分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 开启分页优化
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
