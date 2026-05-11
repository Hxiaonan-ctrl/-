package com.example.crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.dto.RoleDTO;
import com.example.crm.entity.SysRole;
import com.example.crm.mapper.SysRoleMapper;
import com.example.crm.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public boolean addRole(RoleDTO dto) {
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRemark(dto.getRoleDesc()); // 适配你的RoleDTO：roleDesc → 映射到SysRole的remark字段
        role.setRoleCode(dto.getRoleName().toLowerCase()); // 自动生成角色编码（如 销售 → sales）
        return save(role);
    }
}