package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.common.Result;
import com.example.crm.entity.SysRole;
import com.example.crm.entity.SysMenu;
import com.example.crm.service.SysRoleService;
import com.example.crm.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 1. 查询所有角色（过滤已删除）
     */
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        try {
            List<SysRole> roleList = sysRoleService.list(
                    new LambdaQueryWrapper<SysRole>()
                            .orderByAsc(SysRole::getId)
            );
            return Result.success("查询成功", roleList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 2. 新增角色
     */
    @PostMapping
    public Result<?> add(@RequestBody SysRole role) {
        try {
            if (!StringUtils.hasText(role.getRoleName())) {
                return Result.error("角色名称不能为空");
            }
            if (!StringUtils.hasText(role.getRoleCode())) {
                return Result.error("角色编码不能为空");
            }

            long count = sysRoleService.count(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleCode, role.getRoleCode().trim())
            );
            if (count > 0) {
                return Result.error("角色编码已存在，请更换");
            }

            sysRoleService.save(role);
            return Result.success("新增角色成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增角色失败：" + e.getMessage());
        }
    }

    /**
     * 3. 修改角色
     */
    @PutMapping
    public Result<?> update(@RequestBody SysRole role) {
        try {
            if (role.getId() == null) {
                return Result.error("角色ID不能为空");
            }
            if (!StringUtils.hasText(role.getRoleName())) {
                return Result.error("角色名称不能为空");
            }
            if (!StringUtils.hasText(role.getRoleCode())) {
                return Result.error("角色编码不能为空");
            }

            long count = sysRoleService.count(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleCode, role.getRoleCode().trim())
                            .ne(SysRole::getId, role.getId())
            );
            if (count > 0) {
                return Result.error("角色编码已存在，请更换");
            }

            sysRoleService.updateById(role);
            return Result.success("修改角色成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改角色失败：" + e.getMessage());
        }
    }

    /**
     * 4. 删除角色（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            SysRole role = sysRoleService.getById(id);
            if (role == null) {
                return Result.error("角色不存在");
            }

            sysRoleService.removeById(id);
            return Result.success("删除角色成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除角色失败：" + e.getMessage());
        }
    }

    /**
     * 5. 查询菜单树（用于分配权限）
     */
    @GetMapping("/menu/tree")
    public Result<List<SysMenu>> getMenuTree() {
        try {
            List<SysMenu> menuList = sysMenuService.list(
                    new LambdaQueryWrapper<SysMenu>()
                            .eq(SysMenu::getIsDeleted, 0)
                            .orderByAsc(SysMenu::getSort)
            );
            // 构建树形结构（简单实现：假设parent_id=0为顶级菜单）
            List<SysMenu> tree = buildMenuTree(menuList, 0L);
            return Result.success("查询成功", tree);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询菜单树失败：" + e.getMessage());
        }
    }

    /**
     * 6. 根据角色ID查询已分配的菜单（用于前端菜单树回显）
     */
    @GetMapping("/menu/role/{roleId}")
    public Result<List<SysMenu>> getRoleMenus(@PathVariable Long roleId) {
        try {
            // 1. 查询角色
            SysRole role = sysRoleService.getById(roleId);
            if (role == null) {
                return Result.error("角色不存在");
            }

            // 2. 获取角色的菜单ID字符串
            String menuIdsStr = role.getMenuIds();
            if (!StringUtils.hasText(menuIdsStr)) {
                return Result.success("查询成功", List.of());
            }

            // 3. 字符串转ID集合
            List<Long> menuIdList = Arrays.stream(menuIdsStr.split(","))
                    .map(Long::valueOf)
                    .toList();

            // 4. 查询菜单列表并返回树形结构
            List<SysMenu> menuList = sysMenuService.listByIds(menuIdList);
            return Result.success("查询成功", menuList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询角色菜单失败：" + e.getMessage());
        }
    }

    /**
     * 7. 分配菜单权限给角色（✅ 核心修复：含自动补全父菜单ID逻辑）
     * 前端传参：{ "roleId": 1, "menuIds": [1,2,3,4] }
     */
    @PostMapping("/menu/assign")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignMenu(@RequestBody Map<String, Object> params) {
        try {
            // 1. 参数校验与解析
            if (params.get("roleId") == null || params.get("menuIds") == null) {
                return Result.error("角色ID和菜单ID不能为空");
            }
            Long roleId = Long.valueOf(params.get("roleId").toString());
            List<Long> menuIds = ((List<?>) params.get("menuIds")).stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .distinct() // 菜单ID去重
                    .toList();

            // 2. 校验角色是否存在
            SysRole role = sysRoleService.getById(roleId);
            if (role == null) {
                return Result.error("角色不存在");
            }

            // ===================== ✅ 核心新增：自动补全父菜单ID =====================
            Set<Long> allMenuIds = new HashSet<>(menuIds);
            // 遍历所有选中的菜单，递归补全父级
            for (Long menuId : new ArrayList<>(allMenuIds)) {
                addParentMenus(menuId, allMenuIds);
            }
            // =========================================================================

            // 3. 菜单ID列表 转为 逗号分隔字符串（排序）
            String menuIdStr = allMenuIds.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 4. 更新角色的 menu_ids 字段
            role.setMenuIds(menuIdStr);
            sysRoleService.updateById(role);

            return Result.success("权限分配成功");
        } catch (NumberFormatException e) {
            return Result.error("参数格式错误，ID必须为数字");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("权限分配失败：" + e.getMessage());
        }
    }

    // ===================== ✅ 新增：递归补全父菜单的辅助方法 =====================
    private void addParentMenus(Long menuId, Set<Long> allMenuIds) {
        SysMenu menu = sysMenuService.getById(menuId);
        if (menu != null && menu.getParentId() != null && menu.getParentId() != 0) {
            // 添加父菜单ID
            allMenuIds.add(menu.getParentId());
            // 继续向上递归（直到 parent_id = 0）
            addParentMenus(menu.getParentId(), allMenuIds);
        }
    }

    // 辅助：构建菜单树形结构
    private List<SysMenu> buildMenuTree(List<SysMenu> menuList, Long parentId) {
        return menuList.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .peek(menu -> menu.setChildren(buildMenuTree(menuList, menu.getId())))
                .collect(Collectors.toList());
    }
}