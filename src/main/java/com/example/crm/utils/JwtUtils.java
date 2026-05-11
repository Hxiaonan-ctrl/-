package com.example.crm.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    // JWT密钥（建议配置在application.yml中）
    @Value("${jwt.secret:crm-secret-key-1234567890-crm-secret-key}")
    private String secret;

    // Token过期时间：2小时
    @Value("${jwt.expire:7200000}")
    private long expire;

    /**
     * 生成Token
     */
    public String generateToken(String username) {
        // 构建JWT密钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        // 设置Token过期时间
        Date expireDate = new Date(System.currentTimeMillis() + expire);

        // 构建Token负载
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);

        // 生成Token
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * 校验Token有效性
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    /**
     * 密码加密（BCrypt）
     */
    public String encryptPassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }

    /**
     * 密码校验
     */
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
    }
}