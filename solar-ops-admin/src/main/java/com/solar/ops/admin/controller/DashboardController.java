package com.solar.ops.admin.controller;

import com.solar.ops.admin.service.DashboardService;
import com.solar.ops.admin.vo.DashboardRealTimeVO;
import com.solar.ops.admin.vo.InverterMonitorVO;
import com.solar.ops.admin.vo.MobileDashboardVO;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Api(tags = "大屏驾驶舱")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/realtime")
    @ApiOperation(value = "获取大屏实时数据")
    public Result<DashboardRealTimeVO> getRealTimeDashboard() {
        DashboardRealTimeVO data = dashboardService.getRealTimeDashboard();
        return Result.success(data);
    }

    @GetMapping("/inverter/station/{stationId}")
    @ApiOperation(value = "获取电站下的逆变器监控数据")
    public Result<List<InverterMonitorVO>> getInverterMonitorByStation(
            @ApiParam(value = "电站ID", required = true) @PathVariable Long stationId) {
        List<InverterMonitorVO> data = dashboardService.getInverterMonitorByStation(stationId);
        return Result.success(data);
    }

    @GetMapping("/mobile")
    @ApiOperation(value = "获取移动端驾驶舱数据")
    public Result<MobileDashboardVO> getMobileDashboard() {
        MobileDashboardVO data = dashboardService.getMobileDashboard();
        return Result.success(data);
    }
}
