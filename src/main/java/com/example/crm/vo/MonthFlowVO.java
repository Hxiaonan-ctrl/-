package com.example.crm.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MonthFlowVO {
    private String month; // 月份（yyyy-MM）
    private Integer orderNum; // 当月订单数
    private Integer totalQty; // 当月流向总数量
    private BigDecimal totalAmount; // 当月流向总金额
}