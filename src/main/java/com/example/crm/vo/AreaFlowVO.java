package com.example.crm.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AreaFlowVO {
    private String areaName; // 区域名称（省份/城市）
    private Integer orderNum; // 流向订单数
    private Integer totalQty; // 流向总数量
    private BigDecimal totalAmount; // 流向总金额
}