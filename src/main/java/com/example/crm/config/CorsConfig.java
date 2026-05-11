package com.example.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有接口路径
                .allowedOriginPatterns("*") // 允许所有前端域名（开发环境用*，生产环境指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 必须包含OPTIONS（处理预检请求）
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许携带Cookie/Token
                .maxAge(3600); // 预检请求缓存1小时，减少重复请求
    }
}