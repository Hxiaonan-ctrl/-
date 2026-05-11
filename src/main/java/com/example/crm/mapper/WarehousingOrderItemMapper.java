package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.WarehousingOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 入库单明细表Mapper
 */
public interface WarehousingOrderItemMapper extends BaseMapper<WarehousingOrderItem> {

    /** 根据入库单ID查询明细 */
    @Select("SELECT * FROM warehousing_order_item WHERE warehousing_id = #{warehousingId}")
    List<WarehousingOrderItem> selectByWarehousingId(@Param("warehousingId") Long warehousingId);
}