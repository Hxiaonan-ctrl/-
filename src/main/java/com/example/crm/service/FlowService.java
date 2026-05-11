package com.example.crm.service;

import com.example.crm.vo.AreaFlowVO;
import com.example.crm.vo.ChannelFlowVO;
import com.example.crm.vo.MonthFlowVO;
import com.example.crm.vo.RecentFlowVO;
import java.util.List;

/**
 * 流向单业务层接口
 * 定义流向单相关的业务操作规范，由FlowServiceImpl实现具体逻辑
 */
public interface FlowService {

    /**
     * 获取最近的流向单列表（已审核的出库单）
     * @return 最近流向单VO列表（最多返回10条）
     */
    List<RecentFlowVO> getRecentFlow();

    /**
     * （可选扩展）根据流向单号获取流向单详情
     * @param flowNo 流向单号（对应outbound_order表的outbound_id）
     * @return 流向单详情VO（可根据业务需求自定义DetailVO）
     */
    // RecentFlowDetailVO getFlowDetail(String flowNo);

    /**
     * （可选扩展）作废流向单（更新出库单审核状态/标记作废）
     * @param flowNo 流向单号
     * @return 操作结果（true-成功，false-失败）
     */
    // boolean invalidFlow(String flowNo);

    List<AreaFlowVO> getAreaFlow(String year);
    List<ChannelFlowVO> getChannelFlow(String year);
    List<MonthFlowVO> getMonthFlow(String year);
}