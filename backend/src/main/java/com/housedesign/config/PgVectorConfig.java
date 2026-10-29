package com.housedesign.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * RAG 向量库独立数据源：与 MySQL 主数据源隔离，
 * 不参与业务事务，只用于向量读写。
 */
@Configuration
@EnableConfigurationProperties(PgVectorProperties.class)
public class PgVectorConfig {

    @Bean(name = "pgVectorDataSource", destroyMethod = "close")
    public HikariDataSource pgVectorDataSource(PgVectorProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setConnectionTimeout(properties.getConnectionTimeoutMs());
        config.setPoolName("pg-vector-pool");
        return new HikariDataSource(config);
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(
            @Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
