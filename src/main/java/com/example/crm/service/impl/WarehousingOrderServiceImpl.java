package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.Drug;
import com.example.crm.entity.DrugBatch;
import com.example.crm.entity.SysUser;
import com.example.crm.entity.WarehousingOrder;
import com.example.crm.entity.WarehousingOrderItem;
import com.example.crm.mapper.DrugBatchMapper;
import com.example.crm.mapper.DrugMapper;
import com.example.crm.mapper.WarehousingOrderItemMapper;
import com.example.crm.mapper.WarehousingOrderMapper;
import com.example.crm.service.WarehousingOrderService;
import com.example.crm.vo.DrugBatchVO;
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
public class WarehousingOrderServiceImpl extends ServiceImpl<WarehousingOrderMapper, WarehousingOrder> implements WarehousingOrderService {

    @Autowired
    private WarehousingOrderMapper warehousingOrderMapper;
    @Autowired
    private WarehousingOrderItemMapper warehousingOrderItemMapper;
    @Autowired
    private DrugBatchMapper drugBatchMapper;
    @Autowired
    private DrugMapper drugMapper;

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    @Override
    public IPage<WarehousingOrder> queryWarehousingPage(Page<WarehousingOrder> page, Map<String, Object> params) {
        try {
            IPage<WarehousingOrder> orderPage = warehousingOrderMapper.selectWarehousingPage(
                    page,
                    (String) params.get("purchaseNo"),
                    (Integer) params.get("auditStatus"),
                    (String) params.get("warehousingTimeStart"),
                    (String) params.get("warehousingTimeEnd")
            );

            if (orderPage.getRecords() != null && !orderPage.getRecords().isEmpty()) {
                for (WarehousingOrder order : orderPage.getRecords()) {
                    List<WarehousingOrderItem> items = warehousingOrderItemMapper.selectByWarehousingId(order.getWarehousingId());
                    if (items != null && !items.isEmpty()) {
                        order.setTotalDrugCount(items.size());
                        order.setTotalInQty(items.stream().mapToInt(WarehousingOrderItem::getActualInQty).sum());

                        StringBuilder summary = new StringBuilder();
                        for (int i = 0; i < Math.min(items.size(), 2); i++) {
                            WarehousingOrderItem item = items.get(i);
                            if (item.getBatchId() != null) {
                                DrugBatchVO batch = drugBatchMapper.selectBatchWithDrugById(item.getBatchId());
                                if (batch != null) {
                                    item.setDrugName(batch.getDrugName());
                                    summary.append(batch.getDrugName()).append("、");
                                }
                            }
                        }
                        if (summary.length() > 0) {
                            summary.deleteCharAt(summary.length() - 1);
                            if (items.size() > 2) {
                                summary.append(" 等").append(items.size()).append("种");
                            }
                        }
                        order.setDrugSummary(summary.toString());
                    } else {
                        order.setTotalDrugCount(0);
                        order.setTotalInQty(0);
                        order.setDrugSummary("无明细");
                    }

                    if (order.getOperatorName() == null || order.getOperatorName().isEmpty()) {
                        order.setOperatorName(order.getOperatorId() == null ? "-" : order.getOperatorId().toString());
                    }
                    if (order.getAuditorName() == null || order.getAuditorName().isEmpty()) {
                        order.setAuditorName(order.getAuditorId() == null ? "-" : order.getAuditorId().toString());
                    }
                }
            }

            return orderPage;
        } catch (Exception e) {
            log.error("分页查询入库单失败", e);
            throw new RuntimeException("分页查询入库单失败：" + e.getMessage());
        }
    }

    @Override
    public WarehousingOrder getWarehousingDetail(Long warehousingId) {
        Assert.notNull(warehousingId, "入库单ID不能为空");
        WarehousingOrder warehousing = this.getById(warehousingId);
        if (warehousing == null) {
            log.warn("入库单不存在：warehousingId={}", warehousingId);
            return null;
        }

        List<WarehousingOrderItem> items = warehousingOrderItemMapper.selectByWarehousingId(warehousingId);
        if (items != null && !items.isEmpty()) {
            for (WarehousingOrderItem item : items) {
                if (item.getBatchId() != null) {
                    DrugBatchVO batchVO = drugBatchMapper.selectBatchWithDrugById(item.getBatchId());
                    if (batchVO != null) {
                        item.setDrugId(batchVO.getDrugId());
                        item.setBatchNo(batchVO.getBatchNo());
                        item.setProductionDate(batchVO.getProductionDate());
                        item.setExpiryDate(batchVO.getExpiryDate());
                        item.setDrugName(batchVO.getDrugName());
                        item.setSpecification(batchVO.getSpecification());
                    } else {
                        DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
                        if (batch != null) {
                            item.setDrugId(batch.getDrugId());
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
                }
            }
        }
        warehousing.setItems(items);
        return warehousing;
    }

    // ====================== 【核心修改1】新增入库单：操作人自动 = 当前登录人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWarehousing(WarehousingOrder warehousingOrder, List<WarehousingOrderItem> items) {
        Assert.notNull(warehousingOrder, "入库单主信息不能为空");
        Assert.notNull(warehousingOrder.getPurchaseId(), "采购单ID不能为空");
        // 【移除】不再要求前端传 operatorId
        // Assert.notNull(warehousingOrder.getOperatorId(), "操作人ID不能为空");
        Assert.notNull(warehousingOrder.getWarehousingTime(), "入库时间不能为空");

        if (CollectionUtils.isEmpty(items)) {
            log.error("新增入库单失败：明细不能为空");
            throw new IllegalArgumentException("入库明细不能为空");
        }
        for (WarehousingOrderItem item : items) {
            Assert.notNull(item.getBatchId(), "明细批次ID不能为空");
            Assert.isTrue(item.getActualInQty() > 0, "实际入库数量必须大于0");
        }

        // 【核心新增】自动获取当前登录人作为操作人
        Long currentUserId = null;
        String currentUsername = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            currentUserId = 5L;
            currentUsername = "测试用户";
            log.warn("未获取到有效登录信息，临时使用测试用户ID: {}", currentUserId);
        } else {
            if (authentication.getPrincipal() instanceof SysUser sysUser) {
                currentUserId = sysUser.getId();
                currentUsername = sysUser.getLoginName();
            } else {
                currentUserId = 5L;
                currentUsername = "测试用户";
            }
        }

        // 【关键】自动赋值操作人ID
        warehousingOrder.setOperatorId(currentUserId);

        if (warehousingOrder.getAuditStatus() == null) {
            warehousingOrder.setAuditStatus(0);
        }
        warehousingOrder.setIsDeleted(0);

        boolean mainSuccess = this.save(warehousingOrder);
        if (!mainSuccess) {
            log.error("新增入库单主表失败：warehousingId={}", warehousingOrder.getWarehousingId());
            return false;
        }

        Long warehousingId = warehousingOrder.getWarehousingId();
        for (WarehousingOrderItem item : items) {
            item.setWarehousingId(warehousingId);
            warehousingOrderItemMapper.insert(item);
        }

        log.info("新增入库单成功：warehousingId={}，操作人={}", warehousingId, currentUsername);
        return true;
    }

    // ====================== 【核心修改2】编辑入库单：强制保留原始操作人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editWarehousing(WarehousingOrder warehousingOrder, List<WarehousingOrderItem> items) {
        Assert.notNull(warehousingOrder, "入库单主信息不能为空");
        Assert.notNull(warehousingOrder.getWarehousingId(), "入库单ID不能为空");

        WarehousingOrder existOrder = this.getById(warehousingOrder.getWarehousingId());
        if (existOrder == null) {
            String errorMsg = String.format("入库单不存在：warehousingId=%d", warehousingOrder.getWarehousingId());
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        Assert.isTrue(existOrder.getAuditStatus() == 0, "已审核的入库单无法编辑");

        // 【核心新增】强制保留原始操作人，禁止修改
        warehousingOrder.setOperatorId(existOrder.getOperatorId());

        boolean mainSuccess = this.updateById(warehousingOrder);
        if (!mainSuccess) {
            log.error("编辑入库单主表失败：warehousingId={}", warehousingOrder.getWarehousingId());
            return false;
        }

        LambdaQueryWrapper<WarehousingOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehousingOrderItem::getWarehousingId, warehousingOrder.getWarehousingId());
        warehousingOrderItemMapper.delete(wrapper);

        if (!CollectionUtils.isEmpty(items)) {
            Long warehousingId = warehousingOrder.getWarehousingId();
            for (WarehousingOrderItem item : items) {
                Assert.isTrue(item.getActualInQty() > 0, "实际入库数量必须大于0");
                item.setWarehousingId(warehousingId);
                warehousingOrderItemMapper.insert(item);
            }
        }

        log.info("编辑入库单成功：warehousingId={}", warehousingOrder.getWarehousingId());
        return true;
    }

    // ====================== 核心改动：审核人自动获取当前登录人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 【修改】去掉 Long auditorId 参数
    public Map<String, Object> updateWarehousingStatus(Long warehousingId, Integer auditStatus, String auditTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 获取当前登录用户
            Long currentUserId = null;
            String currentUsername = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                currentUserId = 5L;
                currentUsername = "测试用户";
                log.warn("未获取到有效登录信息，临时使用测试用户ID: {}", currentUserId);
            } else {
                if (authentication.getPrincipal() instanceof SysUser sysUser) {
                    currentUserId = sysUser.getId();
                    currentUsername = sysUser.getLoginName(); // ✅ 使用 login_name
                } else {
                    currentUserId = 5L;
                    currentUsername = "测试用户";
                }
            }

            Assert.notNull(warehousingId, "入库单ID不能为空");
            Assert.notNull(auditStatus, "审核状态不能为空");
            Assert.isTrue(auditStatus == 0 || auditStatus == 1, "审核状态只能是0（待审核）或1（已通过）");
            Assert.notNull(auditTime, "审核时间不能为空");

            WarehousingOrder warehousing = this.getById(warehousingId);
            if (warehousing == null) {
                result.put("success", false);
                result.put("message", "入库单不存在");
                return result;
            }

            Integer oldStatus = warehousing.getAuditStatus();
            if (auditStatus == 1 && oldStatus == 1) {
                result.put("success", false);
                result.put("message", "该入库单已审核通过，无需重复操作");
                return result;
            }
            if (auditStatus == 0 && oldStatus == 0) {
                result.put("success", false);
                result.put("message", "该入库单已是待审核状态");
                return result;
            }

            LocalDateTime auditTimeLdt = parseAuditTime(auditTime);
            if (auditTimeLdt == null) {
                result.put("success", false);
                result.put("message", "审核时间格式错误，支持格式：yyyy-MM-dd HH:mm:ss、ISO格式");
                return result;
            }

            // 2. 赋值：当前登录人 = 审核人
            warehousing.setAuditStatus(auditStatus);
            if (auditStatus != 0) {
                warehousing.setAuditorId(currentUserId);
                warehousing.setAuditorName(currentUsername);
                warehousing.setAuditTime(auditTimeLdt);
            } else {
                warehousing.setAuditorId(null);
                warehousing.setAuditorName(null);
                warehousing.setAuditTime(null);
            }
            warehousing.setUpdateTime(LocalDateTime.now());

            boolean updateSuccess = this.updateById(warehousing);

            // 3. 库存更新
            if (updateSuccess) {
                List<WarehousingOrderItem> items = warehousingOrderItemMapper.selectByWarehousingId(warehousingId);
                for (WarehousingOrderItem item : items) {
                    DrugBatch batch = drugBatchMapper.selectById(item.getBatchId());
                    if (batch == null) {
                        log.warn("药品批次不存在：batchId={}，跳过库存更新", item.getBatchId());
                        continue;
                    }

                    int changeQty = (auditStatus == 1) ? item.getActualInQty() : -item.getActualInQty();
                    if (auditStatus == 0 && batch.getCurrentStock() < item.getActualInQty()) {
                        log.warn("批次库存不足，无法回滚：batchId={}，当前库存={}，需扣减={}",
                                item.getBatchId(), batch.getCurrentStock(), item.getActualInQty());
                        continue;
                    }

                    batch.setCurrentStock(batch.getCurrentStock() + changeQty);
                    drugBatchMapper.updateById(batch);
                    log.info("批次库存更新成功：batchId={}，变化量={}", batch.getBatchId(), changeQty);
                }
            }

            if (updateSuccess) {
                result.put("success", true);
                result.put("message", auditStatus == 1 ? "审核通过，库存已更新" : "取消审核成功，库存已回滚");
            } else {
                result.put("success", false);
                result.put("message", "入库单状态更新失败");
            }
        } catch (IllegalArgumentException e) {
            log.error("更新入库单状态参数校验失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("更新入库单状态异常", e);
            result.put("success", false);
            result.put("message", "系统异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWarehousing(Long warehousingId) {
        Assert.notNull(warehousingId, "入库单ID不能为空");
        WarehousingOrder existOrder = this.getById(warehousingId);
        if (existOrder == null) {
            log.warn("删除入库单失败：入库单不存在");
            return false;
        }
        if (existOrder.getAuditStatus() == 1) {
            log.error("删除入库单失败：已审核的入库单不允许删除");
            throw new IllegalArgumentException("已审核的入库单不允许删除");
        }
        boolean deleteSuccess = this.removeById(warehousingId);
        if (deleteSuccess) {
            LambdaQueryWrapper<WarehousingOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WarehousingOrderItem::getWarehousingId, warehousingId);
            warehousingOrderItemMapper.delete(wrapper);
            log.info("删除入库单成功：warehousingId={}", warehousingId);
        }
        return deleteSuccess;
    }

    private LocalDateTime parseAuditTime(String auditTime) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(auditTime, formatter);
            } catch (DateTimeParseException e) {
                continue;
            }
        }
        return null;
    }
}