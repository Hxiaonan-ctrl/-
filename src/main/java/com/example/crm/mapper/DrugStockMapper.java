package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.vo.DrugStockVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 药品库存Mapper接口
 * 与resources/mapper/DrugStockMapper.xml一一对应
 */
public interface DrugStockMapper extends BaseMapper<DrugStockVO> {

    /**
     * 分页查询药品库存列表
     * @param limit 每页条数
     * @param offset 偏移量
     * @return 药品库存列表
     */
    List<DrugStockVO> selectDrugStockList(@Param("limit") Integer limit, @Param("offset") Integer offset);

    /**
     * 查询库存预警列表（库存低于预警阈值）
     * @return 库存预警药品列表
     */
    List<DrugStockVO> selectStockWarningList();

    /**
     * 查询效期预警列表（有效期剩余天数低于预警天数）
     * @return 效期预警药品列表
     */
    List<DrugStockVO> selectExpiryWarningList();
}