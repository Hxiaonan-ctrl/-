package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.crm.dto.AssignPermDTO;
import com.example.crm.entity.SysMenu;
import com.example.crm.entity.SysRole;
import com.example.crm.entity.SysUser;
import com.example.crm.mapper.SysMenuMapper;
import com.example.crm.mapper.SysRoleMapper;
import com.example.crm.mapper.SysUserMapper;
import com.example.crm.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> getPermCodesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || StringUtils.isBlank(user.getRole())) {
            return Collections.emptyList();
        }

        SysRole role = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, user.getRole()));
        if (role == null || StringUtils.isBlank(role.getMenuIds())) {
            return Collections.emptyList();
        }

        List<Long> menuIdList = Arrays.stream(role.getMenuIds().split(","))
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<SysMenu> menuList = sysMenuMapper.selectBatchIds(menuIdList);
        return menuList.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::isNotBlank)
                .flatMap(perms -> Arrays.stream(perms.split(",")))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<SysMenu> getMenusByLoginName(String loginName) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getLoginName, loginName));
        if (user == null) {
            return Collections.emptyList();
        }
        return getMenusByUserId(user.getId());
    }

    @Override
    public List<String> getPermCodesByLoginName(String loginName) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getLoginName, loginName));
        return user == null ? Collections.emptyList() : getPermCodesByUserId(user.getId());
    }

    @Override
    public List<SysMenu> listAllPermissions() {
        return sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getId));
    }

    // ===================== ✅ 核心修复：分配权限时自动补全父菜单ID =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermsToRole(AssignPermDTO dto) {
        Long roleId = dto.getRoleId();
        List<Long> menuIds = dto.getPermIds();

        if (menuIds == null || menuIds.isEmpty()) {
            // 如果没有选择任何菜单，直接清空
            SysRole role = new SysRole();
            role.setId(roleId);
            role.setMenuIds("");
            sysRoleMapper.updateById(role);
            return;
        }

        // 1. 用 Set 去重，避免重复ID
        Set<Long> allMenuIds = new HashSet<>(menuIds);

        // 2. 【核心】遍历所有选中的菜单，递归补全父级菜单
        for (Long menuId : new ArrayList<>(allMenuIds)) {
            addParentMenus(menuId, allMenuIds);
        }

        // 3. 重新拼接成字符串（ID排序，方便阅读）
        String menuIdsStr = allMenuIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 4. 更新角色
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setMenuIds(menuIdsStr);
        sysRoleMapper.updateById(role);
    }

    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || StringUtils.isBlank(user.getRole())) {
            return Collections.emptyList();
        }

        SysRole role = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, user.getRole()));
        if (role == null || StringUtils.isBlank(role.getMenuIds())) {
            return Collections.emptyList();
        }

        List<Long> menuIdList = Arrays.stream(role.getMenuIds().split(","))
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        return sysMenuMapper.selectBatchIds(menuIdList);
    }

    // ===================== ✅ 新增：递归补全父菜单的辅助方法 =====================
    private void addParentMenus(Long menuId, Set<Long> allMenuIds) {
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu != null && menu.getParentId() != null && menu.getParentId() != 0) {
            // 添加父菜单ID
            allMenuIds.add(menu.getParentId());
            // 继续向上递归（直到 parent_id = 0）
            addParentMenus(menu.getParentId(), allMenuIds);
        }
    }
}