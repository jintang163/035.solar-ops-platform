package com.solar.ops.analysis.service;

import com.solar.ops.analysis.dto.InvestmentInfoDTO;
import com.solar.ops.analysis.dto.RevenueQueryDTO;
import com.solar.ops.analysis.vo.RevenueDashboardVO;
import com.solar.ops.analysis.vo.RevenueStatisticsVO;
import com.solar.ops.analysis.vo.RevenueTrendVO;
import com.solar.ops.analysis.vo.StationRevenueRankVO;
import com.solar.ops.common.page.PageResult;

import java.math.BigDecimal;
import java.util.List;

public interface RevenueStatisticsService {

    PageResult<RevenueStatisticsVO> queryPage(RevenueQueryDTO query);

    List<RevenueStatisticsVO> queryList(RevenueQueryDTO query);

    RevenueDashboardVO getDashboard(Long stationId, InvestmentInfoDTO investment);

    List<RevenueTrendVO> getRevenueTrend(Long stationId, Integer type, Integer days);

    List<StationRevenueRankVO> getStationRank(Long stationId, Integer type);

    BigDecimal getTotalRevenue(RevenueQueryDTO query);
}
