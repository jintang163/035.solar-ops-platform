package com.solar.ops.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.entity.StationHealth;
import com.solar.ops.analysis.mapper.StationHealthMapper;
import com.solar.ops.analysis.util.PrCalculator;
import com.solar.ops.analysis.vo.HealthAssessmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthAssessmentService extends ServiceImpl<StationHealthMapper, StationHealth> {

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    public HealthAssessmentVO assessStationHealth(Long stationId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsType, 1)
                .eq(EfficiencyStatistics::getStatisticsDate, today)
                .isNull(EfficiencyStatistics::getInverterId)
                .orderByDesc(EfficiencyStatistics::getCreateTime)
                .last("LIMIT 1");
        EfficiencyStatistics statistics = efficiencyAnalysisService.getOne(wrapper);

        BigDecimal prValue = statistics != null ? statistics.getPrValue() : BigDecimal.ZERO;
        int healthLevel = PrCalculator.getHealthLevel(prValue);

        int faultCount = 0;
        BigDecimal efficiencyScore = PrCalculator.calculateEfficiencyScore(prValue, faultCount);

        StationHealth health = new StationHealth();
        health.setStationId(stationId);
        health.setHealthLevel(healthLevel);
        health.setPrValue(prValue);
        health.setFaultCount(faultCount);
        health.setEfficiencyScore(efficiencyScore);
        health.setAssessmentTime(LocalDateTime.now());
        save(health);

        return convertToVO(health);
    }

    public List<HealthAssessmentVO> getStationHealthList(List<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<StationHealth> healthList = new ArrayList<>();
        for (Long stationId : stationIds) {
            LambdaQueryWrapper<StationHealth> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StationHealth::getStationId, stationId)
                    .orderByDesc(StationHealth::getAssessmentTime)
                    .last("LIMIT 1");
            StationHealth health = getOne(wrapper);
            if (health != null) {
                healthList.add(health);
            }
        }
        return healthList.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public StationHealth getLatestHealth(Long stationId) {
        LambdaQueryWrapper<StationHealth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StationHealth::getStationId, stationId)
                .orderByDesc(StationHealth::getAssessmentTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    public List<StationHealth> getHealthHistory(Long stationId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<StationHealth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StationHealth::getStationId, stationId)
                .between(StationHealth::getAssessmentTime, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .orderByAsc(StationHealth::getAssessmentTime);
        return list(wrapper);
    }

    public void batchAssessHealth(List<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return;
        }
        for (Long stationId : stationIds) {
            assessStationHealth(stationId);
        }
    }

    private HealthAssessmentVO convertToVO(StationHealth health) {
        HealthAssessmentVO vo = new HealthAssessmentVO();
        vo.setStationId(health.getStationId());
        vo.setHealthLevel(health.getHealthLevel());
        vo.setHealthLevelDesc(PrCalculator.getHealthLevelDesc(health.getHealthLevel()));
        vo.setHealthColor(PrCalculator.getHealthColor(health.getHealthLevel()));
        vo.setPrValue(health.getPrValue());
        vo.setFaultCount(health.getFaultCount());
        vo.setEfficiencyScore(health.getEfficiencyScore());
        vo.setAssessmentTime(health.getAssessmentTime());
        return vo;
    }
}
