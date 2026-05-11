package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long itemId;

    // 采购单ID（关联purchase_order表）
    private Long purchaseId;

    // 药品ID（关联drug表）
    private Long drugId;

    // 【新增】批次号（数据库字段：batch_no）
    private String batchNo;

    // 【新增】药品名称（非数据库字段，用于前端展示）
    @TableField(exist = false)
    private String drugName;

    // 【新增】药品规格（非数据库字段，用于前端展示）
    @TableField(exist = false)
    private String specification;

    // 采购数量（对应前端请求体的quantity，数据库字段：purchase_qty）
    private Integer purchaseQty;

    // 采购单价
    private BigDecimal unitPrice;

    // 明细金额
    private BigDecimal totalAmount;

    // 预计到货时间
    private LocalDate expectedArrival;

    // 创建时间（自动填充，建议加上填充注解）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 【兼容前端】非数据库字段，接收前端的quantity参数（可选，用于参数映射）
    @TableField(exist = false)
    private Integer quantity;
}