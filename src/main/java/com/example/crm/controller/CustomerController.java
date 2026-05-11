package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.common.Result;
import com.example.crm.entity.Customer;
import com.example.crm.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    /**
     * 分页+多条件查询客户列表（仅admin/sales角色可访问）
     * 支持按：药企名称（模糊）、联系人（模糊）、联系电话（模糊）、客户类型（精确）查询
     */
    @PreAuthorize("hasAnyRole('admin', 'sales')")
    @GetMapping("/list")
    public Page<Customer> getCustomerList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerName, // 药企名称（模糊）
            @RequestParam(required = false) String contactPerson, // 联系人（模糊）
            @RequestParam(required = false) String phone, // 联系电话（模糊）
            @RequestParam(required = false) Integer type // 客户类型（1-药企，2-药店，精确）
    ) {
        // 1. 构建分页对象
        Page<Customer> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件（LambdaQueryWrapper，避免硬编码字段名）
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        // 药企名称：不为空时模糊查询
        queryWrapper.like(StringUtils.hasText(customerName), Customer::getCustomerName, customerName);
        // 联系人：不为空时模糊查询
        queryWrapper.like(StringUtils.hasText(contactPerson), Customer::getContactPerson, contactPerson);
        // 联系电话：不为空时模糊查询
        queryWrapper.like(StringUtils.hasText(phone), Customer::getPhone, phone);
        // 客户类型：不为空时精确查询
        queryWrapper.eq(type != null, Customer::getType, type);
        // 按创建时间倒序排列（最新的在前面）
        queryWrapper.orderByDesc(Customer::getCreateTime);

        // 3. 执行分页+条件查询
        return customerService.page(page, queryWrapper);
    }

    /**
     * 新增客户（仅admin角色可访问）
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/add")
    public Boolean addCustomer(@RequestBody Customer customer) {
        return customerService.save(customer);
    }

    /**
     * 修改客户（仅admin角色可访问）
     * 注意：必须传id字段，用于定位要修改的记录
     */
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/update")
    public Boolean updateCustomer(@RequestBody Customer customer) {
        return customerService.updateById(customer);
    }

    /**
     * 删除客户（仅admin角色可访问）
     * 逻辑删除：不会真的删除数据库记录，只是将is_deleted设为1
     */
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/delete/{id}")
    public Boolean deleteCustomer(@PathVariable Long id) {
        return customerService.removeById(id);
    }
    /**
     * 获取所有未删除的客户（用于下拉选择，无分页）
     */
    @GetMapping("/all")
    public Result<List<Customer>> getAllCustomer() {
        List<Customer> list = customerService.lambdaQuery()
                .eq(Customer::getIsDeleted, 0)
                .list();
        return Result.success(list);
    }
}