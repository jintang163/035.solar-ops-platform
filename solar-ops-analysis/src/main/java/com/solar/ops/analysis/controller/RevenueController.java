package com.solar.ops.analysis.controller;

import com.solar.ops.analysis.dto.InvestmentInfoDTO;
import com.solar.ops.analysis.dto.PriceSchemeCompareDTO;
import com.solar.ops.analysis.dto.PriceSchemeQueryDTO;
import com.solar.ops.analysis.dto.RevenueQueryDTO;
import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import com.solar.ops.analysis.entity.RevenueStatistics;
import com.solar.ops.analysis.service.ElectricityPriceService;
import com.solar.ops.analysis.service.RevenueCalculateService;
import com.solar.ops.analysis.service.RevenueStatisticsService;
import com.solar.ops.analysis.vo.PriceSchemeCompareVO;
import com.solar.ops.analysis.vo.RevenueDashboardVO;
import com.solar.ops.analysis.vo.RevenueStatisticsVO;
import com.solar.ops.analysis.vo.RevenueTrendVO;
import com.solar.ops.analysis.vo.StationRevenueRankVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/revenue")
@Api(tags = "电费收益计算接口")
public class RevenueController {

    @Autowired
    private ElectricityPriceService electricityPriceService;

    @Autowired
    private RevenueCalculateService revenueCalculateService;

    @Autowired
    private RevenueStatisticsService revenueStatisticsService;

    // ==================== 电价方案接口 ====================

    @PostMapping("/price-scheme/page")
    @ApiOperation("分页查询电价方案")
    public Result<PageResult<ElectricityPriceScheme>> queryPriceSchemePage(
            @RequestBody PriceSchemeQueryDTO queryDTO) {
        List<ElectricityPriceScheme> list = electricityPriceService.list(queryDTO);
        return Result.success(PageResult.build(list, (long) list.size()));
    }

    @PostMapping("/price-scheme/list")
    @ApiOperation("查询电价方案列表")
    public Result<List<ElectricityPriceScheme>> queryPriceSchemeList(
            @RequestBody PriceSchemeQueryDTO queryDTO) {
        return Result.success(electricityPriceService.list(queryDTO));
    }

    @GetMapping("/price-scheme/{id}")
    @ApiOperation("查询电价方案详情")
    public Result<ElectricityPriceScheme> getPriceSchemeById(
            @ApiParam(value = "方案ID", required = true) @PathVariable Long id) {
        return Result.success(electricityPriceService.getById(id));
    }

    @PostMapping("/price-scheme/save")
    @ApiOperation("新增电价方案")
    public Result<Boolean> savePriceScheme(
            @RequestBody ElectricityPriceScheme scheme) {
        return Result.success(electricityPriceService.save(scheme));
    }

    @PostMapping("/price-scheme/update")
    @ApiOperation("更新电价方案")
    public Result<Boolean> updatePriceScheme(
            @RequestBody ElectricityPriceScheme scheme) {
        return Result.success(electricityPriceService.update(scheme));
    }

    @DeleteMapping("/price-scheme/{id}")
    @ApiOperation("删除电价方案")
    public Result<Boolean> removePriceScheme(
            @ApiParam(value = "方案ID", required = true) @PathVariable Long id) {
        return Result.success(electricityPriceService.remove(id));
    }

    @GetMapping("/price-scheme/default")
    @ApiOperation("获取电站默认电价方案")
    public Result<ElectricityPriceScheme> getDefaultScheme(
            @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId) {
        return Result.success(electricityPriceService.getDefaultScheme(stationId));
    }

    @GetMapping("/price-scheme/effective-price")
    @ApiOperation("计算指定日期的有效电价")
    public Result<BigDecimal> calculateEffectivePrice(
            @ApiParam(value = "方案ID", required = true) @RequestParam Long schemeId,
            @ApiParam(value = "计算日期", required = true) @RequestParam LocalDate date) {
        return Result.success(electricityPriceService.calculateEffectivePrice(schemeId, date));
    }

    @PostMapping("/price-scheme/compare")
    @ApiOperation("对比多个电价方案")
    public Result<List<PriceSchemeCompareVO>> compareSchemes(
            @RequestBody @Validated PriceSchemeCompareDTO dto,
            @ApiParam(value = "总投资（元）") @RequestParam(required = false) BigDecimal totalInvestment,
            @ApiParam(value = "年运维成本（元）") @RequestParam(required = false) BigDecimal annualOperationCost,
            @ApiParam(value = "设计寿命（年）") @RequestParam(required = false) Integer designLife) {
        InvestmentInfoDTO investment = new InvestmentInfoDTO();
        investment.setTotalInvestment(totalInvestment);
        investment.setAnnualOperationCost(annualOperationCost);
        investment.setDesignLife(designLife);
        return Result.success(electricityPriceService.compareSchemes(dto, investment));
    }

    // ==================== 收益计算接口 ====================

    @PostMapping("/calculate/daily")
    @ApiOperation("计算单个电站的日收益")
    public Result<RevenueStatistics> calculateDailyRevenue(
            @ApiParam(value = "电站ID", required = true) @RequestParam Long stationId,
            @ApiParam(value = "计算日期", required = true) @RequestParam LocalDate date) {
        return Result.success(revenueCalculateService.calculateDailyRevenue(stationId, date));
    }

    @PostMapping("/calculate/monthly")
    @ApiOperation("计算单个电站的月收益")
    public Result<List<RevenueStatistics>> calculateMonthlyRevenue(
            @ApiParam(value = "电站ID", required = true) @RequestParam Long stationId,
            @ApiParam(value = "年份", required = true) @RequestParam Integer year,
            @ApiParam(value = "月份", required = true) @RequestParam Integer month) {
        return Result.success(revenueCalculateService.calculateMonthlyRevenue(stationId, year, month));
    }

    @PostMapping("/calculate/all-daily")
    @ApiOperation("计算所有电站的日收益")
    public Result<Void> calculateAllStationsDailyRevenue(
            @ApiParam(value = "计算日期", required = true) @RequestParam LocalDate date) {
        revenueCalculateService.calculateAllStationsDailyRevenue(date);
        return Result.success();
    }

    @PostMapping("/calculate/yearly")
    @ApiOperation("计算单个电站的年收益")
    public Result<RevenueStatistics> calculateYearlyRevenue(
            @ApiParam(value = "电站ID", required = true) @RequestParam Long stationId,
            @ApiParam(value = "年份", required = true) @RequestParam Integer year) {
        return Result.success(revenueCalculateService.calculateYearlyRevenue(stationId, year));
    }

    @GetMapping("/calculate/unit-cost")
    @ApiOperation("计算度电成本")
    public Result<BigDecimal> calculateUnitCost(
            @ApiParam(value = "电站ID", required = true) @RequestParam Long stationId,
            @ApiParam(value = "上网电量（kWh）", required = true) @RequestParam BigDecimal energy) {
        return Result.success(revenueCalculateService.calculateUnitCost(stationId, energy));
    }

    // ==================== 收益统计查询接口 ====================

    @PostMapping("/statistics/page")
    @ApiOperation("分页查询收益统计")
    public Result<PageResult<RevenueStatisticsVO>> queryStatisticsPage(
            @RequestBody RevenueQueryDTO queryDTO) {
        return Result.success(revenueStatisticsService.queryPage(queryDTO));
    }

    @PostMapping("/statistics/list")
    @ApiOperation("查询收益统计列表")
    public Result<List<RevenueStatisticsVO>> queryStatisticsList(
            @RequestBody RevenueQueryDTO queryDTO) {
        return Result.success(revenueStatisticsService.queryList(queryDTO));
    }

    @GetMapping("/statistics/dashboard")
    @ApiOperation("获取收益仪表盘数据")
    public Result<RevenueDashboardVO> getDashboard(
            @ApiParam(value = "电站ID（可选，不传则查全部）") @RequestParam(required = false) Long stationId,
            @ApiParam(value = "总投资（元）") @RequestParam(required = false) BigDecimal totalInvestment,
            @ApiParam(value = "年运维成本（元）") @RequestParam(required = false) BigDecimal annualOperationCost,
            @ApiParam(value = "设计寿命（年）") @RequestParam(required = false) Integer designLife) {
        InvestmentInfoDTO investment = new InvestmentInfoDTO();
        investment.setTotalInvestment(totalInvestment);
        investment.setAnnualOperationCost(annualOperationCost);
        investment.setDesignLife(designLife);
        return Result.success(revenueStatisticsService.getDashboard(stationId, investment));
    }

    @GetMapping("/statistics/trend")
    @ApiOperation("获取收益趋势数据")
    public Result<List<RevenueTrendVO>> getRevenueTrend(
            @ApiParam(value = "电站ID（可选）") @RequestParam(required = false) Long stationId,
            @ApiParam(value = "统计类型：1日 2周 3月 4年", defaultValue = "1") @RequestParam(defaultValue = "1") Integer type,
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(revenueStatisticsService.getRevenueTrend(stationId, type, days));
    }

    @GetMapping("/statistics/rank")
    @ApiOperation("获取电站收益排名")
    public Result<List<StationRevenueRankVO>> getStationRank(
            @ApiParam(value = "电站ID（可选，传则只查该电站）") @RequestParam(required = false) Long stationId,
            @ApiParam(value = "统计类型：1日 2周 3月 4年", defaultValue = "3") @RequestParam(defaultValue = "3") Integer type) {
        return Result.success(revenueStatisticsService.getStationRank(stationId, type));
    }

    @PostMapping("/statistics/total")
    @ApiOperation("获取总收益")
    public Result<BigDecimal> getTotalRevenue(
            @RequestBody RevenueQueryDTO queryDTO) {
        return Result.success(revenueStatisticsService.getTotalRevenue(queryDTO));
    }
}
