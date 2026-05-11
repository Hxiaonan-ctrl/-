package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.DrugBatch;
import com.example.crm.vo.DrugBatchVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DrugBatchMapper extends BaseMapper<DrugBatch> {

    int updateBatchByDrugId(@Param("drugId") Long drugId, @Param("batch") DrugBatch batch);

    int deleteBatchByDrugId(@Param("drugId") Long drugId);

    int insertBatch(DrugBatch drugBatch);

    List<DrugBatchVO> selectBatchList(@Param("keyword") String keyword);

    @Select("""
            SELECT 
                batch_id, drug_id, batch_no, production_date,
                expiry_date, expiry_warning_days, current_stock,
                purchase_price, create_time, update_time
            FROM drug_batch
            WHERE drug_id = #{drugId}
            ORDER BY production_date DESC
            """)
    List<DrugBatchVO> selectBatchListByDrugId(@Param("drugId") Long drugId);

    @Update("""
            UPDATE drug_batch
            SET current_stock = current_stock + #{changeQty},
                update_time = NOW()
            WHERE batch_id = #{batchId}
            """)
    int updateBatchStock(@Param("batchId") Long batchId, @Param("changeQty") Integer changeQty);

    @Select("SELECT current_stock FROM drug_batch WHERE batch_id = #{batchId}")
    Integer selectBatchStockById(@Param("batchId") Long batchId);

    // ==========================================
    // 【关键新增】根据批次ID查询批次详情（含药品信息）
    // ==========================================
    @Select("""
            SELECT 
                db.*, d.drug_name, d.specification
            FROM drug_batch db
            LEFT JOIN drug d ON db.drug_id = d.drug_id
            WHERE db.batch_id = #{batchId}
            """)
    DrugBatchVO selectBatchWithDrugById(@Param("batchId") Long batchId);
}