package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trace_record")
public class TraceRecord {
    /**
     * 追溯ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long traceId;

    /**
     * 药品ID（外键关联drug表）
     */
    private Long drugId;

    /**
     * 批次ID（外键关联drug_batch表）
     */
    private Long batchId;

    /**
     * 单据类型（采购单/入库单/出库单）
     */
    private String documentType;

    /**
     * 单据ID（关联对应单据表主键）
     */
    private Long documentId;

    /**
     * 操作人ID（外键关联sys_user表）
     */
    private Long operatorId;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 数量变动（正数=入库/负数=出库/采购=0）
     */
    private Integer quantityChange;

    /**
     * 流转节点（采购申请/入库审核/出库发货等）
     */
    private String flowNode;

    // 审核相关字段（数据库存在的字段保留）
    private Integer auditStatus;
    private Long auditorId;
    private LocalDateTime auditTime;
    private String auditRemark;

    // 非数据库字段（联表查询展示用）
    @TableField(exist = false)
    private String drugName;
    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String operatorName;
    @TableField(exist = false)
    private String auditorName;
}