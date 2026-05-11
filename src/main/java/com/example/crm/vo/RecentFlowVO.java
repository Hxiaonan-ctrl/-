package com.example.crm.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data  // 确保有lombok的@Data注解（自动生成getter/setter）
public class RecentFlowVO {
    // 原字段：private Long flowId; → 改为flowNo
    private Long flowNo;
    private String drugName;
    // 原字段：private String enterpriseName; → 改为companyName
    private String companyName;
    private Integer flowQty;
    private BigDecimal unitPrice;
    // 原字段：private BigDecimal totalAmount; → 改为amount
    private BigDecimal amount;
    // 原字段：private String flowTime; → 改为createTime
    private String createTime;
    private String auditStatus;
}