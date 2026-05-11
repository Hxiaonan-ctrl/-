package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.entity.WarehousingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 入库单Mapper
 */
public interface WarehousingOrderMapper extends BaseMapper<WarehousingOrder> {

    /** 分页查询入库单（多条件筛选） */
    IPage<WarehousingOrder> selectWarehousingPage(
            Page<WarehousingOrder> page,
            @Param("purchaseNo") String purchaseNo,
            @Param("auditStatus") Integer auditStatus,
            @Param("warehousingTimeStart") String warehousingTimeStart,
            @Param("warehousingTimeEnd") String warehousingTimeEnd
    );
    WarehousingOrder getWarehousingDetail(@Param("warehousingId") Long warehousingId);
}