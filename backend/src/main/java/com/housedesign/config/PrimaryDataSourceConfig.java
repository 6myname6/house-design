package com.housedesign.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/**
 * MySQL 主数据源显式声明为 @Primary。
 * 原因：引入 PG 向量数据源后容器中存在两个 DataSource，
 * MyBatis-Plus 自动配置依赖 @ConditionalOnSingleCandidate，
 * 不标主数据源会导致 SqlSessionFactory 不装配。
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
