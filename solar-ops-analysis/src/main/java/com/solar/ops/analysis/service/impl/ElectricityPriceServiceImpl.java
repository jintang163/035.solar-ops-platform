package com.solar.ops.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.InvestmentInfoDTO;
import com.solar.ops.analysis.dto.PriceSchemeCompareDTO;
import com.solar.ops.analysis.dto.PriceSchemeQueryDTO;
import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.enums.RevenueStatisticsTypeEnum;
import com.solar.ops.analysis.mapper.ElectricityPriceSchemeMapper;
import com.solar.ops.analysis.mapper.EfficiencyStatisticsMapper;
import com.solar.ops.analysis.service.ElectricityPriceService;
import com.solar.ops.analysis.vo.PriceSchemeCompareVO;
import com.solar.ops.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElectricityPriceServiceImpl extends ServiceImpl<ElectricityPriceSchemeMapper, ElectricityPriceScheme> implements ElectricityPriceService {

    private static final Logger log = LoggerFactory.getLogger(ElectricityPriceServiceImpl.class);

    @Autowired
    private EfficiencyStatisticsMapper efficiencyStatisticsMapper;

    @Override
    public List<ElectricityPriceScheme> list(PriceSchemeQueryDTO query) {
        LambdaQueryWrapper<ElectricityPriceScheme> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(ElectricityPriceScheme::getIsDefault)
                .orderByDesc(ElectricityPriceScheme::getCreateTime);
        return list(wrapper);
    }

    @Override
    public ElectricityPriceScheme getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(ElectricityPriceScheme scheme) {
        if (scheme.getIsDefault() != null && scheme.getIsDefault() == 1) {
            cancelDefaultScheme(scheme.getStationId());
        }
        return super.save(scheme);
    }

    @Override
    public boolean update(ElectricityPriceScheme scheme) {
        ElectricityPriceScheme existing = getById(scheme.getId());
        if (existing == null) {
            throw new BusinessException("电价方案不存在");
        }
        if (scheme.getIsDefault() != null && scheme.getIsDefault() == 1) {
            cancelDefaultScheme(scheme.getStationId());
        }
        return super.updateById(scheme);
    }

    @Override
    public boolean remove(Long id) {
        return super.removeById(id);
    }

    @Override
    public ElectricityPriceScheme getDefaultScheme(Long stationId) {
        LambdaQueryWrapper<ElectricityPriceScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectricityPriceScheme::getIsDefault, 1)
                .eq(ElectricityPriceScheme::getStatus, 1);
        if (stationId != null) {
            wrapper.and(w -> w.eq(ElectricityPriceScheme::getStationId, stationId)
                    .or().isNull(ElectricityPriceScheme::getStationId));
            wrapper.orderByDesc(ElectricityPriceScheme::getStationId);
        } else {
            wrapper.isNull(ElectricityPriceScheme::getStationId);
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public BigDecimal calculateEffectivePrice(Long schemeId, LocalDate date) {
        ElectricityPriceScheme scheme = getById(schemeId);
        if (scheme == null) {
            return BigDecimal.ZERO;
        }
        return calculateEffectivePrice(scheme, date);
    }

    @Override
    public List<PriceSchemeCompareVO> compareSchemes(PriceSchemeCompareDTO dto, InvestmentInfoDTO investment) {
        List<Long> schemeIds = dto.getSchemeIds();
        List<ElectricityPriceScheme> schemes;
        if (CollectionUtils.isEmpty(schemeIds)) {
            LambdaQueryWrapper<ElectricityPriceScheme> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ElectricityPriceScheme::getStatus, 1)
                    .and(w -> w.eq(ElectricityPriceScheme::getStationId, dto.getStationId())
                            .or().isNull(ElectricityPriceScheme::getStationId));
            schemes = list(wrapper);
        } else {
            schemes = listByIds(schemeIds);
        }

        if (CollectionUtils.isEmpty(schemes)) {
            return Collections.emptyList();
        }

        BigDecimal estimatedYearEnergy = calculateYearlyEnergy(dto.getStationId());

        List<PriceSchemeCompareVO> result = new ArrayList<>();
        for (ElectricityPriceScheme scheme : schemes) {
            PriceSchemeCompareVO vo = convertToCompareVO(scheme, estimatedYearEnergy, investment);
            result.add(vo);
        }

        return result;
    }

    private BigDecimal calculateEffectivePrice(ElectricityPriceScheme scheme, LocalDate date) {
        BigDecimal totalPrice = scheme.getGridPrice() != null
                ? scheme.getGridPrice() : BigDecimal.ZERO;

        boolean inSubsidyPeriod = true;
        if (scheme.getSubsidyStartDate() != null && date.isBefore(scheme.getSubsidyStartDate())) {
            inSubsidyPeriod = false;
        }
        if (scheme.getSubsidyEndDate() != null && date.isAfter(scheme.getSubsidyEndDate())) {
            inSubsidyPeriod = false;
        }

        if (inSubsidyPeriod) {
            if (scheme.getNationalSubsidy() != null) {
                totalPrice = totalPrice.add(scheme.getNationalSubsidy());
            }
            if (scheme.getProvincialSubsidy() != null) {
                totalPrice = totalPrice.add(scheme.getProvincialSubsidy());
            }
            if (scheme.getMunicipalSubsidy() != null) {
                totalPrice = totalPrice.add(scheme.getMunicipalSubsidy());
            }
        }

        return totalPrice.setScale(4, RoundingMode.HALF_UP);
    }

    private void cancelDefaultScheme(Long stationId) {
        LambdaQueryWrapper<ElectricityPriceScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectricityPriceScheme::getIsDefault, 1);
        if (stationId != null) {
            wrapper.eq(ElectricityPriceScheme::getStationId, stationId);
        } else {
            wrapper.isNull(ElectricityPriceScheme::getStationId);
        }
        List<ElectricityPriceScheme> schemes = list(wrapper);
        for (ElectricityPriceScheme scheme : schemes) {
            scheme.setIsDefault(0);
            updateById(scheme);
        }
    }

    private LambdaQueryWrapper<ElectricityPriceScheme> buildQueryWrapper(PriceSchemeQueryDTO query) {
        LambdaQueryWrapper<ElectricityPriceScheme> wrapper = new LambdaQueryWrapper<>();
        if (query.getStationId() != null) {
            wrapper.and(w -> w.eq(ElectricityPriceScheme::getStationId, query.getStationId())
                    .or().isNull(ElectricityPriceScheme::getStationId));
        }
        if (query.getStatus() != null) {
            wrapper.eq(ElectricityPriceScheme::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(ElectricityPriceScheme::getSchemeName, query.getKeyword());
        }
        return wrapper;
    }

    private BigDecimal calculateYearlyEnergy(Long stationId) {
        LocalDate now = LocalDate.now();
        LocalDate oneYearAgo = now.minusYears(1);

        LambdaQueryWrapper<EfficiencyStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EfficiencyStatistics::getStationId, stationId)
                .eq(EfficiencyStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                .between(EfficiencyStatistics::getStatisticsDate, oneYearAgo, now)
                .isNull(EfficiencyStatistics::getInverterId);

        List<EfficiencyStatistics> stats = efficiencyStatisticsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(stats)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalEnergy = stats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long days = stats.size();
        if (days > 0) {
            return totalEnergy.multiply(new BigDecimal("365"))
                    .divide(new BigDecimal(days), 2, RoundingMode.HALF_UP);
        }

        return totalEnergy.setScale(2, RoundingMode.HALF_UP);
    }

    private PriceSchemeCompareVO convertToCompareVO(ElectricityPriceScheme scheme,
                                                     BigDecimal estimatedYearEnergy,
                                                     InvestmentInfoDTO investment) {
        PriceSchemeCompareVO vo = new PriceSchemeCompareVO();
        vo.setSchemeId(scheme.getId());
        vo.setSchemeName(scheme.getSchemeName());
        vo.setGridPrice(scheme.getGridPrice());
        vo.setNationalSubsidy(scheme.getNationalSubsidy());
        vo.setProvincialSubsidy(scheme.getProvincialSubsidy());
        vo.setMunicipalSubsidy(scheme.getMunicipalSubsidy());

        BigDecimal totalPrice = calculateEffectivePrice(scheme, LocalDate.now());
        vo.setTotalPrice(totalPrice);
        vo.setEstimatedYearEnergy(estimatedYearEnergy);

        BigDecimal estimatedYearRevenue = estimatedYearEnergy.multiply(totalPrice)
                .setScale(2, RoundingMode.HALF_UP);
        vo.setEstimatedYearRevenue(estimatedYearRevenue);

        if (investment != null && investment.getTotalInvestment() != null
                && investment.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualOperationCost = investment.getAnnualOperationCost() != null
                    ? investment.getAnnualOperationCost() : BigDecimal.ZERO;
            BigDecimal yearNetRevenue = estimatedYearRevenue.subtract(annualOperationCost);

            BigDecimal roi = yearNetRevenue.divide(investment.getTotalInvestment(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setRoi(roi);

            if (yearNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal paybackPeriod = investment.getTotalInvestment()
                        .divide(yearNetRevenue, 2, RoundingMode.HALF_UP);
                vo.setPaybackPeriod(paybackPeriod);
            } else {
                vo.setPaybackPeriod(BigDecimal.ZERO);
            }
        } else {
            vo.setRoi(BigDecimal.ZERO);
            vo.setPaybackPeriod(BigDecimal.ZERO);
        }

        return vo;
    }
}
