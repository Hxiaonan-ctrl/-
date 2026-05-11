package com.example.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TraceAuditDTO {
    /**
     * 追溯ID
     */
    @NotNull(message = "追溯ID不能为空")
    private Long traceId;

    /**
     * 审核结果（1-通过/2-驳回）
     */
    @NotNull(message = "审核结果不能为空")
    private Integer auditResult;

    /**
     * 审核人ID
     */
    @NotNull(message = "审核人ID不能为空")
    private Long auditorId;

    /**
     * 审核人名称（非必要，用于日志）
     */
    private String auditorName;

    /**
     * 审核备注（驳回时必填）
     */
    private String auditRemark;
}