package com.solar.ops.admin.controller;

import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.service.InverterService;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/inverters")
@Api(tags = "逆变器管理")
public class InverterController {

    @Resource
    private InverterService inverterService;

    @GetMapping
    @ApiOperation(value = "分页查询逆变器列表")
    public Result<PageResult<Inverter>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                             @ApiParam(value = "电站ID") @RequestParam(required = false) Long stationId,
                                             @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
                                             @ApiParam(value = "状态") @RequestParam(required = false) Integer status,
                                             @ApiParam(value = "在线状态") @RequestParam(required = false) Integer onlineStatus) {
        PageResult<Inverter> pageResult = inverterService.page(pageQuery, stationId, keyword, status, onlineStatus);
        return Result.success(pageResult);
    }

    @GetMapping("/list")
    @ApiOperation(value = "根据电站ID查询逆变器列表")
    public Result<List<Inverter>> listByStationId(@ApiParam(value = "电站ID") @RequestParam Long stationId) {
        List<Inverter> list = inverterService.listByStationId(stationId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询逆变器")
    public Result<Inverter> getById(@ApiParam(value = "逆变器ID") @PathVariable Long id) {
        Inverter inverter = inverterService.getInverterById(id);
        return Result.success(inverter);
    }

    @PostMapping
    @ApiOperation(value = "新增逆变器")
    public Result<Void> add(@RequestBody Inverter inverter) {
        inverterService.addInverter(inverter);
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "更新逆变器")
    public Result<Void> update(@RequestBody Inverter inverter) {
        inverterService.updateInverter(inverter);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除逆变器")
    public Result<Void> delete(@ApiParam(value = "逆变器ID") @PathVariable Long id) {
        inverterService.deleteInverter(id);
        return Result.success();
    }
}
