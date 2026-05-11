package com.example.crm.controller;

import com.example.crm.common.Result;
import com.example.crm.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired; // 替换为Autowired
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * 角色权限分配控制器
 * 修复点：1. 替换@Resource为@Autowired；2. 完善参数校验；3. 增加异常处理；4. 类型转换容错
 */
@RestController
@RequestMapping("/api/system/permission")
public class RolePermissionController {

    // 核心修复：替换@Resource为@Autowired，适配Spring Boot 3.x
    @Autowired
    private RolePermissionService rolePermissionService;

    // 给角色分配菜单（完善校验+异常处理）
    @PostMapping("/assignMenu")
    public Result<?> assignMenu(@RequestBody Map<String, Object> params) {
        try {
            // 1. 校验参数是否为空
            if (params == null || params.isEmpty()) {
                return Result.error("请求参数不能为空");
            }

            // 2. 校验并转换角色ID（容错处理）
            Object roleIdObj = params.get("roleId");
            if (roleIdObj == null) {
                return Result.error("角色ID不能为空");
            }
            Long roleId;
            try {
                roleId = Long.valueOf(roleIdObj.toString());
            } catch (NumberFormatException e) {
                return Result.error("角色ID必须为数字");
            }
            if (roleId <= 0) {
                return Result.error("角色ID必须为正整数");
            }

            // 3. 处理菜单ID（允许为空，为空则清空该角色所有菜单权限）
            List<Long> menuIds = (List<Long>) params.get("menuIds");
            // 空列表处理：避免传入null导致后端报错
            menuIds = menuIds == null ? List.of() : menuIds;

            // 4. 调用服务分配权限
            rolePermissionService.assignMenu(roleId, menuIds);
            return Result.success("角色[" + roleId + "]权限分配成功");
        } catch (Exception e) {
            // 捕获所有异常，返回友好提示
            return Result.error("角色权限分配失败：" + e.getMessage());
        }
    }

    // 给用户分配角色（同逻辑完善校验+异常处理）
    @PostMapping("/assignRole")
    public Result<?> assignRole(@RequestBody Map<String, Object> params) {
        try {
            // 1. 校验参数是否为空
            if (params == null || params.isEmpty()) {
                return Result.error("请求参数不能为空");
            }

            // 2. 校验并转换用户ID（容错处理）
            Object userIdObj = params.get("userId");
            if (userIdObj == null) {
                return Result.error("用户ID不能为空");
            }
            Long userId;
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                return Result.error("用户ID必须为数字");
            }
            if (userId <= 0) {
                return Result.error("用户ID必须为正整数");
            }

            // 3. 处理角色ID（允许为空，为空则清空该用户所有角色）
            List<Long> roleIds = (List<Long>) params.get("roleIds");
            // 空列表处理：避免传入null导致后端报错
            roleIds = roleIds == null ? List.of() : roleIds;

            // 4. 调用服务分配角色
            rolePermissionService.assignRole(userId, roleIds);
            return Result.success("用户[" + userId + "]角色分配成功");
        } catch (Exception e) {
            // 捕获所有异常，返回友好提示
            return Result.error("用户角色分配失败：" + e.getMessage());
        }
    }
}