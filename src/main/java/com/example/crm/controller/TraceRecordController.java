package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.crm.common.Result;
import com.example.crm.entity.*;
import com.example.crm.mapper.*;
import com.example.crm.service.TraceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trace")
public class TraceRecordController {

    private static final Logger log = LoggerFactory.getLogger(TraceRecordController.class);

    @Autowired
    private TraceRecordService traceRecordService;

    // 新增注入所需的Mapper
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private OutboundOrderMapper outboundOrderMapper;
    @Autowired
    private OutboundOrderItemMapper outboundOrderItemMapper;
    @Autowired
    private WarehousingOrderMapper warehousingOrderMapper;
    @Autowired
    private WarehousingOrderItemMapper warehousingOrderItemMapper;
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private DrugBatchMapper drugBatchMapper;

    /**
     * 更新追溯记录流转节点
     * @param params 包含traceId（追溯ID）、flowNode（新流转节点）
     * @return 操作结果
     */
    @PostMapping("/updateFlowNode")
    public Result<?> updateTraceFlowNode(@RequestBody Map<String, Object> params) {
        try {
            Long traceId = Long.valueOf(params.get("traceId").toString());
            String flowNode = params.get("flowNode").toString();

            if (traceId == null || flowNode == null || flowNode.trim().isEmpty()) {
                return Result.error("追溯ID和流转节点不能为空");
            }

            TraceRecord trace = traceRecordService.getById(traceId);
            if (trace == null) {
                return Result.error("追溯记录不存在");
            }

            trace.setFlowNode(flowNode);
            boolean success = traceRecordService.updateById(trace);

            return success ? Result.success("流转节点更新成功") : Result.error("流转节点更新失败");
        } catch (Exception e) {
            log.error("更新追溯记录流转节点异常：", e);
            return Result.error("系统异常，流转节点更新失败");
        }
    }

    /**
     * 完整编辑追溯记录（核心新增接口，解决修改未生效问题）
     * 直接接收前端完整参数，不查询旧数据覆盖，仅更新传入的字段
     */
    @PutMapping("/edit")
    public Result<?> editTraceRecord(@RequestBody TraceRecord traceRecord) {
        try {
            // 必传参数校验
            if (traceRecord.getTraceId() == null) {
                return Result.error("追溯ID不能为空");
            }

            // 直接更新（MyBatis-Plus会根据traceId更新非空字段，不覆盖未传的旧字段）
            boolean success = traceRecordService.updateById(traceRecord);

            if (success) {
                return Result.success("修改成功");
            } else {
                return Result.error("修改失败，记录不存在或未变更");
            }
        } catch (Exception e) {
            log.error("编辑追溯记录异常，参数：{}", traceRecord, e);
            return Result.error("系统异常，编辑失败");
        }
    }

    /**
     * 兼容前端/home/trace路径的PUT请求（适配前端原有接口路径）
     */
    @PutMapping("/home/trace")
    public Result<?> editTraceRecordCompat(@RequestBody TraceRecord traceRecord) {
        return editTraceRecord(traceRecord);
    }

    /**
     * 原始流向单取消审核（核心新增：全量数据回写）
     * @param params 包含traceId（追溯ID）、operatorId（操作人ID）、remark（取消原因，可选）
     * @return 操作结果
     */
    @PostMapping("/cancelAudit")
    @Transactional(rollbackFor = Exception.class) // 事务控制，确保数据一致性
    public Result<?> cancelAudit(@RequestBody Map<String, Object> params) {
        try {
            // 1. 参数解析与校验
            Long traceId = params.get("traceId") != null ? Long.valueOf(params.get("traceId").toString()) : null;
            Long operatorId = params.get("operatorId") != null ? Long.valueOf(params.get("operatorId").toString()) : null;

            if (traceId == null) {
                return Result.error("追溯ID不能为空");
            }
            if (operatorId == null) {
                return Result.error("操作人ID不能为空");
            }

            // 2. 查询原始流向单
            TraceRecord trace = traceRecordService.getById(traceId);
            if (trace == null) {
                return Result.error("原始流向单不存在");
            }
            if (trace.getAuditStatus() != 1) {
                return Result.error("只能取消已审核通过的流向单");
            }

            // 3. 回写关联单据 + 库存
            String docType = trace.getDocumentType();
            Long docId = trace.getDocumentId();
            switch (docType) {
                case "出库单":
                    rollbackOutbound(docId);
                    break;
                case "入库单":
                    rollbackWarehousing(docId);
                    break;
                case "采购单":
                    rollbackPurchase(docId);
                    break;
                default:
                    log.warn("流向单关联未知单据类型：{}，仅回滚自身状态", docType);
            }

            // 4. 回滚流向单自身审核状态
            trace.setAuditStatus(0);       // 待审核
            trace.setAuditorId(null);      // 清空审核人
            trace.setAuditTime(null);      // 清空审核时间
            trace.setAuditRemark(null);    // 清空审核备注
            boolean ok = traceRecordService.updateById(trace);

            return ok ? Result.success("取消审核成功，已同步回写所有关联数据") : Result.error("取消审核失败");
        } catch (Exception e) {
            log.error("原始流向单取消审核异常：traceId={}", params.get("traceId"), e);
            return Result.error("系统异常：" + e.getMessage());
        }
    }

    /**
     * 回写出库单 + 恢复库存（私有辅助方法）
     */
    private void rollbackOutbound(Long outboundId) {
        // ① 回滚出库单审核状态
        LambdaUpdateWrapper<OutboundOrder> outWrapper = new LambdaUpdateWrapper<>();
        outWrapper.eq(OutboundOrder::getOutboundId, outboundId)
                .set(OutboundOrder::getAuditStatus, 0)
                .set(OutboundOrder::getAuditorId, null)
                .set(OutboundOrder::getAuditTime, null);
        outboundOrderMapper.update(null, outWrapper);

        // ② 恢复药品批次库存（出库数量加回）
        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOutboundId, outboundId);
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(itemWrapper);
        for (OutboundOrderItem item : items) {
            DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
            if (batch != null) {
                batch.setCurrentStock(batch.getCurrentStock() + item.getActualOutQty());
                drugBatchMapper.updateById(batch);
            }
        }
    }

    /**
     * 回写入库单 + 扣减库存（私有辅助方法）
     */
    private void rollbackWarehousing(Long warehousingId) {
        // ① 回滚入库单审核状态
        LambdaUpdateWrapper<WarehousingOrder> inWrapper = new LambdaUpdateWrapper<>();
        inWrapper.eq(WarehousingOrder::getWarehousingId, warehousingId)
                .set(WarehousingOrder::getAuditStatus, 0)
                .set(WarehousingOrder::getAuditorId, null)
                .set(WarehousingOrder::getAuditTime, null);
        warehousingOrderMapper.update(null, inWrapper);

        // ② 扣减药品批次库存（入库数量扣除）
        LambdaQueryWrapper<WarehousingOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(WarehousingOrderItem::getWarehousingId, warehousingId);
        List<WarehousingOrderItem> items = warehousingOrderItemMapper.selectList(itemWrapper);
        for (WarehousingOrderItem item : items) {
            DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
            if (batch != null) {
                if (batch.getCurrentStock() < item.getActualInQty()) {
                    throw new RuntimeException("批次库存不足，无法回写：batchId=" + item.getBatchId());
                }
                batch.setCurrentStock(batch.getCurrentStock() - item.getActualInQty());
                drugBatchMapper.updateById(batch);
            }
        }
    }

    /**
     * 回写采购单状态（私有辅助方法）
     */
    private void rollbackPurchase(Long purchaseId) {
        LambdaUpdateWrapper<PurchaseOrder> purWrapper = new LambdaUpdateWrapper<>();
        purWrapper.eq(PurchaseOrder::getPurchaseId, purchaseId)
                .set(PurchaseOrder::getOrderStatus, 0)
                .set(PurchaseOrder::getAuditorId, null)
                .set(PurchaseOrder::getAuditTime, null)
                .set(PurchaseOrder::getAuditOpinion, null);
        purchaseOrderMapper.update(null, purWrapper);
    }
}