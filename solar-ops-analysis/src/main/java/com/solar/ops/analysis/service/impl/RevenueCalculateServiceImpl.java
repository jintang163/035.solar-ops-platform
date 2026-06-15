package com.solar.ops.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.analysis.entity.CleaningPlan;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import com.solar.ops.analysis.entity.RevenueStatistics;
import com.solar.ops.analysis.enums.RevenueStatisticsTypeEnum;
import com.solar.ops.analysis.mapper.CleaningPlanMapper;
import com.solar.ops.analysis.mapper.EfficiencyStatisticsMapper;
import com.solar.ops.analysis.mapper.RevenueStatisticsMapper;
import com.solar.ops.analysis.service.ElectricityPriceService;
import com.solar.ops.analysis.service.RevenueCalculateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RevenueCalculateServiceImpl implements RevenueCalculateService {

    private static final Logger log = LoggerFactory.getLogger(RevenueCalculateServiceImpl.class);

    @Autowired
    private EfficiencyStatisticsMapper efficiencyStatisticsMapper;

    @Autowired
    private ElectricityPriceService electricityPriceService;

    @Autowired
    private RevenueStatisticsMapper revenueStatisticsMapper;

    @Autowired
    private CleaningPlanMapper cleaningPlanMapper;

    @Autowired
    private StationMapper stationMapper;

    @Override
    public RevenueStatistics calculateDailyRevenue(Long stationId, LocalDate date) {
        log.info("开始计算电站[{}]在[{}]的日收益", stationId, date);

        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsDate, date)
                .eq(EfficiencyStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                .isNull(EfficiencyStatistics::getInverterId);

        List<EfficiencyStatistics> efficiencyStats = efficiencyStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(efficiencyStats)) {
            log.warn("电站[{}]在[{}]没有效率统计数据", stationId, date);
            return null;
        }

        EfficiencyStatistics efficiency = efficiencyStats.get(0);
        BigDecimal gridEnergy = efficiency.getTotalEnergy() != null
                ? efficiency.getTotalEnergy() : BigDecimal.ZERO;

        ElectricityPriceScheme scheme = electricityPriceService.getDefaultScheme(stationId);
        if (scheme == null) {
            log.warn("电站[{}]没有找到默认电价方案", stationId);
            return null;
        }

        BigDecimal effectivePrice = electricityPriceService.calculateEffectivePrice(scheme.getId(), date);

        BigDecimal gridRevenue = gridEnergy.multiply(scheme.getGridPrice() != null
                ? scheme.getGridPrice() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal nationalSubsidyRevenue = BigDecimal.ZERO;
        BigDecimal provincialSubsidyRevenue = BigDecimal.ZERO;
        BigDecimal municipalSubsidyRevenue = BigDecimal.ZERO;

        boolean inSubsidyPeriod = isInSubsidyPeriod(scheme, date);
        if (inSubsidyPeriod) {
            if (scheme.getNationalSubsidy() != null) {
                nationalSubsidyRevenue = gridEnergy.multiply(scheme.getNationalSubsidy())
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (scheme.getProvincialSubsidy() != null) {
                provincialSubsidyRevenue = gridEnergy.multiply(scheme.getProvincialSubsidy())
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (scheme.getMunicipalSubsidy() != null) {
                municipalSubsidyRevenue = gridEnergy.multiply(scheme.getMunicipalSubsidy())
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalSubsidyRevenue = nationalSubsidyRevenue
                .add(provincialSubsidyRevenue)
                .add(municipalSubsidyRevenue);

        BigDecimal totalRevenue = gridRevenue.add(totalSubsidyRevenue);

        BigDecimal operationCost = getDailyOperationCost(stationId, date);
        BigDecimal unitEnergyCost = calculateUnitCost(stationId, gridEnergy);

        RevenueStatistics revenue = new RevenueStatistics();
        revenue.setStationId(stationId);
        revenue.setPriceSchemeId(scheme.getId());
        revenue.setStatisticsDate(date);
        revenue.setStatisticsType(RevenueStatisticsTypeEnum.DAY.getCode());
        revenue.setGridEnergy(gridEnergy);
        revenue.setGridRevenue(gridRevenue);
        revenue.setNationalSubsidyRevenue(nationalSubsidyRevenue);
        revenue.setProvincialSubsidyRevenue(provincialSubsidyRevenue);
        revenue.setMunicipalSubsidyRevenue(municipalSubsidyRevenue);
        revenue.setTotalSubsidyRevenue(totalSubsidyRevenue);
        revenue.setTotalRevenue(totalRevenue);
        revenue.setUnitEnergyCost(unitEnergyCost);
        revenue.setOperationCost(operationCost);
        revenue.setEffectiveGridPrice(effectivePrice);
        revenue.setSettlementStatus(0);

        LambdaQueryWrapper<RevenueStatistics> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsDate, date)
                .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        RevenueStatistics existing = revenueStatisticsMapper.selectOne(existWrapper);

        if (existing != null) {
            revenue.setId(existing.getId());
            revenueStatisticsMapper.updateById(revenue);
        } else {
            revenueStatisticsMapper.insert(revenue);
        }

        log.info("电站[{}]在[{}]的日收益计算完成，总收益：{}元", stationId, date, totalRevenue);
        return revenue;
    }

    @Override
    public List<RevenueStatistics> calculateMonthlyRevenue(Long stationId, Integer year, Integer month) {
        log.info("开始汇总电站[{}]在[{}年{}月]的月收益", stationId, year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                .between(RevenueStatistics::getStatisticsDate, monthStart, monthEnd);
        List<RevenueStatistics> dailyStats = revenueStatisticsMapper.selectList(dailyWrapper);

        if (CollectionUtils.isEmpty(dailyStats)) {
            log.warn("电站[{}]在[{}年{}月]没有日收益数据，跳过月度汇总", stationId, year, month);
            return Collections.emptyList();
        }

        BigDecimal totalGridEnergy = dailyStats.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGridRevenue = dailyStats.stream()
                .map(s -> s.getGridRevenue() != null ? s.getGridRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNationalSubsidy = dailyStats.stream()
                .map(s -> s.getNationalSubsidyRevenue() != null ? s.getNationalSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProvincialSubsidy = dailyStats.stream()
                .map(s -> s.getProvincialSubsidyRevenue() != null ? s.getProvincialSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMunicipalSubsidy = dailyStats.stream()
                .map(s -> s.getMunicipalSubsidyRevenue() != null ? s.getMunicipalSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSubsidy = totalNationalSubsidy.add(totalProvincialSubsidy).add(totalMunicipalSubsidy);
        BigDecimal totalRevenue = totalGridRevenue.add(totalSubsidy);

        BigDecimal avgPrice = totalGridEnergy.compareTo(BigDecimal.ZERO) > 0
                ? totalRevenue.divide(totalGridEnergy, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalOperationCost = dailyStats.stream()
                .map(s -> s.getOperationCost() != null ? s.getOperationCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unitCost = calculateUnitCost(stationId, totalGridEnergy);

        ElectricityPriceScheme scheme = electricityPriceService.getDefaultScheme(stationId);

        RevenueStatistics monthlyStats = new RevenueStatistics();
        monthlyStats.setStationId(stationId);
        monthlyStats.setPriceSchemeId(scheme != null ? scheme.getId() : null);
        monthlyStats.setStatisticsDate(monthStart);
        monthlyStats.setStatisticsType(RevenueStatisticsTypeEnum.MONTH.getCode());
        monthlyStats.setGridEnergy(totalGridEnergy);
        monthlyStats.setGridRevenue(totalGridRevenue);
        monthlyStats.setNationalSubsidyRevenue(totalNationalSubsidy);
        monthlyStats.setProvincialSubsidyRevenue(totalProvincialSubsidy);
        monthlyStats.setMunicipalSubsidyRevenue(totalMunicipalSubsidy);
        monthlyStats.setTotalSubsidyRevenue(totalSubsidy);
        monthlyStats.setTotalRevenue(totalRevenue);
        monthlyStats.setUnitEnergyCost(unitCost);
        monthlyStats.setOperationCost(totalOperationCost);
        monthlyStats.setEffectiveGridPrice(avgPrice);
        monthlyStats.setSettlementStatus(0);

        LambdaQueryWrapper<RevenueStatistics> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsDate, monthStart)
                .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode());
        RevenueStatistics existing = revenueStatisticsMapper.selectOne(existWrapper);

        if (existing != null) {
            monthlyStats.setId(existing.getId());
            revenueStatisticsMapper.updateById(monthlyStats);
        } else {
            revenueStatisticsMapper.insert(monthlyStats);
        }

        log.info("电站[{}]在[{}年{}月]的月收益汇总完成，总收益：{}元", stationId, year, month, totalRevenue);
        return dailyStats;
    }

    @Override
    public RevenueStatistics calculateYearlyRevenue(Long stationId, Integer year) {
        log.info("开始计算电站[{}]在[{}年]的年收益", stationId, year);

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode())
                .between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
        List<RevenueStatistics> monthlyStats = revenueStatisticsMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(monthlyStats)) {
            LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
            dailyWrapper.eq(RevenueStatistics::getStationId, stationId)
                    .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                    .between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
            monthlyStats = revenueStatisticsMapper.selectList(dailyWrapper);
        }

        if (CollectionUtils.isEmpty(monthlyStats)) {
            log.warn("电站[{}]在[{}年]没有收益数据", stationId, year);
            return null;
        }

        BigDecimal totalGridEnergy = monthlyStats.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGridRevenue = monthlyStats.stream()
                .map(s -> s.getGridRevenue() != null ? s.getGridRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNationalSubsidy = monthlyStats.stream()
                .map(s -> s.getNationalSubsidyRevenue() != null ? s.getNationalSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProvincialSubsidy = monthlyStats.stream()
                .map(s -> s.getProvincialSubsidyRevenue() != null ? s.getProvincialSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMunicipalSubsidy = monthlyStats.stream()
                .map(s -> s.getMunicipalSubsidyRevenue() != null ? s.getMunicipalSubsidyRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSubsidy = totalNationalSubsidy.add(totalProvincialSubsidy).add(totalMunicipalSubsidy);
        BigDecimal totalRevenue = totalGridRevenue.add(totalSubsidy);
        BigDecimal totalOperationCost = monthlyStats.stream()
                .map(s -> s.getOperationCost() != null ? s.getOperationCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPrice = totalGridEnergy.compareTo(BigDecimal.ZERO) > 0
                ? totalRevenue.divide(totalGridEnergy, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal unitCost = calculateUnitCost(stationId, totalGridEnergy);

        ElectricityPriceScheme scheme = electricityPriceService.getDefaultScheme(stationId);

        RevenueStatistics yearlyStats = new RevenueStatistics();
        yearlyStats.setStationId(stationId);
        yearlyStats.setPriceSchemeId(scheme != null ? scheme.getId() : null);
        yearlyStats.setStatisticsDate(yearStart);
        yearlyStats.setStatisticsType(RevenueStatisticsTypeEnum.YEAR.getCode());
        yearlyStats.setGridEnergy(totalGridEnergy);
        yearlyStats.setGridRevenue(totalGridRevenue);
        yearlyStats.setNationalSubsidyRevenue(totalNationalSubsidy);
        yearlyStats.setProvincialSubsidyRevenue(totalProvincialSubsidy);
        yearlyStats.setMunicipalSubsidyRevenue(totalMunicipalSubsidy);
        yearlyStats.setTotalSubsidyRevenue(totalSubsidy);
        yearlyStats.setTotalRevenue(totalRevenue);
        yearlyStats.setUnitEnergyCost(unitCost);
        yearlyStats.setOperationCost(totalOperationCost);
        yearlyStats.setEffectiveGridPrice(avgPrice);
        yearlyStats.setSettlementStatus(0);

        LambdaQueryWrapper<RevenueStatistics> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsDate, yearStart)
                .eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.YEAR.getCode());
        RevenueStatistics existing = revenueStatisticsMapper.selectOne(existWrapper);
        if (existing != null) {
            yearlyStats.setId(existing.getId());
            revenueStatisticsMapper.updateById(yearlyStats);
        } else {
            revenueStatisticsMapper.insert(yearlyStats);
        }

        log.info("电站[{}]在[{}年]的年收益计算完成，总收益：{}元", stationId, year, totalRevenue);
        return yearlyStats;
    }

    @Override
    public void calculateAllStationsDailyRevenue(LocalDate date) {
        log.info("开始计算所有电站在[{}]的日收益", date);

        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStatisticsDate, date)
                .eq(EfficiencyStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                .isNull(EfficiencyStatistics::getInverterId)
                .groupBy(EfficiencyStatistics::getStationId);

        List<EfficiencyStatistics> stationList = efficiencyStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(stationList)) {
            log.warn("在[{}]没有电站的效率统计数据", date);
            return;
        }

        int successCount = 0;
        for (EfficiencyStatistics stat : stationList) {
            try {
                RevenueStatistics revenue = calculateDailyRevenue(stat.getStationId(), date);
                if (revenue != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("计算电站[{}]在[{}]的日收益失败", stat.getStationId(), date, e);
            }
        }

        log.info("所有电站在[{}]的日收益计算完成，成功计算{}个电站", date, successCount);
    }

    @Override
    public void calculateAllStationsMonthlyRevenue(Integer year, Integer month) {
        log.info("开始汇总所有电站在[{}年{}月]的月收益", year, month);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        YearMonth yearMonth = YearMonth.of(year, month);
        wrapper.between(RevenueStatistics::getStatisticsDate, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        wrapper.groupBy(RevenueStatistics::getStationId);

        List<RevenueStatistics> stationList = revenueStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(stationList)) {
            log.warn("在[{}年{}月]没有电站的日收益数据", year, month);
            return;
        }

        List<Long> stationIds = stationList.stream()
                .map(RevenueStatistics::getStationId)
                .distinct()
                .collect(Collectors.toList());

        int successCount = 0;
        for (Long stationId : stationIds) {
            try {
                calculateMonthlyRevenue(stationId, year, month);
                successCount++;
            } catch (Exception e) {
                log.error("汇总电站[{}]在[{}年{}月]的月收益失败", stationId, year, month, e);
            }
        }

        log.info("所有电站在[{}年{}月]的月收益汇总完成，成功汇总{}个电站", year, month, successCount);
    }

    @Override
    public void calculateAllStationsYearlyRevenue(Integer year) {
        log.info("开始汇总所有电站在[{}年]的年收益", year);

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode());
        wrapper.between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
        wrapper.groupBy(RevenueStatistics::getStationId);

        List<RevenueStatistics> stationList = revenueStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(stationList)) {
            LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
            dailyWrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
            dailyWrapper.between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
            dailyWrapper.groupBy(RevenueStatistics::getStationId);
            stationList = revenueStatisticsMapper.selectList(dailyWrapper);
        }

        if (CollectionUtils.isEmpty(stationList)) {
            log.warn("在[{}年]没有电站的收益数据", year);
            return;
        }

        List<Long> stationIds = stationList.stream()
                .map(RevenueStatistics::getStationId)
                .distinct()
                .collect(Collectors.toList());

        int successCount = 0;
        for (Long stationId : stationIds) {
            try {
                calculateYearlyRevenue(stationId, year);
                successCount++;
            } catch (Exception e) {
                log.error("汇总电站[{}]在[{}年]的年收益失败", stationId, year, e);
            }
        }

        log.info("所有电站在[{}年]的年收益汇总完成，成功汇总{}个电站", year, successCount);
    }

    @Override
    public BigDecimal calculateUnitCost(Long stationId, BigDecimal energy) {
        if (energy == null || energy.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal operationCost = getStationPeriodOperationCost(stationId);
        return operationCost.divide(energy, 4, RoundingMode.HALF_UP);
    }

    private boolean isInSubsidyPeriod(ElectricityPriceScheme scheme, LocalDate date) {
        if (scheme.getSubsidyStartDate() != null && date.isBefore(scheme.getSubsidyStartDate())) {
            return false;
        }
        if (scheme.getSubsidyEndDate() != null && date.isAfter(scheme.getSubsidyEndDate())) {
            return false;
        }
        return true;
    }

    private BigDecimal getDailyOperationCost(Long stationId, LocalDate date) {
        BigDecimal dailyBaseCost = BigDecimal.ZERO;
        Station station = stationMapper.selectById(stationId);
        if (station != null && station.getCapacity() != null) {
            BigDecimal annualBase = station.getCapacity().multiply(new BigDecimal("50"));
            dailyBaseCost = annualBase.divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
        }

        LambdaQueryWrapper<CleaningPlan> cpWrapper = new LambdaQueryWrapper<>();
        cpWrapper.eq(CleaningPlan::getStationId, stationId)
                .eq(CleaningPlan::getStatus, 2)
                .between(CleaningPlan::getActualEndTime, date.atStartOfDay(), date.atTime(23, 59, 59));
        List<CleaningPlan> dayPlans = cleaningPlanMapper.selectList(cpWrapper);
        BigDecimal dayCleaningCost = dayPlans.stream()
                .map(p -> p.getCleaningCost() != null ? p.getCleaningCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return dailyBaseCost.add(dayCleaningCost);
    }

    private BigDecimal getStationPeriodOperationCost(Long stationId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LambdaQueryWrapper<CleaningPlan> cpWrapper = new LambdaQueryWrapper<>();
        cpWrapper.eq(CleaningPlan::getStationId, stationId)
                .eq(CleaningPlan::getStatus, 2)
                .ge(CleaningPlan::getActualEndTime, thirtyDaysAgo.atStartOfDay());
        List<CleaningPlan> plans = cleaningPlanMapper.selectList(cpWrapper);
        BigDecimal cleaningCost = plans.stream()
                .map(p -> p.getCleaningCost() != null ? p.getCleaningCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyBaseCost = BigDecimal.ZERO;
        Station station = stationMapper.selectById(stationId);
        if (station != null && station.getCapacity() != null) {
            BigDecimal annualBase = station.getCapacity().multiply(new BigDecimal("50"));
            dailyBaseCost = annualBase.divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
        }

        return cleaningCost.add(dailyBaseCost.multiply(new BigDecimal("30")));
    }
}
