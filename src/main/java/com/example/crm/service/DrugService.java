package com.example.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.dto.DrugAddDTO;
import com.example.crm.dto.DrugUpdateDTO;
import com.example.crm.entity.Drug;
import com.example.crm.vo.DrugBatchVO; // 新增引入批次VO
import com.example.crm.vo.DrugStockVO;

import java.util.List;
import java.util.Map;

public interface DrugService extends IService<Drug> {
    boolean addDrug(DrugAddDTO dto);

    boolean updateDrug(DrugUpdateDTO dto);

    // 新增删除方法
    boolean deleteDrug(Long drugId);

    Map<String, Object> getDrugStockList(String drugName, String manufacturer, String drugCategory, Integer pageNum, Integer pageSize);

    DrugStockVO getDrugDetailById(Long drugId);

    List<DrugStockVO> getStockWarningList();

    List<DrugStockVO> getExpiryWarningList();

    /**
     * 更新药品批次库存
     * @param batchId 批次ID
     * @param changeQty 变动数量（+入库/-出库）
     * @return 包含success、message、currentStock的Map
     */
    Map<String, Object> updateBatchStock(Long batchId, Integer changeQty);

    // ========== 新增：查询所有药品列表（无分页，供前端下拉选择） ==========
    List<DrugStockVO> getAllDrugList();

    // ========== 新增：根据药品ID查询批次列表（核心联动需求） ==========
    List<DrugBatchVO> getBatchListByDrugId(Long drugId);
}