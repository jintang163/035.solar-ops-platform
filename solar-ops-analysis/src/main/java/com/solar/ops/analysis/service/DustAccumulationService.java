package com.solar.ops.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.DustRecordQueryDTO;
import com.solar.ops.analysis.entity.DustAccumulationRecord;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.enums.DustLevelEnum;
import com.solar.ops.analysis.mapper.DustAccumulationRecordMapper;
import com.solar.ops.analysis.util.DustAccumulationCalculator;
import com.solar.ops.analysis.vo.DustRecordVO;
import com.solar.ops.common.page.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DustAccumulationService extends ServiceImpl<DustAccumulationRecordMapper, DustAccumulationRecord> {

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    public PageResult<DustRecordVO> queryDustRecordPage(DustRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<DustAccumulationRecord> wrapper = buildDustQueryWrapper(queryDTO);
        Page<DustAccumulationRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<DustAccumulationRecord> result = page(page, wrapper);
        List<DustRecordVO> voList = result.getRecords().stream()
                .map(this::convertToDustVO)
                .collect(Collectors.toList());
        return PageResult.build(voList, result.getTotal());
    }

    public List<DustRecordVO> queryDustRecordList(DustRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<DustAccumulationRecord> wrapper = buildDustQueryWrapper(queryDTO);
        wrapper.orderByDesc(DustAccumulationRecord::getDetectDate);
        return list(wrapper).stream()
                .map(this::convertToDustVO)
                .collect(Collectors.toList());
    }

    public List<DustAccumulationRecord> detectDustAccumulation(Long stationId, LocalDate detectDate) {
        LambdaQueryWrapper<EfficiencyStatistics> effWrapper = new LambdaQueryWrapper<>();
        effWrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsType, 1)
                .between(EfficiencyStatistics::getStatisticsDate,
                        detectDate.minusDays(14), detectDate)
                .isNotNull(EfficiencyStatistics::getInverterId)
                .gt(EfficiencyStatistics::getTotalEnergy, BigDecimal.ZERO);
        List<EfficiencyStatistics> allStats = efficiencyAnalysisService.list(effWrapper);

        if (CollectionUtils.isEmpty(allStats)) {
            return new ArrayList<>();
        }

        java.util.Map<Long, List<EfficiencyStatistics>> inverterGrouped = allStats.stream()
                .collect(Collectors.groupingBy(EfficiencyStatistics::getInverterId));

        List<DustAccumulationRecord> resultList = new ArrayList<>();

        LocalDate referenceStart = detectDate.minusDays(7);
        LocalDate referenceEnd = detectDate.minusDays(1);

        for (java.util.Map.Entry<Long, List<EfficiencyStatistics>> entry : inverterGrouped.entrySet()) {
            Long inverterId = entry.getKey();
            List<EfficiencyStatistics> inverterStats = entry.getValue();

            EfficiencyStatistics detectStat = inverterStats.stream()
                    .filter(s -> s.getStatisticsDate().equals(detectDate))
                    .findFirst()
                    .orElse(null);

            if (detectStat == null || detectStat.getTotalEnergy() == null
                    || detectStat.getTotalEnergy().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal detectRatio = DustAccumulationCalculator.getRatioFromStat(detectStat);
            if (detectRatio.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal referenceRatio = DustAccumulationCalculator.calculateAverageRatio(
                    inverterStats, referenceStart, referenceEnd);

            if (referenceRatio.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal actualEnergy = detectStat.getTotalEnergy();
            BigDecimal theoreticalEnergy = DustAccumulationCalculator.calculateTheoreticalEnergy(
                    actualEnergy, detectRatio);

            BigDecimal attenuationRate = DustAccumulationCalculator.calculateAttenuationRate(
                    referenceRatio, detectRatio);
            DustLevelEnum dustLevel = DustAccumulationCalculator.determineDustLevel(attenuationRate);
            int continuousDays = DustAccumulationCalculator.calculateContinuousDeclineDays(
                    inverterStats, detectDate);
            BigDecimal estimatedLoss = DustAccumulationCalculator.calculateEstimatedLoss(
                    actualEnergy, attenuationRate);

            String arrayNumber = extractArrayNumber(inverterId, inverterStats);
            String inverterName = detectStat.getStationId() + "号站" + inverterId + "号逆变器";

            DustAccumulationRecord record = new DustAccumulationRecord();
            record.setStationId(stationId);
            record.setInverterId(inverterId);
            record.setInverterName(inverterName);
            record.setArrayNumber(arrayNumber);
            record.setDetectDate(detectDate);
            record.setReferenceDate(referenceEnd);
            record.setActualEnergy(actualEnergy);
            record.setTheoreticalEnergy(theoreticalEnergy);
            record.setReferenceRatio(referenceRatio);
            record.setDetectRatio(detectRatio);
            record.setAttenuationRate(attenuationRate);
            record.setEstimatedLossEnergy(estimatedLoss);
            record.setDustLevel(dustLevel.getCode());
            record.setContinuousDeclineDays(continuousDays);
            record.setHasReminder(0);
            record.setRemark("参考期" + referenceStart + "至" + referenceEnd + "平均比值");

            resultList.add(record);
        }

        if (!CollectionUtils.isEmpty(resultList)) {
            saveBatch(resultList);
        }

        return resultList;
    }

    private String extractArrayNumber(Long inverterId, List<EfficiencyStatistics> stats) {
        if (stats == null || stats.isEmpty()) {
            return inverterId + "号方阵";
        }
        return inverterId + "号方阵";
    }

    public List<DustAccumulationRecord> getRecordsNeedReminder(LocalDate date, int thresholdDays) {
        LambdaQueryWrapper<DustAccumulationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DustAccumulationRecord::getDetectDate, date)
                .eq(DustAccumulationRecord::getHasReminder, 0)
                .in(DustAccumulationRecord::getDustLevel,
                        DustLevelEnum.MODERATE.getCode(), DustLevelEnum.HEAVY.getCode());
        List<DustAccumulationRecord> heavyList = list(wrapper);

        if (thresholdDays > 0) {
            LambdaQueryWrapper<DustAccumulationRecord> lightWrapper = new LambdaQueryWrapper<>();
            lightWrapper.eq(DustAccumulationRecord::getDetectDate, date)
                    .eq(DustAccumulationRecord::getHasReminder, 0)
                    .eq(DustAccumulationRecord::getDustLevel, DustLevelEnum.LIGHT.getCode())
                    .ge(DustAccumulationRecord::getContinuousDeclineDays, thresholdDays);
            heavyList.addAll(list(lightWrapper));
        }

        return heavyList;
    }

    public void markAsHasReminder(Long recordId) {
        DustAccumulationRecord record = getById(recordId);
        if (record != null) {
            record.setHasReminder(1);
            updateById(record);
        }
    }

    private LambdaQueryWrapper<DustAccumulationRecord> buildDustQueryWrapper(DustRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<DustAccumulationRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStationId() != null) {
            wrapper.eq(DustAccumulationRecord::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(DustAccumulationRecord::getInverterId, queryDTO.getInverterId());
        }
        if (StringUtils.hasText(queryDTO.getArrayNumber())) {
            wrapper.like(DustAccumulationRecord::getArrayNumber, queryDTO.getArrayNumber());
        }
        if (queryDTO.getDustLevel() != null) {
            wrapper.eq(DustAccumulationRecord::getDustLevel, queryDTO.getDustLevel());
        }
        if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null) {
            wrapper.between(DustAccumulationRecord::getDetectDate,
                    queryDTO.getStartDate(), queryDTO.getEndDate());
        }
        if (queryDTO.getHasReminder() != null) {
            wrapper.eq(DustAccumulationRecord::getHasReminder, queryDTO.getHasReminder());
        }
        wrapper.orderByDesc(DustAccumulationRecord::getDetectDate);
        return wrapper;
    }

    private DustRecordVO convertToDustVO(DustAccumulationRecord record) {
        DustRecordVO vo = new DustRecordVO();
        vo.setId(record.getId());
        vo.setStationId(record.getStationId());
        vo.setStationName(record.getStationName());
        vo.setInverterId(record.getInverterId());
        vo.setInverterName(record.getInverterName());
        vo.setArrayNumber(record.getArrayNumber());
        vo.setDetectDate(record.getDetectDate());
        vo.setActualEnergy(record.getActualEnergy());
        vo.setTheoreticalEnergy(record.getTheoreticalEnergy());
        vo.setAttenuationRate(record.getAttenuationRate());
        vo.setAttenuationRatePercent(
                DustAccumulationCalculator.calculateAttenuationRatePercentage(record.getAttenuationRate()));
        vo.setEstimatedLossEnergy(record.getEstimatedLossEnergy());
        vo.setDustLevel(record.getDustLevel());
        vo.setContinuousDeclineDays(record.getContinuousDeclineDays());
        vo.setHasReminder(record.getHasReminder());

        DustLevelEnum levelEnum = DustLevelEnum.getByCode(record.getDustLevel());
        if (levelEnum != null) {
            vo.setDustLevelDesc(levelEnum.getDesc());
            vo.setDustLevelColor(levelEnum.getColor());
        }

        return vo;
    }
}
