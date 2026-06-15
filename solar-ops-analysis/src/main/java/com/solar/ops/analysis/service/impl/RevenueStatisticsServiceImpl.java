package com.solar.ops.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.analysis.dto.InvestmentInfoDTO;
import com.solar.ops.analysis.dto.RevenueQueryDTO;
import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import com.solar.ops.analysis.entity.RevenueStatistics;
import com.solar.ops.analysis.enums.RevenueStatisticsTypeEnum;
import com.solar.ops.analysis.mapper.RevenueStatisticsMapper;
import com.solar.ops.analysis.service.ElectricityPriceService;
import com.solar.ops.analysis.service.RevenueCalculateService;
import com.solar.ops.analysis.service.RevenueStatisticsService;
import com.solar.ops.analysis.vo.RevenueDashboardVO;
import com.solar.ops.analysis.vo.RevenueStatisticsVO;
import com.solar.ops.analysis.vo.RevenueTrendVO;
import com.solar.ops.analysis.vo.StationRevenueRankVO;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RevenueStatisticsServiceImpl extends ServiceImpl<RevenueStatisticsMapper, RevenueStatistics> implements RevenueStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(RevenueStatisticsServiceImpl.class);

    @Autowired
    private ElectricityPriceService electricityPriceService;

    @Autowired
    private RevenueCalculateService revenueCalculateService;

    @Autowired
    private StationMapper stationMapper;

    @Override
    public PageResult<RevenueStatisticsVO> queryPage(RevenueQueryDTO query) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = buildQueryWrapper(query);
        Page<RevenueStatistics> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<RevenueStatistics> result = page(page, wrapper);
        List<RevenueStatisticsVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return PageResult.build(voList, result.getTotal());
    }

    @Override
    public List<RevenueStatisticsVO> queryList(RevenueQueryDTO query) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(RevenueStatistics::getStatisticsDate);
        return list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public RevenueDashboardVO getDashboard(Long stationId, InvestmentInfoDTO investment) {
        RevenueDashboardVO vo = new RevenueDashboardVO();

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        int currentYear = today.getYear();

        vo.setTodayRevenue(getDayRevenue(stationId, today));
        vo.setMonthRevenue(getMonthRevenue(stationId, currentMonth));
        vo.setYearRevenue(getYearRevenue(stationId, currentYear));
        RevenueQueryDTO totalQuery = new RevenueQueryDTO();
        totalQuery.setStationId(stationId);
        vo.setTotalRevenue(getTotalRevenue(totalQuery));

        vo.setTodayEnergy(getDayEnergy(stationId, today));
        vo.setMonthEnergy(getMonthEnergy(stationId, currentMonth));

        RevenueQueryDTO query = new RevenueQueryDTO();
        query.setStationId(stationId);
        query.setStatisticsType(RevenueStatisticsTypeEnum.DAY.getCode());
        query.setStartDate(today.minusDays(29));
        query.setEndDate(today);
        List<RevenueStatisticsVO> list = queryList(query);

        BigDecimal totalRevenue = list.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEnergy = list.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = list.stream()
                .map(s -> s.getUnitEnergyCost() != null ? s.getUnitEnergyCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!list.isEmpty()) {
            vo.setAvgGridPrice(totalEnergy.compareTo(BigDecimal.ZERO) > 0
                    ? totalRevenue.divide(totalEnergy, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO));
            vo.setAvgUnitCost(totalCost.divide(new BigDecimal(list.size()), 4, RoundingMode.HALF_UP));
        } else {
            vo.setAvgGridPrice(BigDecimal.ZERO);
            vo.setAvgUnitCost(BigDecimal.ZERO);
        }

        BigDecimal totalInvestment = null;
        BigDecimal annualOpCost = null;

        if (investment != null && investment.getTotalInvestment() != null
                && investment.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0) {
            totalInvestment = investment.getTotalInvestment();
            annualOpCost = investment.getAnnualOperationCost();
        } else if (stationId != null) {
            Station station = stationMapper.selectById(stationId);
            if (station != null && station.getTotalInvestment() != null
                    && station.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0) {
                totalInvestment = station.getTotalInvestment();
                annualOpCost = station.getAnnualOperationCost();
            }
        }

        if (totalInvestment != null) {
            BigDecimal yearNetRevenue = vo.getYearRevenue().subtract(
                    annualOpCost != null ? annualOpCost : BigDecimal.ZERO);
            if (yearNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal roi = yearNetRevenue.divide(totalInvestment, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setRoi(roi);
                BigDecimal paybackPeriod = totalInvestment
                        .divide(yearNetRevenue, 2, RoundingMode.HALF_UP);
                vo.setPaybackPeriod(paybackPeriod);
            } else {
                vo.setRoi(BigDecimal.ZERO);
                vo.setPaybackPeriod(BigDecimal.ZERO);
            }
        } else {
            vo.setRoi(BigDecimal.ZERO);
            vo.setPaybackPeriod(BigDecimal.ZERO);
        }

        vo.setRevenueTrend(getRevenueTrend(stationId, 1, 30));
        vo.setCostTrend(getCostTrend(stationId, 30));
        vo.setStationRank(getStationRank(stationId, 3));

        return vo;
    }

    @Override
    public List<RevenueTrendVO> getRevenueTrend(Long stationId, Integer type, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(RevenueStatistics::getStationId, stationId);
        }
        wrapper.eq(RevenueStatistics::getStatisticsType, type)
                .between(RevenueStatistics::getStatisticsDate, startDate, endDate)
                .orderByAsc(RevenueStatistics::getStatisticsDate);

        List<RevenueStatistics> list = list(wrapper);
        Map<LocalDate, RevenueStatistics> dataMap = new HashMap<>();
        for (RevenueStatistics stat : list) {
            dataMap.put(stat.getStatisticsDate(), stat);
        }

        List<RevenueTrendVO> result = new ArrayList<>();
        DateTimeFormatter formatter = type == RevenueStatisticsTypeEnum.MONTH.getCode()
                ? DateTimeFormatter.ofPattern("yyyy-MM")
                : DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            RevenueStatistics stat = dataMap.get(date);
            RevenueTrendVO trendVO = new RevenueTrendVO();
            if (stat != null) {
                trendVO.setPeriod(stat.getStatisticsDate().format(formatter));
                trendVO.setGridEnergy(stat.getGridEnergy() != null ? stat.getGridEnergy() : BigDecimal.ZERO);
                trendVO.setGridRevenue(stat.getGridRevenue() != null ? stat.getGridRevenue() : BigDecimal.ZERO);
                trendVO.setSubsidyRevenue(stat.getTotalSubsidyRevenue() != null ? stat.getTotalSubsidyRevenue() : BigDecimal.ZERO);
                trendVO.setTotalRevenue(stat.getTotalRevenue() != null ? stat.getTotalRevenue() : BigDecimal.ZERO);
            } else {
                trendVO.setPeriod(date.format(formatter));
                trendVO.setGridEnergy(BigDecimal.ZERO);
                trendVO.setGridRevenue(BigDecimal.ZERO);
                trendVO.setSubsidyRevenue(BigDecimal.ZERO);
                trendVO.setTotalRevenue(BigDecimal.ZERO);
            }
            result.add(trendVO);
        }

        return result;
    }

    @Override
    public List<StationRevenueRankVO> getStationRank(Long stationId, Integer type) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        Integer statisticsType;

        switch (type) {
            case 2:
                startDate = endDate.minusWeeks(1);
                statisticsType = RevenueStatisticsTypeEnum.WEEK.getCode();
                break;
            case 3:
                startDate = endDate.minusMonths(1);
                statisticsType = RevenueStatisticsTypeEnum.MONTH.getCode();
                break;
            case 4:
                startDate = endDate.minusYears(1);
                statisticsType = RevenueStatisticsTypeEnum.YEAR.getCode();
                break;
            default:
                startDate = endDate.minusDays(30);
                statisticsType = RevenueStatisticsTypeEnum.DAY.getCode();
                break;
        }

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(RevenueStatistics::getStationId, stationId);
        }
        wrapper.eq(RevenueStatistics::getStatisticsType, statisticsType)
                .between(RevenueStatistics::getStatisticsDate, startDate, endDate);

        List<RevenueStatistics> list = list(wrapper);

        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<Long, List<RevenueStatistics>> stationGrouped = list.stream()
                .filter(s -> s.getStationId() != null)
                .collect(Collectors.groupingBy(RevenueStatistics::getStationId));

        List<StationRevenueRankVO> rankList = new ArrayList<>();
        for (Map.Entry<Long, List<RevenueStatistics>> entry : stationGrouped) {
            Long statId = entry.getKey();
            List<RevenueStatistics> stationStats = entry.getValue();

            BigDecimal totalRevenue = stationStats.stream()
                    .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalEnergy = stationStats.stream()
                    .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgUnitPrice = totalEnergy.compareTo(BigDecimal.ZERO) > 0
                    ? totalRevenue.divide(totalEnergy, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            BigDecimal avgUnitCost = stationStats.stream()
                    .map(s -> s.getUnitEnergyCost() != null ? s.getUnitEnergyCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(stationStats.size()), 4, RoundingMode.HALF_UP);

            StationRevenueRankVO vo = new StationRevenueRankVO();
            vo.setStationId(statId);
            Station station = stationMapper.selectById(statId);
            vo.setStationName(station != null ? station.getStationName() : "电站" + statId);
            vo.setTotalRevenue(totalRevenue);
            vo.setTotalEnergy(totalEnergy);
            vo.setAvgUnitPrice(avgUnitPrice);
            vo.setUnitCost(avgUnitCost);
            rankList.add(vo);
        }

        rankList.sort(Comparator
                .comparing(StationRevenueRankVO::getTotalRevenue, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StationRevenueRankVO::getTotalEnergy, Comparator.reverseOrder()));

        return rankList.size() > 10 ? rankList.subList(0, 10) : rankList;
    }

    @Override
    public BigDecimal getTotalRevenue(RevenueQueryDTO query) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = buildQueryWrapper(query);
        List<RevenueStatistics> list = list(wrapper);
        return list.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private LambdaQueryWrapper<RevenueStatistics> buildQueryWrapper(RevenueQueryDTO query) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        if (query.getStationId() != null) {
            wrapper.eq(RevenueStatistics::getStationId, query.getStationId());
        }
        if (query.getStatisticsType() != null) {
            wrapper.eq(RevenueStatistics::getStatisticsType, query.getStatisticsType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(RevenueStatistics::getStatisticsDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(RevenueStatistics::getStatisticsDate, query.getEndDate());
        }
        wrapper.orderByDesc(RevenueStatistics::getStatisticsDate);
        return wrapper;
    }

    private RevenueStatisticsVO convertToVO(RevenueStatistics stat) {
        RevenueStatisticsVO vo = new RevenueStatisticsVO();
        BeanUtils.copyProperties(stat, vo);

        RevenueStatisticsTypeEnum typeEnum = RevenueStatisticsTypeEnum.getByCode(stat.getStatisticsType());
        if (typeEnum != null) {
            vo.setStatisticsTypeDesc(typeEnum.getDesc());
        }

        vo.setSettlementStatusDesc(stat.getSettlementStatus() != null && stat.getSettlementStatus() == 1 ? "已结算" : "未结算");

        if (stat.getPriceSchemeId() != null) {
            ElectricityPriceScheme scheme = electricityPriceService.getById(stat.getPriceSchemeId());
            if (scheme != null) {
                vo.setSchemeName(scheme.getSchemeName());
            }
        }

        return vo;
    }

    private BigDecimal getDayRevenue(Long stationId, LocalDate date) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId);
        wrapper.eq(RevenueStatistics::getStatisticsDate, date);
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        RevenueStatistics stat = getOne(wrapper);
        return stat != null && stat.getTotalRevenue() != null ? stat.getTotalRevenue() : BigDecimal.ZERO;
    }

    private BigDecimal getDayEnergy(Long stationId, LocalDate date) {
        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId);
        wrapper.eq(RevenueStatistics::getStatisticsDate, date);
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        RevenueStatistics stat = getOne(wrapper);
        return stat != null && stat.getGridEnergy() != null ? stat.getGridEnergy() : BigDecimal.ZERO;
    }

    private BigDecimal getMonthRevenue(Long stationId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId);
        wrapper.eq(RevenueStatistics::getStatisticsDate, monthStart);
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode());
        RevenueStatistics monthlyStat = getOne(wrapper);
        if (monthlyStat != null && monthlyStat.getTotalRevenue() != null) {
            return monthlyStat.getTotalRevenue();
        }

        LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(RevenueStatistics::getStationId, stationId);
        dailyWrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        dailyWrapper.between(RevenueStatistics::getStatisticsDate, monthStart, monthEnd);
        List<RevenueStatistics> list = list(dailyWrapper);
        return list.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getMonthEnergy(Long stationId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId);
        wrapper.eq(RevenueStatistics::getStatisticsDate, monthStart);
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode());
        RevenueStatistics monthlyStat = getOne(wrapper);
        if (monthlyStat != null && monthlyStat.getGridEnergy() != null) {
            return monthlyStat.getGridEnergy();
        }

        LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(RevenueStatistics::getStationId, stationId);
        dailyWrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        dailyWrapper.between(RevenueStatistics::getStatisticsDate, monthStart, monthEnd);
        List<RevenueStatistics> list = list(dailyWrapper);
        return list.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getYearRevenue(Long stationId, Integer year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RevenueStatistics::getStationId, stationId);
        wrapper.eq(RevenueStatistics::getStatisticsDate, yearStart);
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.YEAR.getCode());
        RevenueStatistics yearlyStat = getOne(wrapper);
        if (yearlyStat != null && yearlyStat.getTotalRevenue() != null) {
            return yearlyStat.getTotalRevenue();
        }

        LambdaQueryWrapper<RevenueStatistics> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(RevenueStatistics::getStationId, stationId);
        monthWrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.MONTH.getCode());
        monthWrapper.between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
        List<RevenueStatistics> list = list(monthWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            return list.stream()
                    .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        LambdaQueryWrapper<RevenueStatistics> dailyWrapper = new LambdaQueryWrapper<>();
        dailyWrapper.eq(RevenueStatistics::getStationId, stationId);
        dailyWrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode());
        dailyWrapper.between(RevenueStatistics::getStatisticsDate, yearStart, yearEnd);
        List<RevenueStatistics> dailyList = list(dailyWrapper);
        return dailyList.stream()
                .map(s -> s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RevenueTrendVO> getCostTrend(Long stationId, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        LambdaQueryWrapper<RevenueStatistics> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(RevenueStatistics::getStationId, stationId);
        }
        wrapper.eq(RevenueStatistics::getStatisticsType, RevenueStatisticsTypeEnum.DAY.getCode())
                .between(RevenueStatistics::getStatisticsDate, startDate, endDate)
                .orderByAsc(RevenueStatistics::getStatisticsDate);

        List<RevenueStatistics> list = list(wrapper);
        Map<LocalDate, RevenueStatistics> dataMap = new HashMap<>();
        for (RevenueStatistics stat : list) {
            dataMap.put(stat.getStatisticsDate(), stat);
        }

        List<RevenueTrendVO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            RevenueStatistics stat = dataMap.get(date);
            RevenueTrendVO trendVO = new RevenueTrendVO();
            trendVO.setPeriod(date.format(formatter));
            if (stat != null) {
                trendVO.setTotalRevenue(stat.getUnitEnergyCost() != null ? stat.getUnitEnergyCost() : BigDecimal.ZERO);
            } else {
                trendVO.setTotalRevenue(BigDecimal.ZERO);
            }
            result.add(trendVO);
        }

        return result;
    }
}
