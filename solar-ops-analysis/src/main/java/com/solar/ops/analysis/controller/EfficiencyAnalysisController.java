package com.solar.ops.analysis.controller;

import com.alibaba.excel.EasyExcel;
import com.solar.ops.analysis.dto.EfficiencyQueryDTO;
import com.solar.ops.analysis.dto.PeriodCompareDTO;
import com.solar.ops.analysis.dto.StationCompareQueryDTO;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.excel.StationCompareExcelVO;
import com.solar.ops.analysis.service.EfficiencyAnalysisService;
import com.solar.ops.analysis.vo.EfficiencyRankVO;
import com.solar.ops.analysis.vo.StationCompareVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @PostMapping("/compare")
    @ApiOperation("电站对比分析")
    public Result<StationCompareVO> compareStations(@Valid @RequestBody StationCompareQueryDTO dto) {
        return Result.success(efficiencyAnalysisService.compareStations(dto));
    }

    @GetMapping("/compare/export")
    @ApiOperation("导出电站对比分析报告")
    public void exportCompare(
            @ApiParam(value = "电站ID列表，逗号分隔", required = true) @RequestParam String stationIds,
            @ApiParam("开始日期") @RequestParam(required = false) LocalDate startTime,
            @ApiParam("结束日期") @RequestParam(required = false) LocalDate endTime,
            @ApiParam("统计类型：1日 2周 3月 4年，默认3月") @RequestParam(required = false, defaultValue = "3") Integer statisticsType,
            HttpServletResponse response) throws IOException {
        StationCompareQueryDTO dto = new StationCompareQueryDTO();
        dto.setStationIds(Arrays.stream(stationIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList()));
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setStatisticsType(statisticsType);

        List<StationCompareExcelVO> list = efficiencyAnalysisService.exportCompare(dto);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("电站对比分析报告", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StationCompareExcelVO.class)
                .sheet("电站对比分析")
                .doWrite(list);
    }
}
