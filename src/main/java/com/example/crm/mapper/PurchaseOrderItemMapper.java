package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
    @Select("SELECT * FROM purchase_order_item WHERE purchase_id = #{purchaseId}")
    List<PurchaseOrderItem> selectByPurchaseId(@Param("purchaseId") Long purchaseId);
}