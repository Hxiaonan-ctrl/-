package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("warehousing_order_item")
public class WarehousingOrderItem {
    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long warehousingId;
    private Long batchId;
    private Integer actualInQty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==========================================
    // 以下为非数据库字段，用于展示（使用 LocalDate）
    // ==========================================
    @TableField(exist = false)
    private Long drugId;

    @TableField(exist = false)
    private String batchNo;

    @TableField(exist = false)
    private LocalDate productionDate; // 改为 LocalDate

    @TableField(exist = false)
    private LocalDate expiryDate; // 改为 LocalDate

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String specification;
}