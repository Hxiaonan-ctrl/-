package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    // 只保留方法定义，SQL走XML（删除@Delete注解）
    void deleteByUserId(@Param("userId") Long userId);

    // 批量插入（仅XML有定义，保留方法）
    void batchInsert(@Param("list") List<SysUserRole> list);

    // 只保留方法定义，SQL走XML（删除@Select注解）
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}