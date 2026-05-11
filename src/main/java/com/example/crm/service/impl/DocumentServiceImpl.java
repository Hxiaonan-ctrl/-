package com.example.crm.service.impl;

import com.example.crm.mapper.DocumentMapper;
import com.example.crm.service.DocumentService;
import com.example.crm.vo.DocumentVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据服务实现类
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper; // 需创建对应的Mapper

    @Override
    public Map<String, Object> getDocumentList(String documentType, String keyword, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 开启分页
        PageHelper.startPage(pageNum, pageSize);
        
        // 2. 调用Mapper查询（支持单据类型+单据ID搜索）
        List<DocumentVO> documentList = documentMapper.selectDocumentList(documentType, keyword);
        
        // 3. 封装分页结果
        PageInfo<DocumentVO> pageInfo = new PageInfo<>(documentList);
        result.put("records", documentList); // 列表数据
        result.put("total", pageInfo.getTotal()); // 总条数
        
        return result;
    }
}