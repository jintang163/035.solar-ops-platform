package com.solar.ops.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.EfficiencyQueryDTO;
import com.solar.ops.analysis.dto.PeriodCompareDTO;
import com.solar.ops.analysis.dto.StationCompareQueryDTO;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.entity.RevenueStatistics;
import com.solar.ops.analysis.entity.StationHealth;
import com.solar.ops.analysis.excel.StationCompareExcelVO;
import com.solar.ops.analysis.mapper.EfficiencyStatisticsMapper;
import com.solar.ops.analysis.mapper.RevenueStatisticsMapper;
import com.solar.ops.analysis.mapper.StationHealthMapper;
import com.solar.ops.analysis.util.PrCalculator;
import com.solar.ops.analysis.vo.CompareSummaryVO;
import com.solar.ops.analysis.vo.EfficiencyRankVO;
import com.solar.ops.analysis.vo.StationCompareItemVO;
import com.solar.ops.analysis.vo.StationCompareVO;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.page.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EfficiencyAnalysisService extends ServiceImpl<EfficiencyStatisticsMapper, EfficiencyStatistics> {

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private InverterMapper inverterMapper;

    @Autowired
    private StationHealthMapper stationHealthMapper;

    @Autowired
    private RevenueStatisticsMapper revenueStatisticsMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public BigDecimal calculatePr(BigDecimal actualEnergy, BigDecimal installedCapacity, BigDecimal peakSunHours) {
        return PrCalculator.calculatePr(actualEnergy, installedCapacity, peakSunHours);
    }

    public PageResult<EfficiencyStatistics> queryEfficiencyList(EfficiencyQueryDTO queryDTO) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = buildQueryWrapper(queryDTO);
        Page<EfficiencyStatistics> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<EfficiencyStatistics> result = page(page, wrapper);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    public List<EfficiencyStatistics> queryStationEfficiency(Long stationId, Integer statisticsType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsType, statisticsType)
                .between(EfficiencyStatistics::getStatisticsDate, startDate, endDate)
                .orderByAsc(EfficiencyStatistics::getStatisticsDate);
        return list(wrapper);
    }

    public List<EfficiencyRankVO> getEfficiencyRank(Integer statisticsType, LocalDate date, int topN) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStatisticsType, statisticsType)
                .eq(EfficiencyStatistics::getStatisticsDate, date)
                .isNull(EfficiencyStatistics::getInverterId);
        List<EfficiencyStatistics> list = list(wrapper);

        return list.stream()
                .sorted(Comparator.comparing(EfficiencyStatistics::getPrValue).reversed())
                .limit(topN)
                .map(this::convertToRankVO)
                .collect(Collectors.toList());
    }

    public List<EfficiencyStatistics> getLowEfficiencyInverters(Long stationId, LocalDate date, BigDecimal prThreshold) {
        if (prThreshold == null) {
            prThreshold = new BigDecimal("0.75");
        }
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsDate, date)
                .eq(EfficiencyStatistics::getStatisticsType, 1)
                .isNotNull(EfficiencyStatistics::getInverterId)
                .lt(EfficiencyStatistics::getPrValue, prThreshold)
                .orderByAsc(EfficiencyStatistics::getPrValue);
        return list(wrapper);
    }

    public void calculateAndSaveHourly(Long stationId, Long inverterId, BigDecimal actualEnergy,
                                       BigDecimal installedCapacity, BigDecimal peakSunHours,
                                       BigDecimal totalEnergy) {
        BigDecimal prValue = PrCalculator.calculatePr(actualEnergy, installedCapacity, peakSunHours);
        BigDecimal systemEfficiency = PrCalculator.calculateSystemEfficiency(actualEnergy, installedCapacity, peakSunHours);
        BigDecimal equivalentHours = PrCalculator.calculateEquivalentHours(totalEnergy, installedCapacity);

        EfficiencyStatistics statistics = new EfficiencyStatistics();
        statistics.setStationId(stationId);
        statistics.setInverterId(inverterId);
        statistics.setStatisticsDate(LocalDate.now());
        statistics.setStatisticsType(1);
        statistics.setPrValue(prValue);
        statistics.setSystemEfficiency(systemEfficiency);
        statistics.setEquivalentHours(equivalentHours);
        statistics.setTotalEnergy(totalEnergy);
        save(statistics);
    }

    public void calculateDailyStatistics(Long stationId, LocalDate date) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsDate, date)
                .eq(EfficiencyStatistics::getStatisticsType, 1)
                .isNull(EfficiencyStatistics::getInverterId);
        List<EfficiencyStatistics> hourlyList = list(wrapper);

        if (hourlyList.isEmpty()) {
            return;
        }

        BigDecimal totalEnergy = hourlyList.stream()
                .map(EfficiencyStatistics::getTotalEnergy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPr = hourlyList.stream()
                .map(EfficiencyStatistics::getPrValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(hourlyList.size()), 4, BigDecimal.ROUND_HALF_UP);

        BigDecimal avgEfficiency = hourlyList.stream()
                .map(EfficiencyStatistics::getSystemEfficiency)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(hourlyList.size()), 4, BigDecimal.ROUND_HALF_UP);

        EfficiencyStatistics dailyStats = new EfficiencyStatistics();
        dailyStats.setStationId(stationId);
        dailyStats.setStatisticsDate(date);
        dailyStats.setStatisticsType(1);
        dailyStats.setPrValue(avgPr);
        dailyStats.setSystemEfficiency(avgEfficiency);
        dailyStats.setTotalEnergy(totalEnergy);
        save(dailyStats);
    }

    private LambdaQueryWrapper<EfficiencyStatistics> buildQueryWrapper(EfficiencyQueryDTO queryDTO) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStationId() != null) {
            wrapper.eq(EfficiencyStatistics::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(EfficiencyStatistics::getInverterId, queryDTO.getInverterId());
        }
        if (queryDTO.getStatisticsType() != null) {
            wrapper.eq(EfficiencyStatistics::getStatisticsType, queryDTO.getStatisticsType());
        }
        if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            wrapper.between(EfficiencyStatistics::getStatisticsDate, queryDTO.getStartDate(), queryDTO.getEndDate());
        }
        wrapper.orderByDesc(EfficiencyStatistics::getStatisticsDate);
        return wrapper;
    }

    private EfficiencyRankVO convertToRankVO(EfficiencyStatistics statistics) {
        EfficiencyRankVO vo = new EfficiencyRankVO();
        vo.setStationId(statistics.getStationId());
        vo.setPrValue(statistics.getPrValue());
        vo.setSystemEfficiency(statistics.getSystemEfficiency());
        vo.setEquivalentHours(statistics.getEquivalentHours());
        vo.setTotalEnergy(statistics.getTotalEnergy());
        return vo;
    }

    public StationCompareVO compareStations(StationCompareQueryDTO dto) {
        StationCompareVO result = new StationCompareVO();
        List<StationCompareItemVO> stationMetrics = new ArrayList<>();

        Integer statisticsType = dto.getStatisticsType() != null ? dto.getStatisticsType() : 3;
        List<Long> stationIds = dto.getStationIds();

        Map<Long, Station> stationMap = getStationMap(stationIds);
        Map<Long, Integer> inverterCountMap = getInverterCountMap(stationIds);

        if (!CollectionUtils.isEmpty(dto.getPeriods())) {
            for (Long stationId : stationIds) {
                Station station = stationMap.get(stationId);
                for (PeriodCompareDTO period : dto.getPeriods()) {
                    StationCompareItemVO item = buildStationCompareItem(
                            stationId, station, period.getLabel(),
                            period.getStartTime(), period.getEndTime(),
                            statisticsType, inverterCountMap
                    );
                    stationMetrics.add(item);
                }
            }
        } else {
            LocalDate startTime = dto.getStartTime();
            LocalDate endTime = dto.getEndTime();
            for (Long stationId : stationIds) {
                Station station = stationMap.get(stationId);
                StationCompareItemVO item = buildStationCompareItem(
                        stationId, station, null,
                        startTime, endTime,
                        statisticsType, inverterCountMap
                );
                stationMetrics.add(item);
            }
        }

        result.setStationMetrics(stationMetrics);
        result.setCompareSummary(buildCompareSummary(stationMetrics));
        return result;
    }

    public List<StationCompareExcelVO> exportCompare(StationCompareQueryDTO dto) {
        StationCompareVO compareVO = compareStations(dto);
        List<StationCompareItemVO> items = compareVO.getStationMetrics();

        Map<String, BigDecimal> avgMap = calculateAverageMetrics(items);

        return items.stream().map(item -> convertToExcelVO(item, avgMap)).collect(Collectors.toList());
    }

    private StationCompareItemVO buildStationCompareItem(Long stationId, Station station, String periodLabel,
                                                         LocalDate startTime, LocalDate endTime,
                                                         Integer statisticsType, Map<Long, Integer> inverterCountMap) {
        StationCompareItemVO vo = new StationCompareItemVO();
        vo.setStationId(stationId);
        vo.setStationName(station != null ? station.getStationName() : "未知电站");
        vo.setPeriodLabel(periodLabel);
        vo.setCapacity(station != null ? station.getCapacity() : BigDecimal.ZERO);

        fillEfficiencyMetrics(vo, stationId, startTime, endTime, statisticsType);
        vo.setFaultRate(queryFaultRate(stationId, startTime, endTime));
        fillHealthMetrics(vo, stationId);

        Integer inverterCount = inverterCountMap.getOrDefault(stationId, 0);
        vo.setInverterCount(inverterCount);
        vo.setOnlineRate(calculateOnlineRate(stationId, inverterCount));
        vo.setRevenue(queryRevenue(stationId, startTime, endTime, statisticsType));

        return vo;
    }

    private void fillEfficiencyMetrics(StationCompareItemVO vo, Long stationId,
                                       LocalDate startTime, LocalDate endTime, Integer statisticsType) {
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsType, statisticsType)
                .isNull(EfficiencyStatistics::getInverterId);

        if (startTime != null && endTime != null) {
            wrapper.between(EfficiencyStatistics::getStatisticsDate, startTime, endTime);
        }

        List<EfficiencyStatistics> list = list(wrapper);

        if (CollectionUtils.isEmpty(list)) {
            vo.setPrValue(BigDecimal.ZERO);
            vo.setSystemEfficiency(BigDecimal.ZERO);
            vo.setEquivalentHours(BigDecimal.ZERO);
            vo.setTotalEnergy(BigDecimal.ZERO);
            return;
        }

        BigDecimal totalPr = list.stream()
                .map(EfficiencyStatistics::getPrValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEfficiency = list.stream()
                .map(EfficiencyStatistics::getSystemEfficiency)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEquivalentHours = list.stream()
                .map(EfficiencyStatistics::getEquivalentHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEnergy = list.stream()
                .map(EfficiencyStatistics::getTotalEnergy)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int size = list.size();
        vo.setPrValue(size > 0 ? totalPr.divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setSystemEfficiency(size > 0 ? totalEfficiency.divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setEquivalentHours(totalEquivalentHours);
        vo.setTotalEnergy(totalEnergy);
    }

    public BigDecimal queryFaultRate(Long stationId, LocalDate startTime, LocalDate endTime) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) FROM work_order WHERE station_id = ? AND deleted = 0");

            List<Object> params = new ArrayList<>();
            params.add(stationId);

            if (startTime != null) {
                sql.append(" AND create_time >= ?");
                params.add(LocalDateTime.of(startTime, LocalDateTime.MIN.toLocalTime()));
            }
            if (endTime != null) {
                sql.append(" AND create_time <= ?");
                params.add(LocalDateTime.of(endTime, LocalDateTime.MAX.toLocalTime()));
            }

            Integer workOrderCount = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());

            LambdaQueryWrapper<Inverter> invWrapper = new LambdaQueryWrapper<>();
            invWrapper.eq(Inverter::getStationId, stationId);
            Integer deviceCount = Math.toIntExact(inverterMapper.selectCount(invWrapper));

            if (deviceCount == 0) {
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf(workOrderCount)
                    .divide(BigDecimal.valueOf(deviceCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void fillHealthMetrics(StationCompareItemVO vo, Long stationId) {
        LambdaQueryWrapper<StationHealth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StationHealth::getStationId, stationId)
                .orderByDesc(StationHealth::getAssessmentTime)
                .last("LIMIT 1");

        StationHealth health = stationHealthMapper.selectOne(wrapper);
        if (health != null && health.getEfficiencyScore() != null) {
            vo.setHealthScore(health.getEfficiencyScore());
        } else {
            vo.setHealthScore(new BigDecimal("85.00"));
        }
    }

    private BigDecimal calculateOnlineRate(Long stationId, Integer inverterCount) {
        if (inverterCount == null || inverterCount == 0) {
            return new BigDecimal("95.00");
        }
        LambdaQueryWrapper<Inverter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inverter::getStationId, stationId)
                .eq(Inverter::getOnlineStatus, 1);
        Long onlineCount = inverterMapper.selectCount(wrapper);

        return BigDecimal.valueOf(onlineCount)
                .divide(BigDecimal.valueOf(inverterCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal queryRevenue(Long stationId, LocalDate startTime, LocalDate endTime, Integer statisticsType) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId)
                .eq(RevenueStatistics::getStatisticsType, statisticsType);

        if (startTime != null && endTime != null) {
            wrapper.between(RevenueStatistics::getStatisticsDate, startTime, endTime);
        }

        List<RevenueStatistics> list = revenueStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BigDecimal.ZERO;
        }

        return list.stream()
                .map(RevenueStatistics::getTotalRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, Station> getStationMap(List<Long> stationIds) {
        if (CollectionUtils.isEmpty(stationIds)) {
            return Collections.emptyMap();
        }
        List<Station> stations = stationMapper.selectBatchIds(stationIds);
        return stations.stream().collect(Collectors.toMap(Station::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, Integer> getInverterCountMap(List<Long> stationIds) {
        if (CollectionUtils.isEmpty(stationIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Inverter> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Inverter::getStationId, stationIds);
        List<Inverter> inverters = inverterMapper.selectList(wrapper);
        return inverters.stream()
                .collect(Collectors.groupingBy(Inverter::getStationId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private CompareSummaryVO buildCompareSummary(List<StationCompareItemVO> items) {
        CompareSummaryVO summary = new CompareSummaryVO();
        if (CollectionUtils.isEmpty(items)) {
            summary.setAvgPr(BigDecimal.ZERO);
            summary.setPrGap(BigDecimal.ZERO);
            summary.setRecommendations(Collections.emptyList());
            return summary;
        }

        List<StationCompareItemVO> validItems = items.stream()
                .filter(i -> i.getPrValue() != null && i.getPrValue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            summary.setAvgPr(BigDecimal.ZERO);
            summary.setPrGap(BigDecimal.ZERO);
            summary.setRecommendations(Collections.singletonList("暂无足够数据进行对比分析，请检查统计数据是否完整"));
            return summary;
        }

        StationCompareItemVO best = validItems.stream()
                .max(Comparator.comparing(StationCompareItemVO::getPrValue))
                .orElse(validItems.get(0));

        StationCompareItemVO worst = validItems.stream()
                .min(Comparator.comparing(StationCompareItemVO::getPrValue))
                .orElse(validItems.get(0));

        BigDecimal totalPr = validItems.stream()
                .map(StationCompareItemVO::getPrValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setBestStation(best.getStationName() + (best.getPeriodLabel() != null ? "(" + best.getPeriodLabel() + ")" : ""));
        summary.setWorstStation(worst.getStationName() + (worst.getPeriodLabel() != null ? "(" + worst.getPeriodLabel() + ")" : ""));
        summary.setAvgPr(totalPr.divide(BigDecimal.valueOf(validItems.size()), 4, RoundingMode.HALF_UP));
        summary.setPrGap(best.getPrValue().subtract(worst.getPrValue()));
        summary.setRecommendations(buildRecommendations(validItems, best, worst));

        return summary;
    }

    private List<String> buildRecommendations(List<StationCompareItemVO> items,
                                              StationCompareItemVO best, StationCompareItemVO worst) {
        List<String> recommendations = new ArrayList<>();

        BigDecimal prGap = best.getPrValue().subtract(worst.getPrValue());
        if (prGap.compareTo(new BigDecimal("0.05")) > 0) {
            recommendations.add("建议" + worst.getStationName() + "参照" + best.getStationName() + "的清洗方案和运维策略，PR差距达"
                    + prGap.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%");
        }

        for (StationCompareItemVO item : items) {
            if (item.getFaultRate() != null && item.getFaultRate().compareTo(new BigDecimal("10")) > 0) {
                recommendations.add(item.getStationName() +
                        (item.getPeriodLabel() != null ? "(" + item.getPeriodLabel() + ")" : "") +
                        "故障率偏高(" + item.getFaultRate().setScale(2, RoundingMode.HALF_UP) + "%)，建议排查设备老化情况");
            }
        }

        for (StationCompareItemVO item : items) {
            if (item.getHealthScore() != null && item.getHealthScore().compareTo(new BigDecimal("70")) < 0) {
                recommendations.add(item.getStationName() +
                        (item.getPeriodLabel() != null ? "(" + item.getPeriodLabel() + ")" : "") +
                        "健康度评分较低(" + item.getHealthScore().setScale(2, RoundingMode.HALF_UP) + "分)，建议进行全面设备巡检");
            }
        }

        for (StationCompareItemVO item : items) {
            if (item.getOnlineRate() != null && item.getOnlineRate().compareTo(new BigDecimal("90")) < 0) {
                recommendations.add(item.getStationName() +
                        (item.getPeriodLabel() != null ? "(" + item.getPeriodLabel() + ")" : "") +
                        "在线率偏低(" + item.getOnlineRate().setScale(2, RoundingMode.HALF_UP) + "%)，建议检查通信网络和设备供电");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("各电站运行状态良好，建议继续保持现有运维策略");
        }

        return recommendations;
    }

    private Map<String, BigDecimal> calculateAverageMetrics(List<StationCompareItemVO> items) {
        Map<String, BigDecimal> avgMap = new HashMap<>();
        if (CollectionUtils.isEmpty(items)) {
            return avgMap;
        }

        int size = items.size();
        avgMap.put("pr", items.stream().map(StationCompareItemVO::getPrValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP));
        avgMap.put("efficiency", items.stream().map(StationCompareItemVO::getSystemEfficiency).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP));
        avgMap.put("hours", items.stream().map(StationCompareItemVO::getEquivalentHours).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP));
        avgMap.put("energy", items.stream().map(StationCompareItemVO::getTotalEnergy).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP));
        avgMap.put("revenue", items.stream().map(StationCompareItemVO::getRevenue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP));

        return avgMap;
    }

    private StationCompareExcelVO convertToExcelVO(StationCompareItemVO item, Map<String, BigDecimal> avgMap) {
        StationCompareExcelVO vo = new StationCompareExcelVO();
        vo.setStationId(item.getStationId());
        vo.setStationName(item.getStationName());
        vo.setPeriodLabel(item.getPeriodLabel());
        vo.setCapacity(item.getCapacity());
        vo.setPrValue(item.getPrValue());
        vo.setPrDiffPercent(calcDiffPercent(item.getPrValue(), avgMap.get("pr")));
        vo.setSystemEfficiency(item.getSystemEfficiency());
        vo.setEfficiencyDiffPercent(calcDiffPercent(item.getSystemEfficiency(), avgMap.get("efficiency")));
        vo.setEquivalentHours(item.getEquivalentHours());
        vo.setHoursDiffPercent(calcDiffPercent(item.getEquivalentHours(), avgMap.get("hours")));
        vo.setTotalEnergy(item.getTotalEnergy());
        vo.setEnergyDiffPercent(calcDiffPercent(item.getTotalEnergy(), avgMap.get("energy")));
        vo.setFaultRate(item.getFaultRate());
        vo.setHealthScore(item.getHealthScore());
        vo.setInverterCount(item.getInverterCount());
        vo.setOnlineRate(item.getOnlineRate());
        vo.setRevenue(item.getRevenue());
        vo.setRevenueDiffPercent(calcDiffPercent(item.getRevenue(), avgMap.get("revenue")));
        return vo;
    }

    private BigDecimal calcDiffPercent(BigDecimal value, BigDecimal avg) {
        if (value == null || avg == null || avg.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.subtract(avg)
                .divide(avg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
