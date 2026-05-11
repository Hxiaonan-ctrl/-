package com.example.crm.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate; // 改为 LocalDate

@Data
public class DrugBatchVO {
    private Long batchId;
    private String batchNo;
    private Long drugId;
    private String drugName;
    private String specification; // 确保有这个字段
    private Integer currentStock;
    private BigDecimal purchasePrice;
    private LocalDate productionDate; // 改为 LocalDate
    private LocalDate expiryDate; // 改为 LocalDate
}