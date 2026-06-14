package com.solar.ops.prediction.controller;

import com.solar.ops.common.result.Result;
import com.solar.ops.prediction.dto.LifetimeAlertQueryDTO;
import com.solar.ops.prediction.entity.InverterHealth;
import com.solar.ops.prediction.entity.LifetimePrediction;
import com.solar.ops.prediction.service.LifetimePredictionService;
import com.solar.ops.prediction.vo.LifetimeAlertVO;
import com.solar.ops.prediction.vo.LifetimePredictionVO;
import com.solar.ops.prediction.vo.SparePartAdviceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lifetime")
@Api(tags = "设备寿命预测接口")
public class LifetimePredictionController {

    @Autowired
    private LifetimePredictionService lifetimePredictionService;

    @GetMapping("/predict")
    @ApiOperation("获取设备寿命预测")
    public Result<LifetimePredictionVO> getLifetimePrediction(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId,
            @ApiParam("预测天数，默认90") @RequestParam(required = false, defaultValue = "90") Integer forecastDays) {
        return Result.success(lifetimePredictionService.predictLifetime(stationId, inverterId, forecastDays));
    }

    @GetMapping("/latest/{stationId}/{inverterId}")
    @ApiOperation("获取最新寿命预测结果")
    public Result<LifetimePredictionVO> getLatestPrediction(
            @ApiParam("电站ID") @PathVariable Long stationId,
            @ApiParam("逆变器ID") @PathVariable Long inverterId) {
        return Result.success(lifetimePredictionService.getLatestPrediction(stationId, inverterId));
    }

    @GetMapping("/history")
    @ApiOperation("查询寿命预测历史")
    public Result<List<LifetimePrediction>> getPredictionHistory(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId,
            @ApiParam("开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(lifetimePredictionService.getPredictionHistory(
                stationId, inverterId, startDate, endDate));
    }

    @GetMapping("/health/history")
    @ApiOperation("查询健康度历史数据")
    public Result<List<InverterHealth>> getHealthHistory(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId,
            @ApiParam("开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(lifetimePredictionService.getHealthHistory(
                stationId, inverterId, startDate, endDate));
    }

    @PostMapping("/train")
    @ApiOperation("触发寿命预测模型训练")
    public Result<Boolean> triggerModelTraining(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId) {
        return Result.success(lifetimePredictionService.triggerModelTraining(stationId, inverterId));
    }

    @GetMapping("/spare-part/advice")
    @ApiOperation("获取备件更换建议")
    public Result<SparePartAdviceVO> getSparePartAdvice(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId) {
        return Result.success(lifetimePredictionService.getSparePartAdvice(stationId, inverterId));
    }

    @GetMapping("/alert/list")
    @ApiOperation("查询寿命预警列表")
    public Result<List<LifetimeAlertVO>> queryAlerts(LifetimeAlertQueryDTO queryDTO) {
        return Result.success(lifetimePredictionService.queryAlerts(queryDTO));
    }

    @PostMapping("/alert/handle/{alertId}")
    @ApiOperation("处理寿命预警")
    public Result<LifetimeAlertVO> handleAlert(
            @ApiParam("预警ID") @PathVariable Long alertId,
            @ApiParam("状态：1-已处理 2-已忽略") @RequestParam Integer status,
            @ApiParam("处理备注") @RequestParam(required = false) String handleRemark) {
        return Result.success(lifetimePredictionService.handleAlert(alertId, status, handleRemark));
    }

    @GetMapping("/alert/pending/count")
    @ApiOperation("统计未处理预警数量")
    public Result<Integer> countPendingAlerts(
            @ApiParam("电站ID（可选）") @RequestParam(required = false) Long stationId) {
        return Result.success(lifetimePredictionService.countPendingAlerts(stationId));
    }

    @PostMapping("/health/calculate")
    @ApiOperation("计算每日健康度")
    public Result<InverterHealth> calculateDailyHealth(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID") @RequestParam Long inverterId,
            @ApiParam("日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(lifetimePredictionService.calculateDailyHealth(stationId, inverterId, date));
    }
}
