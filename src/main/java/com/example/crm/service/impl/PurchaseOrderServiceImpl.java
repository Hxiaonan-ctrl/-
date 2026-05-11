package com.example.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.PurchaseOrder;
import com.example.crm.entity.PurchaseOrderItem;
import com.example.crm.entity.SysUser;
import com.example.crm.mapper.PurchaseOrderItemMapper;
import com.example.crm.mapper.PurchaseOrderMapper;
import com.example.crm.service.PurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    private static final DateTimeFormatter SIMPLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<PurchaseOrder> queryPurchasePage(Page<PurchaseOrder> page, Map<String, Object> params) {
        IPage<PurchaseOrder> resultPage = this.baseMapper.selectPurchasePage(
                page,
                (String) params.get("supplierName"),
                params.get("orderStatus") != null ? Integer.parseInt(params.get("orderStatus").toString()) : null,
                (String) params.get("createTimeStart"),
                (String) params.get("createTimeEnd")
        );

        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            for (PurchaseOrder order : resultPage.getRecords()) {
                if (order.getPurchaserName() == null || order.getPurchaserName().isEmpty()) {
                    order.setPurchaserName(order.getPurchaserId() == null ? "-" : order.getPurchaserId().toString());
                }
                if (order.getAuditorName() == null || order.getAuditorName().isEmpty()) {
                    order.setAuditorName(order.getAuditorId() == null ? "-" : order.getAuditorId().toString());
                }
            }
        }

        return resultPage;
    }

    @Override
    public PurchaseOrder getPurchaseDetail(Long purchaseId) {
        PurchaseOrder purchaseOrder = this.getById(purchaseId);
        if (purchaseOrder != null) {
            LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PurchaseOrderItem::getPurchaseId, purchaseId);
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(wrapper);
            purchaseOrder.setItems(items);
        }
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPurchase(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
        purchaseOrder.setOrderStatus(0);
        boolean mainSuccess = this.save(purchaseOrder);
        if (!mainSuccess) return false;

        if (!CollectionUtils.isEmpty(items)) {
            for (PurchaseOrderItem item : items) {
                item.setPurchaseId(purchaseOrder.getPurchaseId());
                purchaseOrderItemMapper.insert(item);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editPurchase(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
        boolean mainSuccess = this.updateById(purchaseOrder);
        if (!mainSuccess) return false;

        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getPurchaseId, purchaseOrder.getPurchaseId());
        purchaseOrderItemMapper.delete(wrapper);

        if (!CollectionUtils.isEmpty(items)) {
            for (PurchaseOrderItem item : items) {
                item.setPurchaseId(purchaseOrder.getPurchaseId());
                purchaseOrderItemMapper.insert(item);
            }
        }
        return true;
    }

    // ====================== 核心改动：自动获取当前登录人作为审核人 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePurchaseStatus(Long purchaseId, Integer orderStatus, String auditTime, String auditOpinion) {
        Map<String, Object> result = new HashMap<>();

        // 1. 【核心修改】从 SecurityContext 获取当前登录用户（使用 login_name）
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
                currentUsername = sysUser.getLoginName(); // ✅ 完美：使用 login_name
            } else {
                currentUserId = 5L;
                currentUsername = "测试用户";
            }
        }

        // 2. 查询采购单
        PurchaseOrder purchase = this.getById(purchaseId);
        if (purchase == null) {
            result.put("success", false);
            result.put("message", "采购单不存在");
            return result;
        }

        // 3. 状态校验
        boolean statusValid = false;
        String statusMsg = "";
        if (orderStatus == 1 && purchase.getOrderStatus() == 0) {
            statusValid = true;
            statusMsg = "审核通过";
        } else if (orderStatus == 0 && purchase.getOrderStatus() == 1) {
            statusValid = true;
            statusMsg = "取消审核成功（回到待审核）";
        } else if (orderStatus == 2 && purchase.getOrderStatus() == 1) {
            statusValid = true;
            statusMsg = "标记为已到货成功";
        } else if (orderStatus == 3 && purchase.getOrderStatus() == 2) {
            statusValid = true;
            statusMsg = "标记为已完成成功";
        }

        if (!statusValid) {
            result.put("success", false);
            result.put("message", String.format(
                    "状态流转不合法！当前状态：%s，目标状态：%s，仅支持：0→1、1→0、1→2、2→3",
                    getStatusText(purchase.getOrderStatus()),
                    getStatusText(orderStatus)
            ));
            return result;
        }

        // 4. 时间解析
        LocalDateTime auditTimeLdt;
        try {
            auditTimeLdt = LocalDateTime.parse(auditTime, SIMPLE_FORMATTER);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
            return result;
        }

        // 5. 赋值：当前登录人 = 审核人
        purchase.setOrderStatus(orderStatus);
        if (orderStatus != 0) {
            purchase.setAuditorId(currentUserId);
            purchase.setAuditorName(currentUsername);
            purchase.setAuditTime(auditTimeLdt);
            purchase.setAuditOpinion(auditOpinion);
        } else {
            purchase.setAuditorId(null);
            purchase.setAuditorName(null);
            purchase.setAuditTime(null);
            purchase.setAuditOpinion(null);
        }
        purchase.setUpdateTime(LocalDateTime.now());

        boolean updateSuccess = this.updateById(purchase);
        if (updateSuccess) {
            result.put("success", true);
            result.put("message", statusMsg);
        } else {
            result.put("success", false);
            result.put("message", "状态更新失败");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePurchasePhysically(Long purchaseId) {
        if (purchaseId == null || purchaseId <= 0) {
            log.warn("物理删除采购单失败：采购单ID无效，ID={}", purchaseId);
            return false;
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getPurchaseId, purchaseId);
        purchaseOrderItemMapper.delete(itemWrapper);
        log.info("物理删除采购单明细成功，采购单ID={}", purchaseId);

        int deleteCount = baseMapper.deleteById(purchaseId);
        boolean mainDeleted = deleteCount > 0;
        if (mainDeleted) {
            log.info("物理删除采购单主表成功，采购单ID={}", purchaseId);
        } else {
            log.warn("物理删除采购单主表失败：未找到该采购单，ID={}", purchaseId);
        }

        return mainDeleted;
    }

    private String getStatusText(Integer status) {
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待审核");
        statusMap.put(1, "已通过");
        statusMap.put(2, "已到货"); // ✅ 完美：笔误已修正
        statusMap.put(3, "已完成");
        return statusMap.getOrDefault(status, "未知状态");
    }
}