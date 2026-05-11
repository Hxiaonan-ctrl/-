package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.SysUser;
import com.example.crm.mapper.SysUserMapper;
import com.example.crm.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

@Service
// ✅ 必须继承 ServiceImpl<Mapper, Entity>，才能实现 IService 的方法
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public SysUser getByLoginName(String loginName) {
        if (!StringUtils.hasText(loginName)) return null;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getLoginName, loginName.trim());
        return this.getOne(wrapper);
    }

    @Override
    public boolean existsByLoginName(String loginName) {
        if (!StringUtils.hasText(loginName)) return false;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getLoginName, loginName.trim());
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) return false;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone.trim());
        return this.count(wrapper) > 0;
    }

    @Override
    public String encryptPassword(String password) {
        return StringUtils.hasText(password) ? passwordEncoder.encode(password) : null;
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return new ArrayList<>();
    }
}