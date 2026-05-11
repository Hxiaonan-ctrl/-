package com.example.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    // 根据loginName查询用户
    SysUser getByLoginName(String loginName);

    // 判断loginName是否存在
    boolean existsByLoginName(String loginName);

    // 判断手机号是否存在
    boolean existsByPhone(String phone);

    // 新增：密码加密（适配用户新增/注册）
    String encryptPassword(String password);

    // 新增：根据用户ID查询角色ID（适配用户分配角色功能）
    List<Long> getRoleIdsByUserId(Long userId);
}