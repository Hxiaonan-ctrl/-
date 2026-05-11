package com.example.crm.service.impl;

import cn.hutool.core.date.DateUtil;
import com.example.crm.mapper.DrugMapper;
import com.example.crm.mapper.OutboundOrderMapper;
import com.example.crm.service.HomeService;
import com.example.crm.vo.HomeStatVO;
import com.example.crm.vo.RecentFlowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private DrugMapper drugMapper;

    @Autowired
    private OutboundOrderMapper outboundOrderMapper;

    @Override
    public HomeStatVO getHomeStatData() {
        HomeStatVO vo = new HomeStatVO();

        vo.setTotalDrugNum(drugMapper.selectTotalDrugNum());
        vo.setTotalEnterpriseNum(outboundOrderMapper.selectTotalEnterpriseNum());
        vo.setPendingAuditNum(outboundOrderMapper.selectPendingAuditNum());

        String currentMonth = DateUtil.format(DateUtil.date(), "yyyy-MM");
        Double monthAmount = outboundOrderMapper.selectMonthFlowAmount(currentMonth);
        monthAmount = monthAmount == null ? 0.00 : monthAmount;

        String formatAmount;
        if (monthAmount >= 10000) {
            // 【核心修改】使用原生BigDecimal处理，避免Hutool重载歧义
            BigDecimal amount = BigDecimal.valueOf(monthAmount);
            BigDecimal tenThousand = BigDecimal.valueOf(10000);
            String wanAmount = amount.divide(tenThousand, 1, RoundingMode.HALF_UP).toString();
            formatAmount = "¥" + wanAmount + "万";
        } else {
            // 同样使用BigDecimal保留两位小数
            String yuanAmount = BigDecimal.valueOf(monthAmount).setScale(2, RoundingMode.HALF_UP).toString();
            formatAmount = "¥" + yuanAmount + "元";
        }
        vo.setMonthFlowAmount(formatAmount);

        return vo;
    }

    @Override
    public List<RecentFlowVO> getRecentFlowList() {
        return outboundOrderMapper.selectRecentFlowList();
    }
}