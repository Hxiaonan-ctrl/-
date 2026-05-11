package com.example.crm.service.impl;

import com.example.crm.mapper.FlowMapper;
import com.example.crm.mapper.OutboundOrderMapper;
import com.example.crm.vo.AreaFlowVO;
import com.example.crm.vo.ChannelFlowVO;
import com.example.crm.vo.MonthFlowVO;
import com.example.crm.vo.RecentFlowVO;
import com.example.crm.service.FlowService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private FlowMapper flowMapper;
    @Autowired
    private OutboundOrderMapper outboundOrderMapper;
    /**
     * 获取最近流向单
     */
    @Override
    public List<RecentFlowVO> getRecentFlow() {
        List<RecentFlowVO> flowList = flowMapper.getRecentFlow();

        // 1. 兜底处理：如果列表为null，返回空列表（避免前端接收到null）
        if (flowList == null) {
            return new ArrayList<>();
        }

        // 2. 字段空值兜底（核心修复类型不匹配问题）
        flowList.forEach(item -> {
            if (item == null) return; // 跳过null元素

            // 流向单号（Long类型）
            if (item.getFlowNo() == null) {
                item.setFlowNo(0L);
            }
            // 药品名称（String类型）
            if (item.getDrugName() == null) {
                item.setDrugName("未知药品");
            }
            // 药企名称（String类型）
            if (item.getCompanyName() == null) {
                item.setCompanyName("未知药企");
            }
            // 流向金额（BigDecimal类型，核心修复：不能赋值字符串"0盒"）
            if (item.getAmount() == null) {
                item.setAmount(BigDecimal.ZERO); // 金额兜底为0（BigDecimal类型）
            }
            // 流向数量（Integer类型，如果你想显示"盒"，前端做格式化）
            if (item.getFlowQty() == null) {
                item.setFlowQty(0);
            }
            // 创建时间（String类型）
            if (item.getCreateTime() == null) {
                item.setCreateTime("");
            }
            // 审核状态（String类型）
            if (item.getAuditStatus() == null) {
                item.setAuditStatus("未知状态");
            }
        });

        return flowList;
    }

    @Override
    public List<AreaFlowVO> getAreaFlow(String year) {
        return outboundOrderMapper.selectAreaFlow(year);
    }

    @Override
    public List<ChannelFlowVO> getChannelFlow(String year) {
        return outboundOrderMapper.selectChannelFlow(year);
    }

    @Override
    public List<MonthFlowVO> getMonthFlow(String year) {
        return outboundOrderMapper.selectMonthFlow(year);
    }
}