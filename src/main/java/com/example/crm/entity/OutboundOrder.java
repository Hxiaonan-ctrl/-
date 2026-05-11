package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单实体类（对应数据库outbound_order表）
 */
@Data
@TableName("outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long outboundId; // 出库单ID（主键）
    private Long operatorId; // 操作人ID
    private String receiverName; // 接收方全称
    private String receiverContact; // 接收方联系方式
    private LocalDateTime outboundTime; // 出库时间
    private Integer auditStatus; // 审核状态（0-待审核/1-已通过）
    private Long auditorId; // 审核人ID
    private LocalDateTime auditTime; // 审核时间（LocalDateTime类型）
    private String outboundReason; // 出库原因
    private String remarks; // 备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间（LocalDateTime类型）
    private Integer isDeleted; // 逻辑删除
    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private String auditorName;
    @TableField(exist = false)
    private List<OutboundOrderItem> items;
}