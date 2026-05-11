// 1.2 RoleDTO.java（新增/编辑角色参数）
package com.example.crm.dto;

import lombok.Data;

@Data
public class RoleDTO {
    private String roleName;    // 角色名称（如 sales/finance）
    private String roleDesc;    // 角色描述
}