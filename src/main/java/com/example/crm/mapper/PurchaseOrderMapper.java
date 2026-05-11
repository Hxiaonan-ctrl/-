package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
    IPage<PurchaseOrder> selectPurchasePage(
            Page<PurchaseOrder> page,
            @Param("supplierName") String supplierName,
            @Param("orderStatus") Integer orderStatus,
            @Param("createTimeStart") String createTimeStart,
            @Param("createTimeEnd") String createTimeEnd
    );
}