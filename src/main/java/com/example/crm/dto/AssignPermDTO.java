// 1.1 AssignPermDTO.java（接收权限分配参数）
package com.example.crm.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignPermDTO {
    private Long roleId;        // 角色ID
    private List<Long> permIds; // 权限ID列表
}

