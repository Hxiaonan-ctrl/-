package com.example.crm.service;

import com.example.crm.entity.SysRoleMenu;
import com.example.crm.entity.SysUserRole;
import com.example.crm.mapper.SysRoleMenuMapper;
import com.example.crm.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired; // 改用Autowired
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {

    // 替换 @Resource 为 @Autowired
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    // 给角色分配菜单权限
    public void assignMenu(Long roleId, List<Long> menuIds) {
        // 1. 删除原有关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        // 2. 批量插入新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    }).collect(Collectors.toList());
            sysRoleMenuMapper.batchInsert(list);
        }
    }

    // 给用户分配角色
    public void assignRole(Long userId, List<Long> roleIds) {
        // 1. 删除原有关联
        sysUserRoleMapper.deleteByUserId(userId);
        // 2. 批量插入新关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> list = roleIds.stream()
                    .map(roleId -> {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    }).collect(Collectors.toList());
            sysUserRoleMapper.batchInsert(list);
        }
    }
}