package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Drug;
import com.example.crm.vo.DrugStockVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 药品Mapper接口
 */
public interface DrugMapper extends BaseMapper<Drug> {
    List<DrugStockVO> selectDrugStockList(
            @Param("drugName") String drugName,
            @Param("manufacturer") String manufacturer,
            @Param("drugCategory") String drugCategory,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset // 提前计算好的偏移量
    );

    Long selectDrugStockCount(
            @Param("drugName") String drugName,
            @Param("manufacturer") String manufacturer,
            @Param("drugCategory") String drugCategory
    );

    DrugStockVO selectDrugDetailById(@Param("drugId") Long drugId);

    // 新增：查询药品总数量（首页统计用）
    @Select("SELECT COUNT(*) FROM drug")
    Integer selectTotalDrugNum();

    // ========== 新增：查询所有药品列表（无分页，供前端下拉选择） ==========
    @Select("""
            SELECT 
                drug_id, drug_name, approval_no, specification, 
                manufacturer, drug_category, initial_stock, stock_warning,
                create_time, update_time
            FROM drug
            ORDER BY drug_name ASC
            """)
    List<DrugStockVO> selectAllDrugList();
}