package com.example.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.dto.TraceAuditDTO; // 需引入审核DTO
import com.example.crm.dto.TraceRecordQueryDTO;
import com.example.crm.entity.TraceRecord;
import java.util.List;
import java.util.Map;

public interface TraceRecordService extends IService<TraceRecord> {
    /**
     * 获取最近的流向跟踪记录
     */
    List<TraceRecord> getRecentTraceList();

    /**
     * 获取首页统计数据
     */
    Map<String, Object> getTraceStat();

    /**
     * 新增流向跟踪记录
     */
    boolean addTraceRecord(TraceRecord traceRecord);

    /**
     * 修改流向跟踪记录
     */
    boolean updateTraceRecord(TraceRecord traceRecord);

    /**
     * 删除流向跟踪记录
     */
    boolean deleteTraceRecord(Long traceId);

    /**
     * 根据ID查询流向跟踪记录详情
     */
    TraceRecord getTraceDetail(Long traceId);

    /**
     * 条件分页查询流向跟踪记录（新增）
     */
    IPage<TraceRecord> queryTraceRecordPage(TraceRecordQueryDTO queryDTO);

    // ==================== 新增：审核相关方法 ====================
    /**
     * 审核追溯记录
     * @param auditDTO 审核参数
     */
    boolean auditTraceRecord(TraceAuditDTO auditDTO);

    /**
     * 同步追溯数据（审核通过后执行）
     * @param traceId 追溯记录ID
     */
    boolean syncTraceData(Long traceId);
}