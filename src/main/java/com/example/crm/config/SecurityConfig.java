package com.example.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 跨域配置：适配前端5173端口，解决跨域请求拦截
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 测试环境允许前端5173端口，生产可替换为具体域名
        config.setAllowedOriginPatterns(Arrays.asList("http://localhost:5173"));
        // 允许所有HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        config.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带Cookie（如JWT令牌）
        config.setAllowCredentials(true);
        // 预检请求缓存时间，减少OPTIONS请求
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效跨域配置
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 核心安全规则：完全放行所有请求，适配测试环境
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())          // 关闭CSRF（前后端分离必关）
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 启用自定义跨域配置
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()       // 测试环境放行所有请求
                )
                .formLogin(form -> form.disable())     // 关闭默认表单登录（避免生成默认密码）
                .httpBasic(basic -> basic.disable());  // 关闭HTTP Basic认证（避免弹窗）

        return http.build();
    }
}