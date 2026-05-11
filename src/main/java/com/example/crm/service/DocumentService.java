package com.example.crm.service;

import java.util.Map;

/**
 * 单据服务接口
 */
public interface DocumentService {

    /**
     * 分页查询单据列表（适配前端选择弹窗）
     * @param documentType 单据类型（采购单/入库单/出库单）
     * @param keyword 关键词（单据ID）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 包含records（单据列表）和total（总数）的Map
     */
    Map<String, Object> getDocumentList(String documentType, String keyword, Integer pageNum, Integer pageSize);
}