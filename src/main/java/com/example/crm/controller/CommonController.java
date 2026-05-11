package com.example.crm.controller;

import com.example.crm.service.BatchService;
import com.example.crm.service.DocumentService;
import com.example.crm.service.DrugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公共查询接口控制器
 * 支撑前端选择弹窗的分页查询（药品/批次/单据）
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private DrugService drugService;       // 复用现有药品服务
    @Autowired
    private BatchService batchService;     // 需创建批次服务（如果未存在）
    @Autowired
    private DocumentService documentService; // 需创建单据服务（如果未存在）

    /**
     * 分页查询药品列表（适配前端药品选择弹窗）
     * 前端参数：keyword（药品名称/ID）、pageNum、pageSize
     */
    @GetMapping("/drug/page")
    public Map<String, Object> queryDrugPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 复用DrugService的分页逻辑，keyword兼容药品名称/ID查询
            Map<String, Object> data = drugService.getDrugStockList(keyword, null, null, pageNum, pageSize);
            result.put("code", 200);
            result.put("data", data); // data需包含records（列表）、total（总数）
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "药品查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询批次列表（适配前端批次选择弹窗）
     * 前端参数：keyword（批次号/药品名称）、pageNum、pageSize
     */
    @GetMapping("/batch/page")
    public Map<String, Object> queryBatchPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 调用批次服务的分页查询方法（需实现）
            Map<String, Object> data = batchService.getBatchList(keyword, pageNum, pageSize);
            result.put("code", 200);
            result.put("data", data); // data需包含records（列表）、total（总数）
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "批次查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询单据列表（适配前端单据选择弹窗）
     * 前端参数：documentType（单据类型）、keyword（单据ID）、pageNum、pageSize
     */
    @GetMapping("/document/page")
    public Map<String, Object> queryDocumentPage(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 调用单据服务的分页查询方法（需实现）
            Map<String, Object> data = documentService.getDocumentList(documentType, keyword, pageNum, pageSize);
            result.put("code", 200);
            result.put("data", data); // data需包含records（列表）、total（总数）
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "单据查询失败：" + e.getMessage());
        }
        return result;
    }
}