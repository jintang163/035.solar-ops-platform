package com.solar.ops.workorder.controller;

import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import com.solar.ops.workorder.dto.WorkOrderCreateDTO;
import com.solar.ops.workorder.dto.WorkOrderHandleDTO;
import com.solar.ops.workorder.dto.WorkOrderQueryDTO;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.service.WorkOrderService;
import com.solar.ops.workorder.vo.WorkOrderStatisticsVO;
import com.solar.ops.workorder.vo.WorkOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workorder")
@Api(tags = "工单管理")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping("/create")
    @ApiOperation("创建工单")
    public Result<WorkOrder> create(@RequestBody @Validated WorkOrderCreateDTO dto) {
        WorkOrder workOrder = workOrderService.createWorkOrder(dto);
        return Result.success(workOrder);
    }

    @PostMapping("/page")
    @ApiOperation("工单分页查询")
    public Result<PageResult<WorkOrderVO>> page(@RequestBody WorkOrderQueryDTO dto) {
        PageResult<WorkOrderVO> page = workOrderService.page(dto);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @ApiOperation("工单详情")
    public Result<WorkOrderVO> detail(@ApiParam(value = "工单ID", required = true) @PathVariable Long id) {
        WorkOrderVO vo = workOrderService.getDetail(id);
        return Result.success(vo);
    }

    @PostMapping("/accept")
    @ApiOperation("接单")
    public Result<Void> accept(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.acceptOrder(dto.getOrderId(), dto.getOperatorId(), dto.getOperatorName());
        return Result.success();
    }

    @PostMapping("/assign")
    @ApiOperation("派单")
    public Result<Void> assign(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.assignOrder(dto.getOrderId(), dto.getHandlerId(), dto.getHandlerName(),
                dto.getOperatorId(), dto.getOperatorName());
        return Result.success();
    }

    @PostMapping("/startProcess")
    @ApiOperation("开始处理")
    public Result<Void> startProcess(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.startProcess(dto);
        return Result.success();
    }

    @PostMapping("/submitCheck")
    @ApiOperation("提交验收")
    public Result<Void> submitCheck(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.submitCheck(dto);
        return Result.success();
    }

    @PostMapping("/complete")
    @ApiOperation("验收通过/完成工单")
    public Result<Void> complete(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.completeOrder(dto);
        return Result.success();
    }

    @PostMapping("/reject")
    @ApiOperation("验收驳回")
    public Result<Void> reject(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.rejectCheck(dto);
        return Result.success();
    }

    @PostMapping("/close")
    @ApiOperation("关闭工单")
    public Result<Void> close(@RequestBody @Validated WorkOrderHandleDTO dto) {
        workOrderService.closeOrder(dto);
        return Result.success();
    }

    @GetMapping("/statistics")
    @ApiOperation("工单统计")
    public Result<WorkOrderStatisticsVO> statistics(@ApiParam("电站ID") @RequestParam(required = false) Long stationId) {
        WorkOrderStatisticsVO statistics = workOrderService.getStatistics(stationId);
        return Result.success(statistics);
    }
}
