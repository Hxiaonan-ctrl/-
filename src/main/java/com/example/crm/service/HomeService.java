package com.example.crm.service;

import com.example.crm.vo.HomeStatVO;
import com.example.crm.vo.RecentFlowVO;

import java.util.List;

public interface HomeService {
    HomeStatVO getHomeStatData();
    List<RecentFlowVO> getRecentFlowList();
}