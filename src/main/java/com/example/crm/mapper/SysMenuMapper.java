package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {
//    @Select("SELECT DISTINCT m.* " +
//            "FROM sys_user_role ur " +
//            "LEFT JOIN sys_role_menu rm ON ur.role_id = rm.role_id " +
//            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
//            "WHERE ur.user_id = #{userId} " +
//            "ORDER BY m.sort ASC")
//    List<SysMenu> selectMenusByUserId(Long userId);
    /**
     * 根据角色编码查询可访问的菜单列表
     */
    List<SysMenu> selectMenuByRoleCode(@Param("roleCode") String roleCode);
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}