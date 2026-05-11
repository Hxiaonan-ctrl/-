package com.example.crm.service;

import java.util.Map;

/**
 * 药品批次服务接口
 */
public interface BatchService {

    /**
     * 分页查询批次列表（适配前端选择弹窗）
     * @param keyword 关键词（批次号/药品名称）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 包含records（批次列表）和total（总数）的Map
     */
    Map<String, Object> getBatchList(String keyword, Integer pageNum, Integer pageSize);
}