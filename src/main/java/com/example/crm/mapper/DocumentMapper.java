package com.example.crm.mapper;

import com.example.crm.vo.DocumentVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface DocumentMapper {
    /**
     * 分页查询单据列表
     * @param documentType 单据类型
     * @param keyword 单据ID
     * @return 单据列表
     */
    List<DocumentVO> selectDocumentList(
            @Param("documentType") String documentType,
            @Param("keyword") String keyword
    );
}