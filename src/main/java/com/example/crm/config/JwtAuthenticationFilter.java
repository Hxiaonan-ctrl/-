package com.example.crm.config;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器（修复注册接口403问题）
 */
//@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = null;
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            token = authHeader.substring(7).trim();

            if (!JWTUtil.verify(token, jwtSecret.getBytes())) {
                logger.warn("JWT Token无效（签名错误/已过期）：{}", token);
                filterChain.doFilter(request, response);
                return;
            }

            JWT jwt = JWTUtil.parseToken(token);
            String loginName = (String) jwt.getPayload("sub");
            String role = (String) jwt.getPayload("role");

            if (loginName == null || role == null) {
                logger.warn("JWT Token中缺少sub(loginName)或role字段，token：{}", token);
                filterChain.doFilter(request, response);
                return;
            }

            // 注意：SecurityConfig中角色层级是"admin > sales > finance"（无ROLE_前缀）
            // 此处需移除ROLE_前缀，保证权限匹配
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            UserDetails userDetails = User.withUsername(loginName)
                    .password("")
                    .authorities(Collections.singletonList(authority))
                    .build();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JWTException e) {
            logger.error("JWT Token解析失败，token：{}", token, e);
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("JWT过滤器处理异常", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase().trim();
        // 覆盖所有可能的路径形式
        return path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/api/auth/login/")
                || path.startsWith("/api/auth/register/");
    }
}