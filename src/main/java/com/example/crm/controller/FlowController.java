package com.example.crm.controller;

import com.example.crm.service.FlowService;
import com.example.crm.vo.AreaFlowVO;
import com.example.crm.vo.ChannelFlowVO;
import com.example.crm.vo.MonthFlowVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/flow")
@PreAuthorize("hasAnyRole('admin', 'finance')") // 仅管理员/财务可访问
public class FlowController {

    @Resource
    private FlowService flowService;

    /**
     * 区域流向统计
     */
    @GetMapping("/area")
    public List<AreaFlowVO> getAreaFlow(@RequestParam(required = false) String year) {
        return flowService.getAreaFlow(year);
    }

    /**
     * 渠道流向统计
     */
    @GetMapping("/channel")
    public List<ChannelFlowVO> getChannelFlow(@RequestParam(required = false) String year) {
        return flowService.getChannelFlow(year);
    }

    /**
     * 月度流向统计
     */
    @GetMapping("/month")
    public List<MonthFlowVO> getMonthFlow(@RequestParam(defaultValue = "#{T(java.time.Year).now().toString()}") String year) {
        return flowService.getMonthFlow(year);
    }
}