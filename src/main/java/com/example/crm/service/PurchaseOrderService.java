package com.example.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.entity.PurchaseOrder;
import com.example.crm.entity.PurchaseOrderItem;
import java.util.List;
import java.util.Map;

public interface PurchaseOrderService extends IService<PurchaseOrder> {
    IPage<PurchaseOrder> queryPurchasePage(Page<PurchaseOrder> page, Map<String, Object> params);
    PurchaseOrder getPurchaseDetail(Long purchaseId);
    boolean addPurchase(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items);
    boolean editPurchase(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items);
    Map<String, Object> updatePurchaseStatus(Long purchaseId, Integer orderStatus, String auditTime, String auditOpinion);

    boolean deletePurchasePhysically(Long purchaseId);
}