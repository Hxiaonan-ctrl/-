package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.SysRoleMenu;
import com.example.crm.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;


public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    /**
     * 根据角色ID删除关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色菜单关联
     */
    int batchInsert(@Param("list") List<SysRoleMenu> list);

    @Mapper
    public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
        // 无需额外方法，直接使用 MP 原生方法
    }
}