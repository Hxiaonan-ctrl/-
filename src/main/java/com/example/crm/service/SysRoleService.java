package com.example.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.dto.RoleDTO;
import com.example.crm.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {
    boolean addRole(RoleDTO dto);
}