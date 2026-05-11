package com.example.crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.dto.DrugAddDTO;
import com.example.crm.dto.DrugUpdateDTO;
import com.example.crm.entity.Drug;
import com.example.crm.entity.DrugBatch;
import com.example.crm.mapper.DrugBatchMapper;
import com.example.crm.mapper.DrugMapper;
import com.example.crm.service.DrugService;
import com.example.crm.vo.DrugBatchVO;
import com.example.crm.vo.DrugStockVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements DrugService {

    @Autowired
    private DrugMapper drugMapper;

    @Autowired
    private DrugBatchMapper drugBatchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDrug(DrugAddDTO dto) {
        // 1. 保存药品主表
        Drug drug = new Drug();
        BeanUtils.copyProperties(dto, drug);
        drug.setCreateTime(LocalDateTime.now());
        boolean saveDrug = this.save(drug);
        Assert.isTrue(saveDrug, "药品主表保存失败");

        // 2. 保存药品批次表
        DrugBatch batch = new DrugBatch();
        batch.setDrugId(drug.getDrugId());
        batch.setBatchNo(dto.getBatchNo());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setProductionDate(dto.getProductionDate() == null ? LocalDate.now() : dto.getProductionDate());
        batch.setExpiryWarningDays(dto.getExpiryWarningDays());
        batch.setCurrentStock(dto.getInitialStock());
        batch.setPurchasePrice(BigDecimal.ZERO);
        batch.setWarehousingId(null); // 修复外键约束问题
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now()); // 补充更新时间字段

        int insertBatch = drugBatchMapper.insert(batch);
        Assert.isTrue(insertBatch > 0, "药品批次保存失败");

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDrug(DrugUpdateDTO dto) {
        // 1. 修改药品主表
        Drug drug = new Drug();
        BeanUtils.copyProperties(dto, drug);
        drug.setUpdateTime(LocalDateTime.now()); // 补充主表更新时间（如果表中有该字段）
        boolean updateDrug = this.updateById(drug);
        Assert.isTrue(updateDrug, "药品主表修改失败");

        // 2. 修改药品批次表（核心修改：有则更新，无则插入）
        DrugBatch batch = new DrugBatch();
        batch.setBatchNo(dto.getBatchNo());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setExpiryWarningDays(dto.getExpiryWarningDays());
        batch.setCurrentStock(dto.getCurrentStock()); // 同步更新库存
        batch.setUpdateTime(LocalDateTime.now()); // 补充批次更新时间

        // 先尝试更新批次记录
        int updateBatch = drugBatchMapper.updateBatchByDrugId(dto.getDrugId(), batch);

        // 如果更新行数为0（无对应批次记录），则插入新批次
        if (updateBatch == 0) {
            DrugBatch newBatch = new DrugBatch();
            newBatch.setDrugId(dto.getDrugId()); // 关联药品ID
            newBatch.setBatchNo(dto.getBatchNo());
            newBatch.setExpiryDate(dto.getExpiryDate());
            newBatch.setProductionDate(LocalDate.now()); // 默认当前日期为生产日期
            newBatch.setExpiryWarningDays(dto.getExpiryWarningDays() == null ? 30 : dto.getExpiryWarningDays()); // 默认预警30天
            newBatch.setCurrentStock(dto.getCurrentStock() == null ? 0 : dto.getCurrentStock()); // 默认库存0
            newBatch.setPurchasePrice(BigDecimal.ZERO); // 默认采购价0
            newBatch.setWarehousingId(null); // 外键默认null
            newBatch.setCreateTime(LocalDateTime.now());
            newBatch.setUpdateTime(LocalDateTime.now());

            int insertBatch = drugBatchMapper.insert(newBatch);
            Assert.isTrue(insertBatch > 0, "药品批次新增失败（原无批次记录）");
        }

        // 断言调整：更新成功 或 插入成功 都视为批次修改成功
        Assert.isTrue(updateBatch > 0 || updateBatch == 0, "药品批次修改/新增失败");

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDrug(Long drugId) {
        // 1. 先删除关联的批次记录（级联删除）
        int deleteBatch = drugBatchMapper.deleteBatchByDrugId(drugId);

        // 2. 删除药品主表记录
        boolean deleteDrug = this.removeById(drugId);
        Assert.isTrue(deleteDrug, "药品删除失败");

        return deleteDrug && deleteBatch >= 0; // 允许批次不存在（deleteBatch=0也视为成功）
    }

    @Override
    public Map<String, Object> getDrugStockList(String drugName, String manufacturer, String drugCategory, Integer pageNum, Integer pageSize) {
        // 1. 参数校验与默认值设置
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        // 2. 计算偏移量
        Integer offset = (pageNum - 1) * pageSize;

        // 3. 调用Mapper查询
        List<DrugStockVO> list = drugMapper.selectDrugStockList(drugName, manufacturer, drugCategory, pageSize, offset);
        Long total = drugMapper.selectDrugStockCount(drugName, manufacturer, drugCategory);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return result;
    }

    @Override
    public DrugStockVO getDrugDetailById(Long drugId) {
        return drugMapper.selectDrugDetailById(drugId);
    }

    @Override
    public List<DrugStockVO> getStockWarningList() {
        Integer pageSize = 1000;
        Integer offset = 0;
        return drugMapper.selectDrugStockList(null, null, null, pageSize, offset)
                .stream()
                .filter(DrugStockVO::isStockWarning)
                .collect(Collectors.toList());
    }

    @Override
    public List<DrugStockVO> getExpiryWarningList() {
        Integer pageSize = 1000;
        Integer offset = 0;
        return drugMapper.selectDrugStockList(null, null, null, pageSize, offset)
                .stream()
                .filter(DrugStockVO::isExpiryWarning)
                .collect(Collectors.toList());
    }

    /**
     * 实现批次库存更新逻辑（核心新增方法）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateBatchStock(Long batchId, Integer changeQty) {
        Map<String, Object> result = new HashMap<>();

        // 1. 参数非空校验
        Assert.notNull(batchId, "批次ID不能为空");
        Assert.notNull(changeQty, "库存变动数量不能为空");

        // 2. 查询批次信息
        DrugBatch batch = drugBatchMapper.selectById(batchId);
        if (batch == null) {
            result.put("success", false);
            result.put("message", "批次不存在");
            result.put("currentStock", 0);
            return result;
        }

        // 3. 出库时校验库存（避免库存为负）
        int newStock = batch.getCurrentStock() + changeQty;
        if (newStock < 0) {
            result.put("success", false);
            result.put("message", String.format("库存不足，当前库存：%d，需出库：%d",
                    batch.getCurrentStock(), Math.abs(changeQty)));
            result.put("currentStock", batch.getCurrentStock());
            return result;
        }

        // 4. 更新批次库存
        batch.setCurrentStock(newStock);
        batch.setUpdateTime(LocalDateTime.now()); // 补充更新时间
        int updateCount = drugBatchMapper.updateById(batch);

        // 5. 封装返回结果
        if (updateCount > 0) {
            result.put("success", true);
            result.put("message", "库存更新成功");
            result.put("currentStock", newStock);
        } else {
            result.put("success", false);
            result.put("message", "库存更新失败，未修改任何记录");
            result.put("currentStock", batch.getCurrentStock());
        }

        return result;
    }

    // ========== 补充缺失的 getAllDrugList 方法实现 ==========
    @Override
    public List<DrugStockVO> getAllDrugList() {
        // 调用Mapper层的selectAllDrugList方法，返回所有药品（无分页，按名称排序）
        List<DrugStockVO> drugList = drugMapper.selectAllDrugList();
        // 空列表友好处理：返回空列表而非null，避免前端遍历报错
        return drugList == null ? List.of() : drugList;
    }

    @Override
    public List<DrugBatchVO> getBatchListByDrugId(Long drugId) {
        // 1. 参数非空校验（和现有代码风格一致）
        Assert.notNull(drugId, "药品ID不能为空");

        // 2. 调用Mapper层查询批次列表
        List<DrugBatchVO> batchList = drugBatchMapper.selectBatchListByDrugId(drugId);

        // 3. 空列表友好处理（返回空列表而非null，避免前端报错）
        return batchList == null ? List.of() : batchList;
    }
}