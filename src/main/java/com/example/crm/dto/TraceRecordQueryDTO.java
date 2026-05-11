package com.example.crm.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TraceRecordQueryDTO {
    // 新增：补充ServiceImpl中调用的缺失字段（解决编译错误）
    private Long drugId;    // 药品ID（精准筛选用）
    private Long batchId;   // 批次ID（精准筛选用）

    // 原有字段（完全保留，不做任何修改）
    private String documentType;
    private String drugName;
    private String batchNo;
    private String operatorName;
    private String flowNode;
    private LocalDateTime operationTimeStart;
    private LocalDateTime operationTimeEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    // 新增：审核状态筛选（0-待审核/1-已通过/2-已驳回）
    private Integer auditStatus;
}