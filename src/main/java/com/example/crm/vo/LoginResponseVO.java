package com.example.crm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO：封装前端需要的用户信息+Token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVO {
    private String loginName;  // 登录名
    private String role;       // 角色（admin/sales/finance）
    private Long userId;       // 用户ID
    private String token;      // JWT Token
}