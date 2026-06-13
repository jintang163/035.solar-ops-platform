package com.solar.ops.analysis.controller;

import com.solar.ops.analysis.entity.StationHealth;
import com.solar.ops.analysis.service.HealthAssessmentService;
import com.solar.ops.analysis.vo.HealthAssessmentVO;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/health")
@Api(tags = "健康度评估接口")
public class HealthController {

    @Autowired
    private HealthAssessmentService healthAssessmentService;

    @PostMapping("/assess/{stationId}")
    @ApiOperation("评估电站健康度")
    public Result<HealthAssessmentVO> assessStationHealth(@PathVariable Long stationId) {
        return Result.success(healthAssessmentService.assessStationHealth(stationId));
    }

    @GetMapping("/latest/{stationId}")
    @ApiOperation("获取最新健康度评估结果")
    public Result<StationHealth> getLatestHealth(@PathVariable Long stationId) {
        return Result.success(healthAssessmentService.getLatestHealth(stationId));
    }

    @GetMapping("/history/{stationId}")
    @ApiOperation("获取健康度历史记录")
    public Result<List<StationHealth>> getHealthHistory(
            @PathVariable Long stationId,
            @ApiParam("开始日期") @RequestParam LocalDate startDate,
            @ApiParam("结束日期") @RequestParam LocalDate endDate) {
        return Result.success(healthAssessmentService.getHealthHistory(stationId, startDate, endDate));
    }

    @PostMapping("/batch")
    @ApiOperation("批量评估电站健康度")
    public Result<Void> batchAssessHealth(@RequestBody List<Long> stationIds) {
        healthAssessmentService.batchAssessHealth(stationIds);
        return Result.success();
    }

    @PostMapping("/list")
    @ApiOperation("获取多个电站健康度列表")
    public Result<List<HealthAssessmentVO>> getStationHealthList(@RequestBody List<Long> stationIds) {
        return Result.success(healthAssessmentService.getStationHealthList(stationIds));
    }
}
