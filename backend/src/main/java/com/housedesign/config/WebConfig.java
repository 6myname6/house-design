package com.housedesign.config;

import com.housedesign.interceptor.JwtInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    // 存储后端类型：只有本地磁盘存储才需要把 /files/** 映射成静态资源
    @Value("${app.storage.type:local}")
    private String storageType;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login", "/api/auth/register",
                        "/api/auth/sms/code", "/api/auth/sms/login",
                        "/files/**",
                        // Swagger/OpenAPI 文档不鉴权
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // type=oss 时图片由 OSS 公网域名直出，本地这个映射用不到，注册了反而会掩盖配置错误
        if (!"local".equals(storageType)) {
            return;
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./storage/");
    }
}
