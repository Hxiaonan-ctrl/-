package com.example.crm.controller;

import com.example.crm.common.Result;
import com.example.crm.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getMenuTree(@RequestParam String roleCode) {
        try {
            List<Map<String, Object>> menuTree = sysMenuService.getMenuTreeByRoleCode(roleCode);
            return Result.success("查询成功", menuTree);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询菜单失败：" + e.getMessage());
        }
    }
}