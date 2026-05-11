package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("warehousing_order")
public class WarehousingOrder {
    @TableId(type = IdType.AUTO)
    private Long warehousingId;

    private Long purchaseId;
    private Long operatorId;
    private LocalDateTime warehousingTime;
    private Integer auditStatus;
    private Long auditorId;
    private LocalDateTime auditTime;
    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    // 入库单明细列表
    @TableField(exist = false)
    private List<WarehousingOrderItem> items;

    // ==========================================
    // 【新增】列表页展示的关键字段（非数据库字段）
    // ==========================================
    @TableField(exist = false)
    private Integer totalDrugCount; // 药品品种数

    @TableField(exist = false)
    private Integer totalInQty; // 总入库数量

    @TableField(exist = false)
    private String drugSummary; // 药品明细摘要

    @TableField(exist = false)
    private String operatorName; // 操作人名称

    @TableField(exist = false)
    private String auditorName; // 审核人名称
}