package com.example.crm.dto;

import lombok.Data;

/**
 * 注册请求参数DTO（使用Lombok @Data）
 */
@Data
public class UserRegisterDTO {
    // 核心修改：将username改为loginName，匹配AuthController的调用
    private String loginName;
    private String password;
    private String phone;
}