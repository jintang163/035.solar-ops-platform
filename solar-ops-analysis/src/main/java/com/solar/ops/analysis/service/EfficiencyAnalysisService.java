package com.solar.ops.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.EfficiencyQueryDTO;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.mapper.EfficiencyStatisticsMapper;
import com.solar.ops.analysis.util.PrCalculator;
import com.solar.ops.analysis.vo.EfficiencyRankVO;
import com.solar.ops.common.page.PageResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EfficiencyAnalysisService extends ServiceImpl<EfficiencyStatisticsMapper, EfficiencyStatistics> {

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
}
