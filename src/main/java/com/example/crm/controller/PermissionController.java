package com.example.crm.controller;

import com.example.crm.common.Result;
import com.example.crm.dto.AssignPermDTO;
import com.example.crm.dto.RoleDTO;
import com.example.crm.entity.SysMenu;
import com.example.crm.entity.SysRole;
import com.example.crm.service.PermissionService;
import com.example.crm.service.SysRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/permission")
@Slf4j
public class PermissionController {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private SysRoleService sysRoleService;

    // ========== 菜单权限查询 ==========
    @GetMapping("/user")
    public Result<List<SysMenu>> getUserPermissions() {
        String loginName = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(permissionService.getMenusByLoginName(loginName));
    }

    // ========== 按钮权限码查询 ==========
    @GetMapping("/user/current")
    public Result<List<String>> getCurrentUserPermCodes() {
        String loginName = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(permissionService.getPermCodesByLoginName(loginName));
    }

    // ========== 角色管理 ==========
    @GetMapping("/role/list")
    public Result<List<SysRole>> getAllRoles() {
        return Result.success(sysRoleService.list());
    }

    @PostMapping("/role/add")
    public Result<Boolean> addRole(@RequestBody RoleDTO dto) {
        boolean success = sysRoleService.addRole(dto);
        return success ? Result.success(true) : Result.error("新增角色失败");
    }

    @DeleteMapping("/role/{id}")
    public Result<Boolean> deleteRole(@PathVariable Long id) {
        return Result.success(sysRoleService.removeById(id));
    }

    // ========== 权限列表查询（修正：返回 SysMenu） ==========
    @GetMapping("/list")
    public Result<List<SysMenu>> getAllPermissions() {
        return Result.success(permissionService.listAllPermissions());
    }

    // ========== 根据角色ID查询权限 ==========
    @GetMapping("/role/{roleId}")
    public Result<List<String>> getPermsByRoleId(@PathVariable Long roleId) {
        return Result.success(permissionService.getPermCodesByUserId(roleId));
    }

    // ========== 权限分配 ==========
    @PostMapping("/role/assign")
    public Result<Void> assignPermsToRole(@RequestBody AssignPermDTO dto) {
        try {
            permissionService.assignPermsToRole(dto);
            return Result.success();
        } catch (Exception e) {
            log.error("权限分配失败", e);
            return Result.error("权限分配失败：" + e.getMessage());
        }
    }
}