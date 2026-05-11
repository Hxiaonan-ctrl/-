package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 核心：泛型必须是 SysPermission，而非 SysMenu
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    // 原有：根据用户ID查询权限码
    @Select("SELECT DISTINCT m.perms " +
            "FROM sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm ON ur.role_id = rm.role_id " +
            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.perms != '' ")
    List<String> selectPermCodesByUserId(Long userId);

    // 原有：根据角色ID查询权限码
    @Select("SELECT m.perms " +
            "FROM sys_role_menu rm " +
            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE rm.role_id = #{roleId} " +
            "AND m.perms != '' ")
    List<String> selectPermCodesByRoleId(Long roleId);

    // 原有：根据角色名称查询权限码
    @Select("SELECT m.perms " +
            "FROM sys_role_menu rm " +
            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
            "LEFT JOIN sys_role r ON rm.role_id = r.id " +
            "WHERE r.role_name = #{roleName} " +
            "AND m.perms != '' ")
    List<String> selectPermCodesByRoleName(String roleName);
}