package com.solar.ops.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.analysis.entity.CleaningPlan;
import com.solar.ops.analysis.entity.DustAccumulationRecord;
import com.solar.ops.analysis.enums.CleaningStatusEnum;
import com.solar.ops.analysis.enums.DustLevelEnum;
import com.solar.ops.analysis.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CleaningStatisticsService {

    @Autowired
    private CleaningPlanService cleaningPlanService;

    @Autowired
    private CleaningReminderService cleaningReminderService;

    @Autowired
    private DustAccumulationService dustAccumulationService;

    public CleaningDashboardVO getDashboardStatistics(Long stationId) {
        CleaningDashboardVO vo = new CleaningDashboardVO();

        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        LambdaQueryWrapper<CleaningPlan> allPlanWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            allPlanWrapper.eq(CleaningPlan::getStationId, stationId);
        }
        allPlanWrapper.eq(CleaningPlan::getStatus, CleaningStatusEnum.COMPLETED.getCode());
        List<CleaningPlan> allCompletedPlans = cleaningPlanService.list(allPlanWrapper);

        LambdaQueryWrapper<CleaningPlan> monthPlanWrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            monthPlanWrapper.eq(CleaningPlan::getStationId, stationId);
        }
        monthPlanWrapper.eq(CleaningPlan::getStatus, CleaningStatusEnum.COMPLETED.getCode())
                .between(CleaningPlan::getPlanDate, monthStart, monthEnd);
        List<CleaningPlan> monthCompletedPlans = cleaningPlanService.list(monthPlanWrapper);

        vo.setTotalCleaningCount(allCompletedPlans.size());
        vo.setMonthlyCleaningCount(monthCompletedPlans.size());

        Map<String, Integer> statusCount = cleaningPlanService.countPlansByStatus(stationId);
        vo.setPendingPlanCount(statusCount.getOrDefault(CleaningStatusEnum.PENDING.name(), 0));
        vo.setInProgressPlanCount(statusCount.getOrDefault(CleaningStatusEnum.IN_PROGRESS.name(), 0));

        vo.setUnhandledReminderCount((int) cleaningReminderService.countUnhandledReminders(stationId));

        BigDecimal totalImproved = allCompletedPlans.stream()
                .map(p -> p.getImprovedEnergy() != null ? p.getImprovedEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalImprovedEnergy(totalImproved);

        BigDecimal monthImproved = monthCompletedPlans.stream()
                .map(p -> p.getImprovedEnergy() != null ? p.getImprovedEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyImprovedEnergy(monthImproved);

        BigDecimal totalSaved = totalImproved.multiply(new BigDecimal("0.5"))
                .setScale(2, RoundingMode.HALF_UP);
        vo.setTotalSavedCost(totalSaved);

        BigDecimal monthCost = monthCompletedPlans.stream()
                .map(p -> p.getCleaningCost() != null ? p.getCleaningCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyCleaningCost(monthCost);

        long calculatedCount = allCompletedPlans.stream()
                .filter(p -> p.getImprovedEnergy() != null
                        && p.getImprovedEnergy().compareTo(BigDecimal.ZERO) > 0)
                .count();
        if (calculatedCount > 0 && totalImproved.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgBefore = allCompletedPlans.stream()
                    .filter(p -> p.getBeforeCleanEnergy() != null
                            && p.getBeforeCleanEnergy().compareTo(BigDecimal.ZERO) > 0)
                    .map(CleaningPlan::getBeforeCleanEnergy)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (avgBefore.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgImprove = totalImproved.divide(
                        new BigDecimal(calculatedCount), 4, RoundingMode.HALF_UP);
                BigDecimal avgRate = avgImprove.divide(
                        avgBefore.divide(new BigDecimal(calculatedCount), 4, RoundingMode.HALF_UP),
                        4, RoundingMode.HALF_UP);
                vo.setAverageImprovementRate(avgRate.multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setAverageImprovementRate(BigDecimal.ZERO);
            }
        } else {
            vo.setAverageImprovementRate(BigDecimal.ZERO);
        }

        vo.setDustLevelStats(getDustLevelStats(stationId));
        vo.setImprovementTrend(getImprovementTrend(stationId, now));
        vo.setStationRank(getStationCleaningRank(stationId));

        return vo;
    }

    public List<DustLevelStatVO> getDustLevelStats(Long stationId) {
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        LambdaQueryWrapper<DustAccumulationRecord> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(DustAccumulationRecord::getStationId, stationId);
        }
        wrapper.ge(DustAccumulationRecord::getDetectDate, weekAgo);
        wrapper.groupBy(DustAccumulationRecord::getDustLevel);
        wrapper.select(DustAccumulationRecord::getDustLevel,
                com.baomidou.mybatisplus.core.toolkit.Wrappers.count());

        List<Map<String, Object>> maps = dustAccumulationService.listMaps(wrapper);

        Map<Integer, Long> countMap = new HashMap<>();
        for (DustLevelEnum level : DustLevelEnum.values()) {
            countMap.put(level.getCode(), 0L);
        }

        for (Map<String, Object> map : maps) {
            Integer level = (Integer) map.get("dust_level");
            Long count = ((Number) map.getOrDefault("COUNT(*)", 0L)).longValue();
            if (level != null && countMap.containsKey(level)) {
                countMap.put(level, count);
            }
        }

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<DustLevelStatVO> result = new ArrayList<>();
        for (DustLevelEnum level : DustLevelEnum.values()) {
            Long count = countMap.get(level.getCode());
            double ratio = total > 0 ? (count.doubleValue() / total) : 0;
            result.add(new DustLevelStatVO(
                    level.getCode(),
                    level.getDesc(),
                    count.intValue(),
                    ratio,
                    level.getColor()
            ));
        }

        return result;
    }

    public List<CleaningTrendVO> getImprovementTrend(Long stationId, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(29);
        LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(CleaningPlan::getStationId, stationId);
        }
        wrapper.eq(CleaningPlan::getStatus, CleaningStatusEnum.COMPLETED.getCode())
                .between(CleaningPlan::getPlanDate, startDate, endDate);
        List<CleaningPlan> plans = cleaningPlanService.list(wrapper);

        Map<LocalDate, List<CleaningPlan>> groupedByDate = plans.stream()
                .collect(Collectors.groupingBy(CleaningPlan::getPlanDate));

        List<CleaningTrendVO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<CleaningPlan> dayPlans = groupedByDate.getOrDefault(date, Collections.emptyList());
            Integer count = dayPlans.size();
            BigDecimal improved = dayPlans.stream()
                    .map(p -> p.getImprovedEnergy() != null ? p.getImprovedEnergy() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new CleaningTrendVO(date, count, improved));
        }

        return result;
    }

    public List<StationCleaningRankVO> getStationCleaningRank(Long filterStationId) {
        LambdaQueryWrapper<CleaningPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningPlan::getStatus, CleaningStatusEnum.COMPLETED.getCode());
        if (filterStationId != null) {
            wrapper.eq(CleaningPlan::getStationId, filterStationId);
        }
        List<CleaningPlan> allPlans = cleaningPlanService.list(wrapper);

        if (CollectionUtils.isEmpty(allPlans)) {
            return Collections.emptyList();
        }

        Map<Long, List<CleaningPlan>> stationGrouped = allPlans.stream()
                .filter(p -> p.getStationId() != null)
                .collect(Collectors.groupingBy(CleaningPlan::getStationId));

        List<StationCleaningRankVO> rankList = new ArrayList<>();
        for (Map.Entry<Long, List<CleaningPlan>> entry : stationGrouped.entrySet()) {
            Long stationId = entry.getKey();
            List<CleaningPlan> stationPlans = entry.getValue();

            Integer count = stationPlans.size();
            BigDecimal improved = stationPlans.stream()
                    .map(p -> p.getImprovedEnergy() != null ? p.getImprovedEnergy() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgImproved = count > 0
                    ? improved.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            List<CleaningPlan> calculatedPlans = stationPlans.stream()
                    .filter(p -> p.getImprovedEnergy() != null
                            && p.getImprovedEnergy().compareTo(BigDecimal.ZERO) > 0
                            && p.getBeforeCleanEnergy() != null
                            && p.getBeforeCleanEnergy().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
            BigDecimal improvementRate = BigDecimal.ZERO;
            if (!calculatedPlans.isEmpty()) {
                BigDecimal totalBefore = calculatedPlans.stream()
                        .map(CleaningPlan::getBeforeCleanEnergy)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalImproved = calculatedPlans.stream()
                        .map(CleaningPlan::getImprovedEnergy)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalBefore.compareTo(BigDecimal.ZERO) > 0) {
                    improvementRate = totalImproved.divide(totalBefore, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }
            }

            String stationName = stationPlans.stream()
                    .map(CleaningPlan::getStationName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("电站" + stationId);

            StationCleaningRankVO vo = new StationCleaningRankVO();
            vo.setStationId(stationId);
            vo.setStationName(stationName);
            vo.setCleaningCount(count);
            vo.setImprovedEnergy(improved);
            vo.setAvgImprovedEnergy(avgImproved);
            vo.setImprovementRate(improvementRate);
            rankList.add(vo);
        }

        rankList.sort(Comparator
                .comparing(StationCleaningRankVO::getImprovedEnergy, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StationCleaningRankVO::getCleaningCount, Comparator.reverseOrder()));

        return rankList.size() > 10 ? rankList.subList(0, 10) : rankList;
    }
}
