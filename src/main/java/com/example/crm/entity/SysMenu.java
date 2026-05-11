package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String perms;
    private Integer type;
    private String icon;
    private Integer sort;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
    
    @TableField(exist = false)
    private List<SysMenu> children;
}