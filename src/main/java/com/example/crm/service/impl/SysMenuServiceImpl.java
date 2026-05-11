package com.example.crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.SysMenu;
import com.example.crm.entity.SysRole;
import com.example.crm.mapper.SysMenuMapper;
import com.example.crm.mapper.SysRoleMapper;
import com.example.crm.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    // 【新增】注入角色Mapper
    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public List<Map<String, Object>> getMenuTreeByRoleCode(String roleCode) {
        // 1. 【核心修改】查询角色，获取分配的菜单ID
        SysRole role = sysRoleMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(SysRole.class)
                        .eq(SysRole::getRoleCode, roleCode)
                        .eq(SysRole::getIsDeleted, 0)
        );

        // 无角色/无分配菜单，直接返回空
        if (role == null || !StringUtils.hasText(role.getMenuIds())) {
            return new ArrayList<>();
        }

        // 2. 解析菜单ID
        List<Long> menuIds = Arrays.stream(role.getMenuIds().split(","))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 3. 【核心修改】只查询分配的菜单，不返回多余菜单
        String sql = "SELECT id, parent_id, menu_name, path, icon, perms " +
                "FROM sys_menu " +
                "WHERE is_deleted = 0 AND id IN (" +
                menuIds.stream().map(String::valueOf).collect(Collectors.joining(",")) +
                ") ORDER BY sort ASC";
        List<Map<String, Object>> menuList = jdbcTemplate.queryForList(sql);

        // 4. 构建树形结构
        List<Map<String, Object>> treeMenu = new ArrayList<>();
        for (Map<String, Object> menu : menuList) {
            Object parentIdObj = menu.get("parent_id");
            long parentId = (parentIdObj instanceof Number) ? ((Number) parentIdObj).longValue() : 0L;

            if (parentId == 0L) {
                Map<String, Object> topMenu = convertMenu(menu);
                topMenu.put("children", findChildren(menu.get("id").toString(), menuList));
                treeMenu.add(topMenu);
            }
        }
        return treeMenu;
    }

    // 转换数据库字段为前端格式
    private Map<String, Object> convertMenu(Map<String, Object> dbMenu) {
        Map<String, Object> frontMenu = new HashMap<>();
        frontMenu.put("index", dbMenu.get("id").toString());
        frontMenu.put("title", dbMenu.get("menu_name"));
        frontMenu.put("icon", dbMenu.get("icon"));
        frontMenu.put("path", dbMenu.get("path"));
        frontMenu.put("perms", dbMenu.get("perms") == null ? "" : dbMenu.get("perms"));
        return frontMenu;
    }

    // 递归查找子菜单
    private List<Map<String, Object>> findChildren(String parentId, List<Map<String, Object>> allMenus) {
        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> menu : allMenus) {
            Object menuParentIdObj = menu.get("parent_id");
            long menuParentId = (menuParentIdObj instanceof Number) ? ((Number) menuParentIdObj).longValue() : 0L;

            if (parentId.equals(String.valueOf(menuParentId))) {
                Map<String, Object> childMenu = convertMenu(menu);
                childMenu.put("children", findChildren(menu.get("id").toString(), allMenus));
                children.add(childMenu);
            }
        }
        return children;
    }
}