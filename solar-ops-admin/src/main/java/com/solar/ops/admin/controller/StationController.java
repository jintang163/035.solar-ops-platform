package com.solar.ops.admin.controller;

import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.service.StationService;
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
@RequestMapping("/api/stations")
@Api(tags = "电站管理")
public class StationController {

    @Resource
    private StationService stationService;

    @GetMapping
    @ApiOperation(value = "分页查询电站列表")
    public Result<PageResult<Station>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
                                            @ApiParam(value = "状态") @RequestParam(required = false) Integer status) {
        PageResult<Station> pageResult = stationService.page(pageQuery, keyword, status);
        return Result.success(pageResult);
    }

    @GetMapping("/list")
    @ApiOperation(value = "获取所有启用的电站列表")
    public Result<List<Station>> listAll() {
        List<Station> list = stationService.listAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询电站")
    public Result<Station> getById(@ApiParam(value = "电站ID") @PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return Result.success(station);
    }

    @PostMapping
    @ApiOperation(value = "新增电站")
    public Result<Void> add(@RequestBody Station station) {
        stationService.addStation(station);
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "更新电站")
    public Result<Void> update(@RequestBody Station station) {
        stationService.updateStation(station);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除电站")
    public Result<Void> delete(@ApiParam(value = "电站ID") @PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success();
    }
}
