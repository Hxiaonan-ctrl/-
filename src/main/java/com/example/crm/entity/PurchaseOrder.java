package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long purchaseId;

    // 采购订单编号（唯一）
    private String orderNo;

    // 采购专员ID（关联sys_user表）
    private Long purchaserId;

    // 采购专员名称（前端展示）
    @TableField(exist = false)
    private String purchaserName;

    // 供应商全称
    private String supplierName;

    // 供应商联系人
    private String supplierContact;

    // 供应商手机号
    private String supplierPhone;

    /**
     * 采购单状态：0-待审核/1-已通过/2-已到货/3-已完成
     */
    private Integer orderStatus;

    // 审核意见
    private String auditOpinion;

    // 备注
    private String remark;

    // 采购总金额
    private BigDecimal totalAmount;

    // 预计到货日期
    private LocalDate expectedDate;

    // 审核人ID
    private Long auditorId;

    // 【关键】审核人姓名（前端展示）
    @TableField(exist = false)
    private String auditorName;

    // 审核时间
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private List<PurchaseOrderItem> items;
}