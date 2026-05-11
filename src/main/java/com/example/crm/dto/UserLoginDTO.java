package com.example.crm.dto;

import lombok.Data;

/**
 * 登录请求参数DTO（使用Lombok @Data）
 */
@Data
public class UserLoginDTO {
    // 用户名
    private String loginName;
    // 密码
    private String password;
}