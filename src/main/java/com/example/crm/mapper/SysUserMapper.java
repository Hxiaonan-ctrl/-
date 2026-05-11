package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {
    // MyBatis-Plus封装基础CRUD，无需额外编写
}