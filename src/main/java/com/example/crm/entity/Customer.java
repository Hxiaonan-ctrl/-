package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerName;

    private Integer type;

    private String contactPerson;

    private String phone;

    private String address;

    // 核心修复：新增时自动填充创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 核心修复：新增/更新时自动填充更新时间
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}