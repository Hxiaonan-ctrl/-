package com.example.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.entity.OutboundOrder;
import com.example.crm.entity.OutboundOrderItem;

import java.util.List;
import java.util.Map;

/**
 * 出库单服务接口
 */
public interface OutboundOrderService extends IService<OutboundOrder> {

    /**
     * 分页查询出库单
     */
    IPage<OutboundOrder> queryOutboundPage(Page<OutboundOrder> page, Map<String, Object> params);

    /**
     * 根据ID查询出库单详情（含明细）
     */
    OutboundOrder getOutboundDetail(Long outboundId);

    /**
     * 新增出库单（含明细）
     */
    boolean addOutbound(OutboundOrder outboundOrder, List<OutboundOrderItem> items);

    /**
     * 编辑出库单（含明细）
     */
    boolean editOutbound(OutboundOrder outboundOrder, List<OutboundOrderItem> items);

    /**
     * 更新出库单状态（审核通过/驳回）
     */
    Map<String, Object> updateOutboundStatus(Long outboundId, Integer auditStatus, String auditOpinion);

    // ✅【新增】取消审核方法（必须加，否则实现类报错）
    Map<String, Object> cancelOutboundAudit(Long outboundId);
}