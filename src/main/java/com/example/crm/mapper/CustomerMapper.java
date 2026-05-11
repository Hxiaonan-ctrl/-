package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户Mapper（MyBatis-Plus基础CRUD）
 */
public interface CustomerMapper extends BaseMapper<Customer> {
}