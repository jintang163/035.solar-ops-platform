package com.solar.ops.analysis.controller;

import com.solar.ops.analysis.dto.EfficiencyQueryDTO;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.service.EfficiencyAnalysisService;
import com.solar.ops.analysis.vo.EfficiencyRankVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/efficiency")
@Api(tags = "效率分析接口")
public class EfficiencyAnalysisController {

    @Autowired
    private EfficiencyAnalysisService efficiencyAnalysisService;

    @GetMapping("/list")
    @ApiOperation("分页查询效率统计列表")
    public Result<PageResult<EfficiencyStatistics>> list(EfficiencyQueryDTO queryDTO) {
        return Result.success(efficiencyAnalysisService.queryEfficiencyList(queryDTO));
    }

    @GetMapping("/station/{stationId}")
    @ApiOperation("查询电站效率趋势")
    public Result<List<EfficiencyStatistics>> getStationEfficiency(
            @PathVariable Long stationId,
            @ApiParam("统计类型：1日 2周 3月 4年") @RequestParam Integer statisticsType,
            @ApiParam("开始日期") @RequestParam LocalDate startDate,
            @ApiParam("结束日期") @RequestParam LocalDate endDate) {
        return Result.success(efficiencyAnalysisService.queryStationEfficiency(stationId, statisticsType, startDate, endDate));
    }

    @GetMapping("/rank")
    @ApiOperation("效率排名")
    public Result<List<EfficiencyRankVO>> getEfficiencyRank(
            @ApiParam("统计类型：1日 2周 3月 4年") @RequestParam Integer statisticsType,
            @ApiParam("统计日期") @RequestParam LocalDate date,
            @ApiParam("排名数量") @RequestParam(defaultValue = "10") int topN) {
        return Result.success(efficiencyAnalysisService.getEfficiencyRank(statisticsType, date, topN));
    }

    @GetMapping("/low-efficiency/{stationId}")
    @ApiOperation("低效逆变器识别")
    public Result<List<EfficiencyStatistics>> getLowEfficiencyInverters(
            @PathVariable Long stationId,
            @ApiParam("统计日期") @RequestParam LocalDate date,
            @ApiParam("PR阈值") @RequestParam(required = false) BigDecimal prThreshold) {
        return Result.success(efficiencyAnalysisService.getLowEfficiencyInverters(stationId, date, prThreshold));
    }

    @PostMapping("/calculate")
    @ApiOperation("计算PR值")
    public Result<BigDecimal> calculatePr(
            @ApiParam("实际发电量") @RequestParam BigDecimal actualEnergy,
            @ApiParam("装机容量") @RequestParam BigDecimal installedCapacity,
            @ApiParam("峰值日照小时数") @RequestParam BigDecimal peakSunHours) {
        return Result.success(efficiencyAnalysisService.calculatePr(actualEnergy, installedCapacity, peakSunHours));
    }
}
