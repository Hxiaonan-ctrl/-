package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 出库单明细表Mapper
 */

public interface OutboundOrderItemMapper extends BaseMapper<OutboundOrderItem> {

    /** 根据出库单ID查询明细 */
    @Select("SELECT * FROM outbound_order_item WHERE outbound_id = #{outboundId}")
    List<OutboundOrderItem> selectByOutboundId(@Param("outboundId") Long outboundId);
}