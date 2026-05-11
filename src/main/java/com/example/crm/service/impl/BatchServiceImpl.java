package com.example.crm.service.impl;

import com.example.crm.mapper.DrugBatchMapper;
import com.example.crm.service.BatchService;
import com.example.crm.vo.DrugBatchVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药品批次服务实现类
 */
@Service
public class BatchServiceImpl implements BatchService {

    @Autowired
    private DrugBatchMapper drugBatchMapper; // 需创建对应的Mapper

    @Override
    public Map<String, Object> getBatchList(String keyword, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 开启分页（PageHelper，和你现有DrugService分页逻辑一致）
        PageHelper.startPage(pageNum, pageSize);
        
        // 2. 调用Mapper查询（支持批次号/药品名称模糊搜索）
        List<DrugBatchVO> batchList = drugBatchMapper.selectBatchList(keyword);
        
        // 3. 封装分页结果
        PageInfo<DrugBatchVO> pageInfo = new PageInfo<>(batchList);
        result.put("records", batchList); // 列表数据
        result.put("total", pageInfo.getTotal()); // 总条数
        
        return result;
    }
}