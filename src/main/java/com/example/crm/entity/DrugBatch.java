package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("drug_batch")
public class DrugBatch {
    @TableId(type = IdType.AUTO)
    private Long batchId;               // 批次ID（主键）
    private Long drugId;                // 药品ID（外键）
    private String batchNo;             // 批次号
    private LocalDate productionDate;   // 生产日期
    private LocalDate expiryDate;       // 有效期至
    private Integer expiryWarningDays = 30; // 效期预警天数
    private Integer currentStock = 0;   // 当前库存
    private BigDecimal purchasePrice;   // 采购单价
    private Long warehousingId;         // 入库单ID（外键）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;   // 创建时间
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;   // 更新时间
}