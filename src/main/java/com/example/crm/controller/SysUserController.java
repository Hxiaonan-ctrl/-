package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.common.Result;
import com.example.crm.entity.SysRole;
import com.example.crm.entity.SysUser;
import com.example.crm.service.SysUserService;
import com.example.crm.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 1. 查询用户列表（支持用户名/手机号模糊搜索）
     */
    @GetMapping("/list")
    public Result<List<SysUser>> list(
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) String phone
    ) {
        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(loginName)) {
                wrapper.like(SysUser::getLoginName, loginName.trim());
            }
            if (StringUtils.hasText(phone)) {
                wrapper.like(SysUser::getPhone, phone.trim());
            }
            // ❌ 删除这一行：wrapper.eq(SysUser::getIsDeleted, 0);
            // MyBatis-Plus的@TableLogic会自动添加 is_deleted=0 条件
            wrapper.orderByAsc(SysUser::getId);

            List<SysUser> userList = sysUserService.list(wrapper);

            // 关联角色名称
            List<SysRole> roleList = sysRoleService.list();
            Map<String, String> roleNameMap = roleList.stream()
                    .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getRoleName));
            userList.forEach(user -> {
                if (StringUtils.hasText(user.getRole())) {
                    user.setRoleName(roleNameMap.get(user.getRole()));
                }
            });

            return Result.success("查询成功", userList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 2. 新增用户
     */
    @PostMapping
    public Result<?> add(@RequestBody SysUser user) {
        try {
            // 校验用户名是否重复
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getLoginName, user.getLoginName());
            if (sysUserService.count(wrapper) > 0) {
                return Result.error("用户名已存在");
            }

            // 密码加密
            if (StringUtils.hasText(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            sysUserService.save(user);
            return Result.success("新增用户成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增用户失败：" + e.getMessage());
        }
    }

    /**
     * 3. 编辑用户（不修改密码）
     */
    @PutMapping
    public Result<?> update(@RequestBody SysUser user) {
        try {
            user.setPassword(null); // 编辑时不修改密码
            sysUserService.updateById(user);
            return Result.success("编辑用户成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("编辑用户失败：" + e.getMessage());
        }
    }

    /**
     * 4. 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // MyBatis-Plus的removeById会自动设置is_deleted=1（因为加了@TableLogic注解）
            sysUserService.removeById(id);
            return Result.success("删除用户成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除用户失败：" + e.getMessage());
        }
    }

    /**
     * 5. 分配角色给用户（适配：使用 roleCode）
     */
    @PutMapping("/assignRole")
    public Result<?> assignRole(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("id").toString());
            String roleCode = params.get("roleCode").toString();

            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 校验角色编码是否有效
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getRoleCode, roleCode);
            if (sysRoleService.count(roleWrapper) == 0) {
                return Result.error("角色不存在");
            }

            user.setRole(roleCode);
            sysUserService.updateById(user);
            return Result.success("角色分配成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("角色分配失败：" + e.getMessage());
        }
    }

    /**
     * 6. 根据用户ID查询用户信息
     */
    @GetMapping("/{userId}")
    public Result<SysUser> getUserInfo(@PathVariable Long userId) {
        try {
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success("用户信息查询成功", user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用户信息失败：" + e.getMessage());
        }
    }
}