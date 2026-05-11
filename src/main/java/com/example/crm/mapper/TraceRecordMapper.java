package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.TraceRecord;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TraceRecordMapper extends BaseMapper<TraceRecord> {
    List<TraceRecord> selectTraceRecordList(
            @Param("drugId") Long drugId,
            @Param("batchId") Long batchId,
            @Param("documentType") String documentType,
            @Param("drugName") String drugName,
            @Param("batchNo") String batchNo, // 保留原有batchNo参数
            @Param("operatorName") String operatorName,
            @Param("flowNode") String flowNode,
            @Param("operationTimeStart") LocalDateTime operationTimeStart,
            @Param("operationTimeEnd") LocalDateTime operationTimeEnd,
            @Param("auditStatus") Integer auditStatus, // 新增：审核状态筛选参数
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    Long selectTraceRecordCount(
            @Param("drugId") Long drugId,
            @Param("batchId") Long batchId,
            @Param("documentType") String documentType,
            @Param("drugName") String drugName,
            @Param("batchNo") String batchNo, // 保留原有batchNo参数
            @Param("operatorName") String operatorName,
            @Param("flowNode") String flowNode,
            @Param("operationTimeStart") LocalDateTime operationTimeStart,
            @Param("operationTimeEnd") LocalDateTime operationTimeEnd,
            @Param("auditStatus") Integer auditStatus // 新增：审核状态筛选参数
    );
}