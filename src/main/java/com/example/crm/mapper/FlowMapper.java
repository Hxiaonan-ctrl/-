package com.example.crm.mapper;

import com.example.crm.vo.RecentFlowVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 流向单Mapper接口
 */
public interface FlowMapper {
    // 查询最近流向单
    List<RecentFlowVO> getRecentFlow();
}