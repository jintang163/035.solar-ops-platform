package com.solar.ops.admin.controller;

import com.solar.ops.admin.dto.UserStationAssignDTO;
import com.solar.ops.admin.entity.SysUserStation;
import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.service.SysUserStationService;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api")
@Api(tags = "用户电站权限管理")
public class SysUserStationController {

    @Resource
    private SysUserStationService sysUserStationService;

    @Resource
    private LoginUserHolder loginUserHolder;

    @GetMapping("/user-stations/{userId}")
    @ApiOperation(value = "获取用户电站权限列表")
    public Result<List<SysUserStation>> getByUserId(@ApiParam(value = "用户ID") @PathVariable Long userId) {
        List<SysUserStation> list = sysUserStationService.getByUserId(userId);
        return Result.success(list);
    }

    @PostMapping("/user-stations/assign")
    @ApiOperation(value = "分配用户电站权限")
    public Result<Void> assign(@RequestBody UserStationAssignDTO dto) {
        sysUserStationService.assignStations(dto);
        return Result.success();
    }
}
