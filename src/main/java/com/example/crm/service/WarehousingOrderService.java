package com.example.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crm.entity.WarehousingOrder;
import com.example.crm.entity.WarehousingOrderItem;

import java.util.List;
import java.util.Map;

public interface WarehousingOrderService extends IService<WarehousingOrder> {
    /** 分页查询入库单 */
    IPage<WarehousingOrder> queryWarehousingPage(Page<WarehousingOrder> page, Map<String, Object> params);

    /** 根据ID查询入库单（含明细） */
    WarehousingOrder getWarehousingDetail(Long warehousingId);

    /** 新增入库单（含明细） */
    boolean addWarehousing(WarehousingOrder warehousingOrder, List<WarehousingOrderItem> items);

    /** 编辑入库单（含明细） */
    boolean editWarehousing(WarehousingOrder warehousingOrder, List<WarehousingOrderItem> items);

    /** 更新入库单状态（与出库单风格统一） */
    Map<String, Object> updateWarehousingStatus(Long warehousingId, Integer auditStatus, String auditTime);

    /** 删除入库单（补充：业务完整性） */
    boolean deleteWarehousing(Long warehousingId);
}