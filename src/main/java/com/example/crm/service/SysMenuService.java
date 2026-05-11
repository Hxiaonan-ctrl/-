package com.example.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.entity.SysMenu;

import java.util.List;
import java.util.Map;

// 关键：继承 IService<SysMenu>，获得 MyBatis-Plus 内置的 list/remove/save 等方法
public interface SysMenuService extends IService<SysMenu> {
    /**
     * 根据角色编码查询菜单树（适配前端格式）
     * @param roleCode 角色编码（如admin/sales/finance）
     * @return 树形菜单数据（含perms字段）
     */
    List<Map<String, Object>> getMenuTreeByRoleCode(String roleCode);
}