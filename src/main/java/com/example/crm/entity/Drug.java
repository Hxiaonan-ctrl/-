package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("drug")
public class Drug {
    @TableId(type = IdType.AUTO)
    private Long drugId;                // 药品ID（主键）
    private String drugName;            // 药品通用名称
    private String approvalNo;          // 批准文号
    private String specification;       // 药品规格
    private String manufacturer;        // 生产厂家
    private String drugCategory;        // 药品分类
    private Integer initialStock = 0;   // 初始库存
    private Integer stockWarning;       // 库存预警阈值
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;   // 创建时间
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;   // 更新时间
}