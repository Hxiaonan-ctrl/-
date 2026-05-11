package com.example.crm.controller;

import com.example.crm.dto.DrugAddDTO;
import com.example.crm.dto.DrugUpdateDTO;
import com.example.crm.service.DrugService;
import com.example.crm.vo.DrugBatchVO;
import com.example.crm.vo.DrugStockVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药品管理控制器
 */
@RestController
@RequestMapping("/api/drug")
public class DrugController {

    // 新增日志对象，统一异常日志打印
    private static final Logger log = LoggerFactory.getLogger(DrugController.class);

    @Autowired
    private DrugService drugService;

    /**
     * 新增药品
     */
    @PostMapping("/add")
    public Map<String, Object> addDrug(@Validated @RequestBody DrugAddDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = drugService.addDrug(dto);
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "新增药品成功" : "新增药品失败");
        } catch (Exception e) {
            log.error("新增药品异常：", e);
            result.put("code", 500);
            result.put("message", "新增失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 编辑药品
     */
    @PutMapping("/update")
    public Map<String, Object> updateDrug(@Validated @RequestBody DrugUpdateDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = drugService.updateDrug(dto);
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "编辑药品成功" : "编辑药品失败");
        } catch (Exception e) {
            log.error("编辑药品异常：", e);
            result.put("code", 500);
            result.put("message", "编辑失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 删除药品（级联删除批次）
     */
    @DeleteMapping("/delete/{drugId}")
    public Map<String, Object> deleteDrug(@PathVariable Long drugId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = drugService.deleteDrug(drugId);
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "删除药品成功" : "删除药品失败");
        } catch (Exception e) {
            log.error("删除药品异常：", e);
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 查询药品库存列表
     */
    @GetMapping("/stock/list")
    public Map<String, Object> getDrugStockList(
            @RequestParam(required = false) String drugName,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String drugCategory,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = drugService.getDrugStockList(drugName, manufacturer, drugCategory, pageNum, pageSize);
            result.put("code", 200);
            result.put("data", data);
            result.put("message", "查询成功");
        } catch (Exception e) {
            log.error("查询药品库存列表异常：", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询药品详情
     */
    @GetMapping("/detail/{drugId}")
    public Map<String, Object> getDrugDetail(@PathVariable Long drugId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DrugStockVO detail = drugService.getDrugDetailById(drugId);
            result.put("code", 200);
            result.put("data", detail);
            result.put("message", "查询成功");
        } catch (Exception e) {
            log.error("查询药品详情异常：", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 查询库存预警列表
     */
    @GetMapping("/stock/warning")
    public Map<String, Object> getStockWarningList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DrugStockVO> list = drugService.getStockWarningList();
            result.put("code", 200);
            result.put("data", list);
            result.put("message", "查询成功");
        } catch (Exception e) {
            log.error("查询库存预警列表异常：", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 查询效期预警列表
     */
    @GetMapping("/expiry/warning")
    public Map<String, Object> getExpiryWarningList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DrugStockVO> list = drugService.getExpiryWarningList();
            result.put("code", 200);
            result.put("data", list);
            result.put("message", "查询成功");
        } catch (Exception e) {
            log.error("查询效期预警列表异常：", e);
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 更新药品批次库存（新增接口）
     * @param params 包含batchId（批次ID）、changeQty（变动数量，+入库/-出库）
     * @return 操作结果
     */
    @PostMapping("/batch/updateStock")
    public Map<String, Object> updateBatchStock(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 参数解析与非空校验
            if (!params.containsKey("batchId") || params.get("batchId") == null) {
                result.put("code", 500);
                result.put("message", "批次ID不能为空");
                return result;
            }
            if (!params.containsKey("changeQty") || params.get("changeQty") == null) {
                result.put("code", 500);
                result.put("message", "库存变动数量不能为空");
                return result;
            }

            // 2. 类型转换（兼容前端传的字符串/数字）
            Long batchId;
            Integer changeQty;
            try {
                batchId = Long.valueOf(params.get("batchId").toString());
                changeQty = Integer.valueOf(params.get("changeQty").toString());
            } catch (NumberFormatException e) {
                result.put("code", 500);
                result.put("message", "参数格式错误：批次ID和变动数量必须为数字");
                return result;
            }

            // 3. 调用Service更新库存
            Map<String, Object> stockResult = drugService.updateBatchStock(batchId, changeQty);
            boolean success = (boolean) stockResult.get("success");

            result.put("code", success ? 200 : 500);
            result.put("message", (String) stockResult.get("message"));
            // 返回更新后的库存（可选）
            if (success && stockResult.containsKey("currentStock")) {
                result.put("data", Map.of("currentStock", stockResult.get("currentStock")));
            }
        } catch (Exception e) {
            log.error("更新药品批次库存异常：", e);
            result.put("code", 500);
            result.put("message", "库存更新失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 查询所有药品列表（供前端下拉选择）
     */
    @GetMapping("/list")
    public Map<String, Object> getDrugList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DrugStockVO> drugList = drugService.getAllDrugList();
            result.put("code", 200);
            result.put("data", drugList);
            result.put("message", "查询药品列表成功");
        } catch (Exception e) {
            log.error("查询药品列表异常：", e);
            result.put("code", 500);
            result.put("message", "查询药品失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 根据药品ID查询批次列表
     * @param drugId 药品ID
     * @return 该药品的所有批次信息
     */
    @GetMapping("/batch/listByDrugId")
    public Map<String, Object> getBatchListByDrugId(@RequestParam Long drugId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DrugBatchVO> batchList = drugService.getBatchListByDrugId(drugId);
            result.put("code", 200);
            result.put("data", batchList);
            result.put("message", "查询批次列表成功");
        } catch (Exception e) {
            log.error("根据药品ID查询批次列表异常：", e);
            result.put("code", 500);
            result.put("message", "查询批次失败：" + e.getMessage());
        }
        return result;
    }
}