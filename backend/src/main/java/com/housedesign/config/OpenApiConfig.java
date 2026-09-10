package com.housedesign.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 配置：文档元信息 + Bearer JWT 鉴权说明。
 * 访问：http://localhost:8080/swagger-ui.html
 * JSON：http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI houseDesignOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("筑梦家 HouseDesign API")
                        .description("房屋装修效果可视化平台后端接口文档")
                        .version("1.0.0"))
                // 全局声明需要 Bearer Token（登录/注册、/files/** 除外）
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 token，格式：Bearer <token>")));
    }
}
