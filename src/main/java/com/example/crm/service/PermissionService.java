package com.example.crm.service;

import com.example.crm.dto.AssignPermDTO;
import com.example.crm.entity.SysMenu;
import java.util.List;

public interface PermissionService {
    List<String> getPermCodesByUserId(Long userId);
    List<SysMenu> getMenusByLoginName(String loginName);
    List<String> getPermCodesByLoginName(String loginName);
    List<SysMenu> listAllPermissions();
    void assignPermsToRole(AssignPermDTO dto);
    List<SysMenu> getMenusByUserId(Long userId);
}