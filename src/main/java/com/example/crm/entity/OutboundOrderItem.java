package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("outbound_order_item")
public class OutboundOrderItem {
    @TableId(type = IdType.AUTO)
    private Long itemId;
    private Long outboundId;
    private Long batchId;
    private Integer actualOutQty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 前端展示专用字段（非数据库）
    @TableField(exist = false)
    private String drugName;
    @TableField(exist = false)
    private String specification;
    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private LocalDate productionDate;
    @TableField(exist = false)
    private LocalDate expiryDate;
}