package com.example.crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.Customer;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.service.CustomerService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

/**
 * 客户Service实现类
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    @Resource
    private CustomerMapper customerMapper;
}