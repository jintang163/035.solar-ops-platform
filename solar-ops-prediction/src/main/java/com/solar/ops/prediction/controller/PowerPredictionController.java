package com.solar.ops.prediction.controller;

import com.solar.ops.common.result.Result;
import com.solar.ops.prediction.dto.AlertQueryDTO;
import com.solar.ops.prediction.dto.PredictionQueryDTO;
import com.solar.ops.prediction.entity.PowerPrediction;
import com.solar.ops.prediction.entity.WeatherRecord;
import com.solar.ops.prediction.service.PowerPredictionService;
import com.solar.ops.prediction.vo.PredictionAlertVO;
import com.solar.ops.prediction.vo.PredictionCurveVO;
import com.solar.ops.prediction.vo.PredictionSummaryVO;
import com.solar.ops.prediction.vo.WeatherOverviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prediction")
@Api(tags = "功率预测接口")
public class PowerPredictionController {

    @Autowired
    private PowerPredictionService predictionService;

    @GetMapping("/curve")
    @ApiOperation("获取预测曲线（预测vs实发对比）")
    public Result<PredictionCurveVO> getPredictionCurve(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID（可选）") @RequestParam(required = false) Long inverterId,
            @ApiParam("小时数，默认6") @RequestParam(required = false, defaultValue = "6") Integer hours) {
        return Result.success(predictionService.getPredictionCurve(stationId, inverterId, hours));
    }

    @GetMapping("/summary/{stationId}")
    @ApiOperation("获取电站预测概览")
    public Result<PredictionSummaryVO> getPredictionSummary(
            @ApiParam("电站ID") @PathVariable Long stationId) {
        return Result.success(predictionService.getPredictionSummary(stationId));
    }

    @GetMapping("/list")
    @ApiOperation("查询预测记录列表")
    public Result<List<PowerPrediction>> queryPredictions(PredictionQueryDTO queryDTO) {
        return Result.success(predictionService.queryPredictions(queryDTO));
    }

    @PostMapping("/execute")
    @ApiOperation("手动触发功率预测")
    public Result<PredictionCurveVO> executePrediction(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID（可选）") @RequestParam(required = false) Long inverterId,
            @ApiParam("预测时域，默认6") @RequestParam(required = false, defaultValue = "6") Integer horizon) {
        return Result.success(predictionService.executePrediction(stationId, inverterId, horizon));
    }

    @PostMapping("/train")
    @ApiOperation("触发模型训练")
    public Result<Boolean> triggerModelTraining(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("逆变器ID（可选）") @RequestParam(required = false) Long inverterId) {
        return Result.success(predictionService.triggerModelTraining(stationId, inverterId));
    }

    @GetMapping("/weather/overview/{stationId}")
    @ApiOperation("获取气象概览（实时+未来小时预测）")
    public Result<WeatherOverviewVO> getWeatherOverview(
            @ApiParam("电站ID") @PathVariable Long stationId) {
        return Result.success(predictionService.getWeatherOverview(stationId));
    }

    @GetMapping("/weather/history")
    @ApiOperation("查询气象历史数据")
    public Result<List<WeatherRecord>> queryWeatherHistory(
            @ApiParam("电站ID") @RequestParam Long stationId,
            @ApiParam("查询小时数，默认24") @RequestParam(required = false, defaultValue = "24") Integer hours) {
        return Result.success(predictionService.queryWeatherHistory(stationId, hours));
    }

    @GetMapping("/alert/list")
    @ApiOperation("查询预测告警列表")
    public Result<List<PredictionAlertVO>> queryAlerts(AlertQueryDTO queryDTO) {
        return Result.success(predictionService.queryAlerts(queryDTO));
    }

    @PostMapping("/alert/handle/{alertId}")
    @ApiOperation("处理告警")
    public Result<PredictionAlertVO> handleAlert(
            @ApiParam("告警ID") @PathVariable Long alertId,
            @ApiParam("状态：1-已处理 2-已忽略") @RequestParam Integer status,
            @ApiParam("处理备注") @RequestParam(required = false) String remark,
            @ApiParam("根因：weather-天气 equipment-设备 other-其他") @RequestParam(required = false) String rootCause) {
        return Result.success(predictionService.handleAlert(alertId, status, remark, rootCause));
    }

    @GetMapping("/alert/pending/count")
    @ApiOperation("统计未处理告警数量")
    public Result<Integer> countPendingAlerts(
            @ApiParam("电站ID（可选）") @RequestParam(required = false) Long stationId) {
        return Result.success(predictionService.countPendingAlerts(stationId));
    }
}
