package com.solar.ops.workorder.controller;

import com.solar.ops.common.result.Result;
import com.solar.ops.workorder.entity.OperatorLocation;
import com.solar.ops.workorder.service.DispatchService;
import com.solar.ops.workorder.service.WorkOrderService;
import com.solar.ops.workorder.vo.OperatorRecommendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workorder/dispatch")
@Api(tags = "工单智能调度")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;
    private final WorkOrderService workOrderService;

    @PostMapping("/location/report")
    @ApiOperation("上报运维人员位置")
    public Result<Boolean> reportLocation(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        String userName = params.get("userName") != null ? params.get("userName").toString() : null;
        BigDecimal longitude = params.get("longitude") != null ? new BigDecimal(params.get("longitude").toString()) : null;
        BigDecimal latitude = params.get("latitude") != null ? new BigDecimal(params.get("latitude").toString()) : null;
        BigDecimal accuracy = params.get("accuracy") != null ? new BigDecimal(params.get("accuracy").toString()) : null;
        BigDecimal speed = params.get("speed") != null ? new BigDecimal(params.get("speed").toString()) : null;
        BigDecimal heading = params.get("heading") != null ? new BigDecimal(params.get("heading").toString()) : null;
        String locationType = params.get("locationType") != null ? params.get("locationType").toString() : null;

        if (userId == null || longitude == null || latitude == null) {
            return Result.error("参数不完整");
        }

        boolean success = dispatchService.reportLocation(
                userId, userName, longitude, latitude, accuracy, speed, heading, locationType
        );

        return Result.success(success);
    }

    @GetMapping("/operators/recommend")
    @ApiOperation("推荐最优接单人员")
    public Result<List<OperatorRecommendVO>> recommendOperators(
            @ApiParam("工单ID") @RequestParam(required = false) Long orderId,
            @ApiParam("电站ID") @RequestParam(required = false) Long stationId,
            @ApiParam("电站经度") @RequestParam(required = false) BigDecimal stationLng,
            @ApiParam("电站纬度") @RequestParam(required = false) BigDecimal stationLat,
            @ApiParam("所需技能") @RequestParam(required = false) String requiredSkill,
            @ApiParam("故障级别") @RequestParam(required = false) Integer faultLevel) {

        List<OperatorRecommendVO> list = dispatchService.recommendByOrder(
                orderId, stationId, stationLng, stationLat, requiredSkill, faultLevel
        );

        return Result.success(list);
    }

    @GetMapping("/operators/locations")
    @ApiOperation("获取所有运维人员最新位置")
    public Result<List<OperatorLocation>> getAllLocations(
            @ApiParam("电站ID") @RequestParam(required = false) Long stationId) {

        List<OperatorLocation> list = dispatchService.getAllLatestLocations(stationId);
        return Result.success(list);
    }

    @GetMapping("/operators/{userId}/location")
    @ApiOperation("获取指定运维人员最新位置")
    public Result<OperatorLocation> getLocation(
            @ApiParam("运维人员ID") @PathVariable Long userId) {

        OperatorLocation location = dispatchService.getLatestLocation(userId);
        return Result.success(location);
    }

    @PostMapping("/assign")
    @ApiOperation("一键派单")
    public Result<Boolean> assignOrder(@RequestBody Map<String, Object> params) {
        Long orderId = params.get("orderId") != null ? Long.valueOf(params.get("orderId").toString()) : null;
        Long handlerId = params.get("handlerId") != null ? Long.valueOf(params.get("handlerId").toString()) : null;
        String handlerName = params.get("handlerName") != null ? params.get("handlerName").toString() : null;
        Long operatorId = params.get("operatorId") != null ? Long.valueOf(params.get("operatorId").toString()) : null;
        String operatorName = params.get("operatorName") != null ? params.get("operatorName").toString() : null;

        if (orderId == null || handlerId == null) {
            return Result.error("参数不完整");
        }

        boolean success = workOrderService.assignOrder(
                orderId, handlerId, handlerName, operatorId, operatorName
        );

        return Result.success(success);
    }

    @PostMapping("/autoAssign")
    @ApiOperation("智能自动派单（自动选择最优人员）")
    public Result<Map<String, Object>> autoAssignOrder(@RequestBody Map<String, Object> params) {
        Long orderId = params.get("orderId") != null ? Long.valueOf(params.get("orderId").toString()) : null;
        Long stationId = params.get("stationId") != null ? Long.valueOf(params.get("stationId").toString()) : null;
        BigDecimal stationLng = params.get("stationLng") != null ? new BigDecimal(params.get("stationLng").toString()) : null;
        BigDecimal stationLat = params.get("stationLat") != null ? new BigDecimal(params.get("stationLat").toString()) : null;
        String requiredSkill = params.get("requiredSkill") != null ? params.get("requiredSkill").toString() : null;
        Integer faultLevel = params.get("faultLevel") != null ? Integer.valueOf(params.get("faultLevel").toString()) : null;
        Long operatorId = params.get("operatorId") != null ? Long.valueOf(params.get("operatorId").toString()) : null;
        String operatorName = params.get("operatorName") != null ? params.get("operatorName").toString() : null;

        if (orderId == null) {
            return Result.error("工单ID不能为空");
        }

        List<OperatorRecommendVO> recommendations = dispatchService.recommendByOrder(
                orderId, stationId, stationLng, stationLat, requiredSkill, faultLevel
        );

        if (recommendations.isEmpty()) {
            return Result.error("附近没有可用的运维人员");
        }

        OperatorRecommendVO bestOperator = recommendations.get(0);
        boolean success = workOrderService.assignOrder(
                orderId, bestOperator.getUserId(), bestOperator.getUserName(),
                operatorId, operatorName
        );

        Map<String, Object> result = Map.of(
                "success", success,
                "operator", bestOperator
        );

        return Result.success(result);
    }
}
