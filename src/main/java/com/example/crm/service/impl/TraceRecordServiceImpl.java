package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.dto.TraceAuditDTO; // 新增引入
import com.example.crm.dto.TraceRecordQueryDTO;
import com.example.crm.entity.Drug;
import com.example.crm.entity.DrugBatch;
import com.example.crm.entity.SysUser;
import com.example.crm.entity.TraceRecord;
import com.example.crm.mapper.TraceRecordMapper;
import com.example.crm.service.DrugBatchService;
import com.example.crm.service.DrugService;
import com.example.crm.service.TraceRecordService;
import com.example.crm.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TraceRecordServiceImpl extends ServiceImpl<TraceRecordMapper, TraceRecord> implements TraceRecordService {

    private static final Logger log = LoggerFactory.getLogger(TraceRecordServiceImpl.class);

    @Autowired
    private DrugService drugService;

    @Autowired
    private DrugBatchService drugBatchService;

    @Autowired
    private SysUserService sysUserService;

    // ==================== 原有方法（未修改） ====================
    @Override
    public List<TraceRecord> getRecentTraceList() {
        List<TraceRecord> list = list(
                new LambdaQueryWrapper<TraceRecord>()
                        .orderByDesc(TraceRecord::getOperationTime)
                        .last("LIMIT 20")
        );
        fillRelatedInfo(list);
        return list;
    }

    @Override
    public Map<String, Object> getTraceStat() {
        Map<String, Object> stat = new HashMap<>();
        stat.put("totalTrace", count());
        stat.put("todayTrace", count(
                new LambdaQueryWrapper<TraceRecord>()
                        .ge(TraceRecord::getOperationTime, LocalDate.now().atStartOfDay())
        ));
        stat.put("drugCount", list().stream().map(TraceRecord::getDrugId).distinct().count());
        // 修复：从硬编码0改为实际查询待审核数量（auditStatus=0）
        stat.put("pendingAudit", count(
                new LambdaQueryWrapper<TraceRecord>()
                        .eq(TraceRecord::getAuditStatus, 0)
        ));
        return stat;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTraceRecord(TraceRecord traceRecord) {
        if (traceRecord.getDrugId() == null) throw new RuntimeException("药品ID不能为空");
        if (traceRecord.getBatchId() == null) throw new RuntimeException("批次ID不能为空");
        if (!StringUtils.hasText(traceRecord.getDocumentType())) throw new RuntimeException("单据类型不能为空");
        if (traceRecord.getDocumentId() == null) throw new RuntimeException("单据ID不能为空");
        if (traceRecord.getOperatorId() == null) throw new RuntimeException("操作人ID不能为空");
        if (!StringUtils.hasText(traceRecord.getFlowNode())) throw new RuntimeException("流转节点不能为空");

        // 新增时默认设置审核状态为 0（待审核）
        traceRecord.setAuditStatus(0);
        traceRecord.setOperationTime(LocalDateTime.now()); // 补充操作时间默认值
        return save(traceRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTraceRecord(TraceRecord traceRecord) {
        if (traceRecord.getTraceId() == null) throw new RuntimeException("追溯ID不能为空");
        TraceRecord existing = getById(traceRecord.getTraceId());
        if (existing == null) throw new RuntimeException("追溯记录不存在");

        // 新增校验：已审核（状态=1）的记录禁止修改
        if (existing.getAuditStatus() != null && existing.getAuditStatus() == 1) {
            throw new RuntimeException("已审核的追溯记录不允许修改");
        }

        existing.setFlowNode(traceRecord.getFlowNode());
        existing.setQuantityChange(traceRecord.getQuantityChange());
        return updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTraceRecord(Long traceId) {
        if (traceId == null) throw new RuntimeException("追溯ID不能为空");
        TraceRecord existing = getById(traceId);
        if (existing == null) throw new RuntimeException("追溯记录不存在");

        // 新增校验：已审核（状态=1）的记录禁止删除
        if (existing.getAuditStatus() != null && existing.getAuditStatus() == 1) {
            throw new RuntimeException("已审核的追溯记录不允许删除");
        }

        return removeById(traceId);
    }

    @Override
    public TraceRecord getTraceDetail(Long traceId) {
        if (traceId == null) throw new RuntimeException("追溯ID不能为空");
        TraceRecord record = getById(traceId);
        if (record == null) throw new RuntimeException("追溯记录不存在");
        fillRelatedInfo(List.of(record));
        return record;
    }

    @Override
    public IPage<TraceRecord> queryTraceRecordPage(TraceRecordQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<TraceRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 核心修复：提前计算偏移量（避免在SQL中做算术运算）
        Integer offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();

        // 3. 调用自定义Mapper查询列表（兼容审核状态筛选，需同步修改Mapper）
        List<TraceRecord> records = baseMapper.selectTraceRecordList(
                queryDTO.getDrugId(),
                queryDTO.getBatchId(),
                queryDTO.getDocumentType(),
                queryDTO.getDrugName(),
                queryDTO.getBatchNo(),
                queryDTO.getOperatorName(),
                queryDTO.getFlowNode(),
                queryDTO.getOperationTimeStart(),
                queryDTO.getOperationTimeEnd(),
                queryDTO.getAuditStatus(), // 新增：传递审核状态筛选参数
                queryDTO.getPageSize(),
                offset
        );

        // 4. 调用自定义Mapper查询总数（兼容审核状态筛选，需同步修改Mapper）
        Long total = baseMapper.selectTraceRecordCount(
                queryDTO.getDrugId(),
                queryDTO.getBatchId(),
                queryDTO.getDocumentType(),
                queryDTO.getDrugName(),
                queryDTO.getBatchNo(),
                queryDTO.getOperatorName(),
                queryDTO.getFlowNode(),
                queryDTO.getOperationTimeStart(),
                queryDTO.getOperationTimeEnd(),
                queryDTO.getAuditStatus() // 新增：传递审核状态筛选参数
        );

        // 5. 组装分页结果
        page.setRecords(records);
        page.setTotal(total);

        // 6. 填充关联信息
        fillRelatedInfo(page.getRecords());

        return page;
    }

    private void fillRelatedInfo(List<TraceRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // 1. 收集各类ID
        List<Long> drugIds = list.stream().map(TraceRecord::getDrugId).distinct().collect(Collectors.toList());
        List<Long> batchIds = list.stream().map(TraceRecord::getBatchId).distinct().collect(Collectors.toList());
        List<Long> operatorIds = list.stream().map(TraceRecord::getOperatorId).distinct().collect(Collectors.toList());
        List<Long> auditorIds = list.stream()
                .filter(record -> record.getAuditorId() != null)
                .map(TraceRecord::getAuditorId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 药品名称映射（修复：使用 putAll 避免重新赋值）
        Map<Long, String> drugNameMap = new HashMap<>();
        if (!drugIds.isEmpty()) {
            drugNameMap.putAll(
                    drugService.listByIds(drugIds).stream()
                            .collect(Collectors.toMap(Drug::getDrugId, Drug::getDrugName))
            );
        }

        // 3. 批次号映射（同理修复）
        Map<Long, String> batchNoMap = new HashMap<>();
        if (!batchIds.isEmpty()) {
            batchNoMap.putAll(
                    drugBatchService.listByIds(batchIds).stream()
                            .collect(Collectors.toMap(DrugBatch::getBatchId, DrugBatch::getBatchNo))
            );
        }

        // 4. 操作人名称映射（同理修复）
        Map<Long, String> operatorNameMap = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            operatorNameMap.putAll(
                    sysUserService.listByIds(operatorIds).stream()
                            .collect(Collectors.toMap(SysUser::getId, SysUser::getLoginName))
            );
        }

        // 5. 审核人名称映射（同理修复）
        Map<Long, String> auditorNameMap = new HashMap<>();
        if (!auditorIds.isEmpty()) {
            auditorNameMap.putAll(
                    sysUserService.listByIds(auditorIds).stream()
                            .collect(Collectors.toMap(SysUser::getId, SysUser::getLoginName))
            );
        }

        // 6. 填充关联信息（lambda 引用的变量现在是 effectively final）
        list.forEach(record -> {
            record.setDrugName(drugNameMap.get(record.getDrugId()));
            record.setBatchNo(batchNoMap.get(record.getBatchId()));
            record.setOperatorName(operatorNameMap.get(record.getOperatorId()));
            record.setAuditorName(auditorNameMap.get(record.getAuditorId()));
        });
    }

    // ==================== 新增：审核核心实现（未修改） ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditTraceRecord(TraceAuditDTO auditDTO) {
        // 1. 参数校验
        if (auditDTO.getTraceId() == null) throw new RuntimeException("追溯ID不能为空");
        if (auditDTO.getAuditResult() == null) throw new RuntimeException("审核结果不能为空");
        if (auditDTO.getAuditorId() == null) throw new RuntimeException("审核人ID不能为空");
        // 驳回时必须填写备注
        if (auditDTO.getAuditResult() == 2 && !StringUtils.hasText(auditDTO.getAuditRemark())) {
            throw new RuntimeException("驳回审核时必须填写备注说明原因");
        }

        // 2. 查询待审核记录
        TraceRecord traceRecord = getById(auditDTO.getTraceId());
        if (traceRecord == null) throw new RuntimeException("追溯记录不存在");
        // 只能审核待审核（状态=0）的记录
        if (traceRecord.getAuditStatus() == null || traceRecord.getAuditStatus() != 0) {
            throw new RuntimeException("该记录非待审核状态，无法执行审核操作");
        }

        // 3. 更新审核信息
        traceRecord.setAuditStatus(auditDTO.getAuditResult()); // 1=通过，2=驳回
        traceRecord.setAuditorId(auditDTO.getAuditorId());
        traceRecord.setAuditTime(LocalDateTime.now());
        traceRecord.setAuditRemark(auditDTO.getAuditRemark());

        return updateById(traceRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncTraceData(Long traceId) {
        // 1. 参数校验
        if (traceId == null) throw new RuntimeException("追溯ID不能为空");

        try {
            // 2. 查询追溯记录详情
            TraceRecord traceRecord = getById(traceId);
            if (traceRecord == null) {
                log.error("同步数据失败：追溯记录ID={} 不存在", traceId);
                return false;
            }
            // 仅同步已审核通过（状态=1）的记录
            if (traceRecord.getAuditStatus() != 1) {
                log.error("同步数据失败：追溯记录ID={} 未审核通过", traceId);
                return false;
            }

            // 3. 核心同步逻辑（根据业务场景适配）
            // 3.1 查询批次信息
            DrugBatch drugBatch = drugBatchService.getById(traceRecord.getBatchId());
            if (drugBatch == null) {
                log.error("同步数据失败：批次ID={} 不存在", traceRecord.getBatchId());
                return false;
            }

            // 3.2 根据单据类型同步库存（核心业务逻辑）
            // 入库单：库存 += 数量变动
            if ("入库单".equals(traceRecord.getDocumentType())) {
                drugBatch.setCurrentStock(drugBatch.getCurrentStock() + traceRecord.getQuantityChange());
            }
            // 出库单：库存 -= 数量变动（需校验库存充足）
            else if ("出库单".equals(traceRecord.getDocumentType())) {
                if (drugBatch.getCurrentStock() < traceRecord.getQuantityChange()) {
                    throw new RuntimeException("批次库存不足，无法完成出库同步");
                }
                drugBatch.setCurrentStock(drugBatch.getCurrentStock() - traceRecord.getQuantityChange());
            }
            // 采购单：仅更新单据状态，不修改库存（需根据实际单据表调整）
            else if ("采购单".equals(traceRecord.getDocumentType())) {
                // 示例：调用采购单Service更新状态（需根据实际代码调整）
                // purchaseOrderService.updateOrderStatus(traceRecord.getDocumentId(), 1);
            }

            // 3.3 保存批次库存更新
            drugBatchService.updateById(drugBatch);
            log.info("追溯记录ID={} 数据同步成功，批次ID={} 库存更新为{}",
                    traceId, drugBatch.getBatchId(), drugBatch.getCurrentStock());

            return true;
        } catch (Exception e) {
            log.error("同步追溯数据失败：", e);
            throw new RuntimeException("数据同步失败：" + e.getMessage());
        }
    }
}