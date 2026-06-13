package com.solar.ops.device.controller;

import com.solar.ops.common.result.Result;
import com.solar.ops.device.dto.InverterDataDTO;
import com.solar.ops.device.service.DeviceDataService;
import com.solar.ops.device.websocket.DeviceDataWebSocket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "设备数据管理")
@RestController
@RequestMapping("/data")
public class DeviceDataController {

    @Autowired
    private DeviceDataService deviceDataService;

    @Autowired
    private DeviceDataWebSocket deviceDataWebSocket;

    @ApiOperation("获取设备实时数据")
    @GetMapping("/realtime/{deviceId}")
    public Result<InverterDataDTO> getRealtimeData(
            @ApiParam("设备ID") @PathVariable String deviceId) {
        InverterDataDTO data = deviceDataService.getRealtimeData(deviceId);
        if (data != null) {
            return Result.success(data);
        } else {
            return Result.fail("暂无实时数据");
        }
    }

    @ApiOperation("获取所有设备实时数据")
    @GetMapping("/realtime/all")
    public Result<Map<String, InverterDataDTO>> getAllRealtimeData() {
        Map<String, InverterDataDTO> dataMap = deviceDataService.getAllRealtimeData();
        return Result.success(dataMap);
    }

    @ApiOperation("获取设备历史数据")
    @GetMapping("/history/{deviceId}")
    public Result<List<InverterDataDTO>> getHistoryData(
            @ApiParam("设备ID") @PathVariable String deviceId,
            @ApiParam("开始时间(时间戳)") @RequestParam Long startTime,
            @ApiParam("结束时间(时间戳)") @RequestParam Long endTime,
            @ApiParam("返回条数限制") @RequestParam(required = false, defaultValue = "1000") Integer limit) {
        List<InverterDataDTO> dataList = deviceDataService.getHistoryData(deviceId, startTime, endTime, limit);
        return Result.success(dataList);
    }

    @ApiOperation("获取设备在线状态")
    @GetMapping("/status/{deviceId}")
    public Result<Map<String, Object>> getDeviceStatus(
            @ApiParam("设备ID") @PathVariable String deviceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("online", deviceDataService.isDeviceOnline(deviceId));
        return Result.success(result);
    }

    @ApiOperation("获取在线设备数量")
    @GetMapping("/online/count")
    public Result<Map<String, Object>> getOnlineDeviceCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("onlineCount", deviceDataService.getOnlineDeviceCount());
        result.put("websocketCount", deviceDataWebSocket.getOnlineCount());
        return Result.success(result);
    }

    @ApiOperation("模拟上报设备数据")
    @PostMapping("/mock")
    public Result<String> mockDeviceData(@RequestBody InverterDataDTO data) {
        if (data.getDeviceId() == null) {
            return Result.fail("设备ID不能为空");
        }
        if (data.getTimestamp() == null) {
            data.setTimestamp(System.currentTimeMillis());
        }
        deviceDataService.processDeviceData(data);
        return Result.success("模拟数据上报成功");
    }
}
