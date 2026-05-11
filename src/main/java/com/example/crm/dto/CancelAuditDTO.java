package com.example.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CancelAuditDTO {

    @NotNull(message = "追溯ID不能为空")
    private Long traceId;

    // 操作人（当前登录人）
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    // 取消原因（可选）
    private String remark;
}