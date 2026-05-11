package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.Drug;
import com.example.crm.entity.DrugBatch;
import com.example.crm.entity.OutboundOrder;
import com.example.crm.entity.OutboundOrderItem;
import com.example.crm.entity.SysUser;
import com.example.crm.mapper.DrugBatchMapper;
import com.example.crm.mapper.DrugMapper;
import com.example.crm.mapper.OutboundOrderItemMapper;
import com.example.crm.mapper.OutboundOrderMapper;
import com.example.crm.mapper.SysUserMapper;
import com.example.crm.service.OutboundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OutboundOrderServiceImpl extends ServiceImpl<OutboundOrderMapper, OutboundOrder> implements OutboundOrderService {

    @Autowired
    private OutboundOrderMapper outboundOrderMapper;
    @Autowired
    private OutboundOrderItemMapper outboundOrderItemMapper;
    @Autowired
    private DrugBatchMapper drugBatchMapper;
    @Autowired
    private DrugMapper drugMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    // 兼容前端常用的时间格式
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    @Override
    public IPage<OutboundOrder> queryOutboundPage(Page<OutboundOrder> page, Map<String, Object> params) {
        return outboundOrderMapper.selectOutboundPage(
                page,
                (String) params.get("receiverName"),
                (Integer) params.get("auditStatus"),
                (String) params.get("outboundTimeStart"),
                (String) params.get("outboundTimeEnd")
        );
    }

    // ====================== 修复：查询出库单详情（补全所有展示字段）======================
    @Override
    public OutboundOrder getOutboundDetail(Long outboundId) {
        OutboundOrder outbound = this.getById(outboundId);
        if (outbound != null) {
            List<OutboundOrderItem> items = outboundOrderItemMapper.selectByOutboundId(outboundId);

            // 填充药品、批次信息
            for (OutboundOrderItem item : items) {
                DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
                if (batch != null) {
                    item.setBatchNo(batch.getBatchNo());
                    item.setProductionDate(batch.getProductionDate());
                    item.setExpiryDate(batch.getExpiryDate());

                    Drug drug = drugMapper.selectById(batch.getDrugId());
                    if (drug != null) {
                        item.setDrugName(drug.getDrugName());
                        item.setSpecification(drug.getSpecification());
                    }
                }
            }

            // 填充操作人/审核人姓名
            if (outbound.getOperatorId() != null) {
                SysUser operator = sysUserMapper.selectById(outbound.getOperatorId());
                outbound.setOperatorName(operator != null ? operator.getLoginName() : "未知用户");
            }
            if (outbound.getAuditorId() != null) {
                SysUser auditor = sysUserMapper.selectById(outbound.getAuditorId());
                outbound.setAuditorName(auditor != null ? auditor.getLoginName() : "未知用户");
            }

            outbound.setItems(items);
        }
        return outbound;
    }

    // ====================== 【核心修改1】新增出库单：操作人自动 = 当前登录人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOutbound(OutboundOrder outboundOrder, List<OutboundOrderItem> items) {
        // 【核心新增】自动获取当前登录人作为操作人
        Long currentUserId = getCurrentUserId();
        String currentUsername = getCurrentUsername();

        outboundOrder.setOperatorId(currentUserId);
        outboundOrder.setAuditStatus(0);
        outboundOrder.setIsDeleted(0);

        boolean mainSuccess = this.save(outboundOrder);
        if (!mainSuccess) return false;

        if (!CollectionUtils.isEmpty(items)) {
            for (OutboundOrderItem item : items) {
                item.setOutboundId(outboundOrder.getOutboundId());
                outboundOrderItemMapper.insert(item);
            }
        }

        log.info("新增出库单成功：outboundId={}，操作人={}", outboundOrder.getOutboundId(), currentUsername);
        return true;
    }

    // ====================== 【核心修改2】编辑出库单：强制保留原始操作人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editOutbound(OutboundOrder outboundOrder, List<OutboundOrderItem> items) {
        OutboundOrder existOrder = this.getById(outboundOrder.getOutboundId());
        Assert.notNull(existOrder, "出库单不存在");
        Assert.isTrue(existOrder.getAuditStatus() == 0, "已审核的出库单无法编辑");

        // 【核心新增】强制保留原始操作人
        outboundOrder.setOperatorId(existOrder.getOperatorId());

        boolean mainSuccess = this.updateById(outboundOrder);
        if (!mainSuccess) return false;

        LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboundOrderItem::getOutboundId, outboundOrder.getOutboundId());
        outboundOrderItemMapper.delete(wrapper);

        if (!CollectionUtils.isEmpty(items)) {
            for (OutboundOrderItem item : items) {
                item.setOutboundId(outboundOrder.getOutboundId());
                outboundOrderItemMapper.insert(item);
            }
        }

        log.info("编辑出库单成功：outboundId={}", outboundOrder.getOutboundId());
        return true;
    }

    // ====================== 【核心修改3】审核出库单：审核人自动 = 当前登录人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 【修改】去掉 Long auditorId 和 String auditTime 参数
    public Map<String, Object> updateOutboundStatus(Long outboundId, Integer auditStatus, String auditOpinion) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 【核心新增】自动获取当前登录人
            Long currentUserId = getCurrentUserId();
            String currentUsername = getCurrentUsername();

            Assert.notNull(outboundId, "出库单ID不能为空");
            Assert.notNull(auditStatus, "审核状态不能为空");
            Assert.isTrue(auditStatus == 0 || auditStatus == 1 || auditStatus == 2, "审核状态只能是0（待审核）、1（已通过）或2（已驳回）");

            OutboundOrder order = this.getById(outboundId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "出库单不存在");
                return result;
            }

            // 状态校验
            Integer oldStatus = order.getAuditStatus();
            if (auditStatus == 1 && oldStatus == 1) {
                result.put("success", false);
                result.put("message", "该出库单已审核通过，无需重复操作");
                return result;
            }
            if (auditStatus == 2 && oldStatus == 2) {
                result.put("success", false);
                result.put("message", "该出库单已驳回，无需重复操作");
                return result;
            }
            if (auditStatus == 0 && oldStatus == 0) {
                result.put("success", false);
                result.put("message", "该出库单已是待审核状态");
                return result;
            }

            // 【核心修改】赋值：当前登录人 = 审核人，时间自动生成
            order.setAuditStatus(auditStatus);
            if (auditStatus != 0) {
                order.setAuditorId(currentUserId);
                order.setAuditTime(LocalDateTime.now());
            } else {
                // 取消审核：清空
                order.setAuditorId(null);
                order.setAuditTime(null);
            }
            order.setUpdateTime(LocalDateTime.now());
            boolean updateSuccess = this.updateById(order);

            // 审核通过则扣减药品批次库存
            if (updateSuccess && auditStatus == 1) {
                List<OutboundOrderItem> items = outboundOrderItemMapper.selectByOutboundId(outboundId);
                for (OutboundOrderItem item : items) {
                    DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
                    if (batch != null) {
                        int newStock = batch.getCurrentStock() - item.getActualOutQty();
                        if (newStock < 0) {
                            result.put("success", false);
                            result.put("message", "批次库存不足，无法出库：batchId=" + item.getBatchId());
                            throw new RuntimeException("批次库存不足");
                        }
                        batch.setCurrentStock(newStock);
                        drugBatchMapper.updateById(batch);
                    }
                }
            }

            // 取消审核则回滚库存
            if (updateSuccess && auditStatus == 0 && oldStatus == 1) {
                List<OutboundOrderItem> items = outboundOrderItemMapper.selectByOutboundId(outboundId);
                for (OutboundOrderItem item : items) {
                    DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
                    if (batch != null) {
                        int newStock = batch.getCurrentStock() + item.getActualOutQty();
                        batch.setCurrentStock(newStock);
                        drugBatchMapper.updateById(batch);
                    }
                }
            }

            if (updateSuccess) {
                result.put("success", true);
                result.put("message", "出库单状态更新成功");
                log.info("审核出库单成功：outboundId={}，审核人={}", outboundId, currentUsername);
            } else {
                result.put("success", false);
                result.put("message", "出库单状态更新失败，未修改任何记录");
            }
        } catch (IllegalArgumentException e) {
            log.error("更新出库单状态参数校验失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("更新出库单状态异常", e);
            result.put("success", false);
            result.put("message", "系统异常：" + e.getMessage());
        }
        return result;
    }

    // ====================== 辅助方法：获取当前登录人ID ======================
    private Long getCurrentUserId() {
        Long currentUserId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            currentUserId = 5L;
            log.warn("未获取到有效登录信息，临时使用测试用户ID: {}", currentUserId);
        } else {
            if (authentication.getPrincipal() instanceof SysUser sysUser) {
                currentUserId = sysUser.getId();
            } else {
                currentUserId = 5L;
            }
        }
        return currentUserId;
    }

    // ====================== 辅助方法：获取当前登录人姓名(login_name) ======================
    private String getCurrentUsername() {
        String currentUsername = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            currentUsername = "测试用户";
        } else {
            if (authentication.getPrincipal() instanceof SysUser sysUser) {
                currentUsername = sysUser.getLoginName(); // ✅ 使用 login_name
            } else {
                currentUsername = "测试用户";
            }
        }
        return currentUsername;
    }

    // ====================== 辅助方法：万能时间解析 ======================
    private LocalDateTime parseDateTimeSafely(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        String cleanStr = dateTimeStr.trim();

        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(cleanStr, formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一种格式
            }
        }

        throw new IllegalArgumentException("时间格式解析失败：" + dateTimeStr + "，支持格式：yyyy-MM-dd HH:mm:ss");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelOutboundAudit(Long outboundId) {
        // 直接调用状态更新方法，设置为待审核(0)
        return updateOutboundStatus(outboundId, 0, "取消审核");
    }
}