package com.solar.ops.admin.controller;

import com.solar.ops.admin.entity.GridDispatchCommand;
import com.solar.ops.admin.service.GridDispatchService;
import com.solar.ops.admin.vo.*;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/grid-dispatch")
@Api(tags = "电网调度对接")
public class GridDispatchController {

    @Resource
    private GridDispatchService gridDispatchService;

    @GetMapping("/summary")
    @ApiOperation("获取调度总览数据")
    public Result<GridDispatchSummaryVO> getSummary() {
        return Result.success(gridDispatchService.getDispatchSummary());
    }

    @GetMapping("/commands")
    @ApiOperation("分页查询调度指令")
    public Result<PageResult<GridDispatchCommandVO>> getCommands(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页条数", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @ApiParam(value = "指令类型") @RequestParam(required = false) Integer commandType,
            @ApiParam(value = "指令状态") @RequestParam(required = false) Integer status,
            @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId,
            @ApiParam(value = "关键词搜索") @RequestParam(required = false) String keyword) {
        return Result.success(gridDispatchService.getCommandPage(pageNum, pageSize, commandType, status, stationId, keyword));
    }

    @GetMapping("/commands/{id}")
    @ApiOperation("获取指令详情")
    public Result<GridDispatchCommandVO> getCommandDetail(@PathVariable Long id) {
        return Result.success(gridDispatchService.getCommandDetail(id));
    }

    @GetMapping("/commands/{id}/curve")
    @ApiOperation("获取指令执行曲线对比数据")
    public Result<List<GridDispatchCurveDataVO>> getCommandCurve(@PathVariable Long id) {
        return Result.success(gridDispatchService.getCommandCurveData(id));
    }

    @GetMapping("/upload-records")
    @ApiOperation("分页查询上传记录")
    public Result<PageResult<GridDispatchUploadRecordVO>> getUploadRecords(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页条数", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @ApiParam(value = "协议类型：1-IEC104 2-Modbus TCP") @RequestParam(required = false) Integer protocolType,
            @ApiParam(value = "上传状态：0-待上传 1-成功 2-失败") @RequestParam(required = false) Integer uploadStatus,
            @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId) {
        return Result.success(gridDispatchService.getUploadRecordPage(pageNum, pageSize, protocolType, uploadStatus, stationId));
    }

    @PostMapping("/commands/manual")
    @ApiOperation("创建人工调度指令")
    public Result<GridDispatchCommandVO> createManualCommand(@RequestBody GridDispatchCommand command) {
        return Result.success(gridDispatchService.createManualCommand(command, 1L, "admin"));
    }

    @PutMapping("/commands/{id}/cancel")
    @ApiOperation("取消调度指令")
    public Result<Void> cancelCommand(@PathVariable Long id) {
        gridDispatchService.cancelCommand(id, 1L, "admin");
        return Result.success();
    }

    @GetMapping("/protocol-configs")
    @ApiOperation("获取协议配置列表")
    public Result<List<GridDispatchProtocolConfigVO>> getProtocolConfigs() {
        return Result.success(gridDispatchService.getProtocolConfigs());
    }

    @PostMapping("/protocol-configs/{id}/test")
    @ApiOperation("测试协议连接")
    public Result<Boolean> testProtocolConnection(@PathVariable Long id) {
        return Result.success(gridDispatchService.testProtocolConnection(id));
    }
}
