package com.example.crm.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChannelFlowVO {
    private String channelName; // 渠道名称（药企/药店）
    private Integer channelType; // 渠道类型（1-药企 2-药店）
    private Integer orderNum; // 流向订单数
    private Integer totalQty; // 流向总数量
    private BigDecimal totalAmount; // 流向总金额
}