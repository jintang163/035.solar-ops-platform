package com.solar.ops.analysis.controller;

import com.solar.ops.analysis.dto.*;
import com.solar.ops.analysis.entity.CleaningPlan;
import com.solar.ops.analysis.entity.CleaningReminder;
import com.solar.ops.analysis.entity.DustAccumulationRecord;
import com.solar.ops.analysis.service.*;
import com.solar.ops.analysis.vo.*;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cleaning")
@Api(tags = "清洁提醒与清洗计划接口")
public class CleaningController {

    @Autowired
    private DustAccumulationService dustAccumulationService;

    @Autowired
    private CleaningReminderService cleaningReminderService;

    @Autowired
    private CleaningPlanService cleaningPlanService;

    @Autowired
    private CleaningStatisticsService cleaningStatisticsService;

    // ==================== 积灰检测记录接口 ====================

    @PostMapping("/dust/page")
    @ApiOperation("分页查询积灰检测记录")
    public Result<PageResult<DustRecordVO>> queryDustRecordPage(
            @RequestBody DustRecordQueryDTO queryDTO) {
        return Result.success(dustAccumulationService.queryDustRecordPage(queryDTO));
    }

    @PostMapping("/dust/list")
    @ApiOperation("查询积灰检测记录列表")
    public Result<List<DustRecordVO>> queryDustRecordList(
            @RequestBody DustRecordQueryDTO queryDTO) {
        return Result.success(dustAccumulationService.queryDustRecordList(queryDTO));
    }

    @PostMapping("/dust/detect")
    @ApiOperation("手动触发积灰检测（指定电站和日期）")
    public Result<List<DustAccumulationRecord>> detectDustAccumulation(
            @ApiParam(value = "电站ID", required = true) @RequestParam Long stationId,
            @ApiParam(value = "检测日期", required = true) @RequestParam LocalDate detectDate) {
        return Result.success(dustAccumulationService.detectDustAccumulation(stationId, detectDate));
    }

    // ==================== 清洗提醒接口 ====================

    @PostMapping("/reminder/page")
    @ApiOperation("分页查询清洗提醒")
    public Result<PageResult<CleaningReminderVO>> queryReminderPage(
            @RequestBody CleaningReminderQueryDTO queryDTO) {
        return Result.success(cleaningReminderService.queryReminderPage(queryDTO));
    }

    @PostMapping("/reminder/list")
    @ApiOperation("查询清洗提醒列表")
    public Result<List<CleaningReminderVO>> queryReminderList(
            @RequestBody CleaningReminderQueryDTO queryDTO) {
        return Result.success(cleaningReminderService.queryReminderList(queryDTO));
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("查询清洗提醒详情")
    public Result<CleaningReminderVO> getReminderDetail(
            @ApiParam(value = "提醒ID", required = true) @PathVariable Long id) {
        return Result.success(cleaningReminderService.getReminderDetail(id));
    }

    @PostMapping("/reminder/generate")
    @ApiOperation("手动生成清洗建议（根据积灰记录）")
    public Result<List<CleaningReminder>> generateReminders(
            @ApiParam(value = "检测日期", required = true) @RequestParam LocalDate detectDate) {
        return Result.success(cleaningReminderService.generateCleaningReminders(detectDate));
    }

    @PostMapping("/reminder/ignore/{id}")
    @ApiOperation("忽略清洗提醒")
    public Result<Void> ignoreReminder(
            @ApiParam(value = "提醒ID", required = true) @PathVariable Long id,
            @ApiParam(value = "操作人ID") @RequestParam(required = false) Long handlerId,
            @ApiParam(value = "操作人姓名") @RequestParam(required = false) String handlerName) {
        cleaningReminderService.ignoreReminder(id, handlerId, handlerName);
        return Result.success();
    }

    // ==================== 清洗计划接口 ====================

    @PostMapping("/plan/create")
    @ApiOperation("创建清洗计划")
    public Result<CleaningPlan> createPlan(
            @RequestBody @Validated CleaningPlanCreateDTO dto,
            @ApiParam(value = "创建人ID") @RequestParam(required = false) Long creatorId,
            @ApiParam(value = "创建人姓名") @RequestParam(required = false) String creatorName) {
        return Result.success(cleaningPlanService.createPlan(dto, creatorId, creatorName));
    }

    @PostMapping("/plan/update")
    @ApiOperation("更新清洗计划")
    public Result<CleaningPlan> updatePlan(
            @RequestBody @Validated CleaningPlanCreateDTO dto,
            @ApiParam(value = "操作人ID") @RequestParam(required = false) Long operatorId,
            @ApiParam(value = "操作人姓名") @RequestParam(required = false) String operatorName) {
        return Result.success(cleaningPlanService.updatePlan(dto, operatorId, operatorName));
    }

    @PostMapping("/plan/page")
    @ApiOperation("分页查询清洗计划")
    public Result<PageResult<CleaningPlanVO>> queryPlanPage(
            @RequestBody CleaningPlanQueryDTO queryDTO) {
        return Result.success(cleaningPlanService.queryPlanPage(queryDTO));
    }

    @PostMapping("/plan/list")
    @ApiOperation("查询清洗计划列表")
    public Result<List<CleaningPlanVO>> queryPlanList(
            @RequestBody CleaningPlanQueryDTO queryDTO) {
        return Result.success(cleaningPlanService.queryPlanList(queryDTO));
    }

    @GetMapping("/plan/calendar")
    @ApiOperation("按日期范围查询清洗计划（用于日历视图）")
    public Result<List<CleaningPlanVO>> queryPlansByDateRange(
            @ApiParam(value = "开始日期", required = true) @RequestParam LocalDate startDate,
            @ApiParam(value = "结束日期", required = true) @RequestParam LocalDate endDate,
            @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId) {
        return Result.success(cleaningPlanService.queryPlansByDateRange(startDate, endDate, stationId));
    }

    @GetMapping("/plan/{id}")
    @ApiOperation("查询清洗计划详情")
    public Result<CleaningPlanVO> getPlanDetail(
            @ApiParam(value = "计划ID", required = true) @PathVariable Long id) {
        return Result.success(cleaningPlanService.getPlanDetail(id));
    }

    @PostMapping("/plan/start")
    @ApiOperation("开始执行清洗计划")
    public Result<Void> startExecution(
            @RequestBody @Validated CleaningPlanExecuteDTO dto) {
        cleaningPlanService.startExecution(dto);
        return Result.success();
    }

    @PostMapping("/plan/complete")
    @ApiOperation("完成清洗计划（上传清洗后照片，统计提升发电量）")
    public Result<Void> completeExecution(
            @RequestBody @Validated CleaningPlanExecuteDTO dto) {
        cleaningPlanService.completeExecution(dto);
        return Result.success();
    }

    @PostMapping("/plan/cancel/{id}")
    @ApiOperation("取消清洗计划")
    public Result<Void> cancelPlan(
            @ApiParam(value = "计划ID", required = true) @PathVariable Long id,
            @ApiParam(value = "操作人ID") @RequestParam(required = false) Long operatorId,
            @ApiParam(value = "操作人姓名") @RequestParam(required = false) String operatorName,
            @ApiParam(value = "取消原因") @RequestParam(required = false, defaultValue = "") String reason) {
        cleaningPlanService.cancelPlan(id, operatorId, operatorName, reason);
        return Result.success();
    }

    @PostMapping("/plan/upload-photos")
    @ApiOperation("上传清洗前后照片")
    public Result<Void> uploadPhotos(
            @ApiParam(value = "计划ID", required = true) @RequestParam Long planId,
            @ApiParam(value = "清洗前照片URL（多个逗号分隔）") @RequestParam(required = false) String beforePhotos,
            @ApiParam(value = "清洗后照片URL（多个逗号分隔）") @RequestParam(required = false) String afterPhotos) {
        cleaningPlanService.uploadPhotos(planId, beforePhotos, afterPhotos);
        return Result.success();
    }

    // ==================== 统计仪表盘接口 ====================

    @GetMapping("/statistics/dashboard")
    @ApiOperation("获取清洗统计仪表盘数据")
    public Result<CleaningDashboardVO> getDashboardStatistics(
            @ApiParam(value = "电站ID（可选，不传则查全部）") @RequestParam(required = false) Long stationId) {
        return Result.success(cleaningStatisticsService.getDashboardStatistics(stationId));
    }

    @GetMapping("/statistics/dust-level")
    @ApiOperation("获取积灰等级分布统计")
    public Result<List<DustLevelStatVO>> getDustLevelStats(
            @ApiParam(value = "电站ID（可选）") @RequestParam(required = false) Long stationId) {
        return Result.success(cleaningStatisticsService.getDustLevelStats(stationId));
    }

    @GetMapping("/statistics/trend")
    @ApiOperation("获取近30天清洗提升趋势")
    public Result<List<CleaningTrendVO>> getImprovementTrend(
            @ApiParam(value = "电站ID（可选）") @RequestParam(required = false) Long stationId) {
        return Result.success(cleaningStatisticsService.getImprovementTrend(stationId, LocalDate.now()));
    }

    @GetMapping("/statistics/rank")
    @ApiOperation("获取各电站清洗排名")
    public Result<List<StationCleaningRankVO>> getStationCleaningRank(
            @ApiParam(value = "电站ID（可选，传则只查该电站）") @RequestParam(required = false) Long stationId) {
        return Result.success(cleaningStatisticsService.getStationCleaningRank(stationId));
    }
}
