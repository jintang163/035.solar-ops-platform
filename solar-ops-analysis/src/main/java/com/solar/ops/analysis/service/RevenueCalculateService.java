package com.solar.ops.analysis.service;

import com.solar.ops.analysis.entity.RevenueStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RevenueCalculateService {

    RevenueStatistics calculateDailyRevenue(Long stationId, LocalDate date);

    List<RevenueStatistics> calculateMonthlyRevenue(Long stationId, Integer year, Integer month);

    void calculateAllStationsDailyRevenue(LocalDate date);

    BigDecimal calculateUnitCost(Long stationId, BigDecimal energy);
}
