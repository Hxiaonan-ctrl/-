package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对应数据库 login_name 字段
    @TableField("login_name")
    private String loginName;

    // 对应数据库 username 字段（注意：你的表中是 username，不是 loginName）
    @TableField("username")
    private String username;

    // 对应数据库 password 字段
    @TableField("password")
    private String password;

    // 对应数据库 phone 字段
    @TableField("phone")
    private String phone;

    // 对应数据库 role 字段（默认值 'sales'）
    @TableField("role")
    private String role;

    // 对应数据库 create_time 字段（自动填充）
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 对应数据库 update_time 字段（自动填充）
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 注意：数据库表中没有 role_id、remark、roleName 字段，
    // 如需使用，需先在数据库中添加，或标记为 exist=false
    @TableField(exist = false)
    private String roleName; // 仅用于前端显示，不映射到数据库

    // 新增：逻辑删除字段
    @TableLogic // MyBatis-Plus逻辑删除注解
    private Integer isDeleted;
}