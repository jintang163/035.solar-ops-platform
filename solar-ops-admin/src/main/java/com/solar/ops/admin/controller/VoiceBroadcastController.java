package com.solar.ops.admin.controller;

import com.solar.ops.admin.service.VoiceBroadcastService;
import com.solar.ops.admin.vo.VoiceBroadcastConfigVO;
import com.solar.ops.admin.vo.VoiceBroadcastRecordVO;
import com.solar.ops.admin.vo.VoiceSpeakerDeviceVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/voice-broadcast")
@Api(tags = "语音告警播报")
public class VoiceBroadcastController {

    @Resource
    private VoiceBroadcastService voiceBroadcastService;

    @GetMapping("/history")
    @ApiOperation(value = "分页查询播报历史记录")
    public Result<PageResult<VoiceBroadcastRecordVO>> getBroadcastHistory(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页条数", defaultValue = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @ApiParam(value = "播报类型") @RequestParam(required = false) Integer broadcastType,
            @ApiParam(value = "告警级别") @RequestParam(required = false) Integer alarmLevel,
            @ApiParam(value = "关键词搜索") @RequestParam(required = false) String keyword) {
        return Result.success(voiceBroadcastService.getBroadcastHistory(pageNum, pageSize, broadcastType, alarmLevel, keyword));
    }

    @GetMapping("/config")
    @ApiOperation(value = "获取语音播报配置")
    public Result<VoiceBroadcastConfigVO> getBroadcastConfig() {
        return Result.success(voiceBroadcastService.getBroadcastConfig());
    }

    @PutMapping("/config")
    @ApiOperation(value = "更新语音播报配置")
    public Result<Void> updateBroadcastConfig(@RequestBody VoiceBroadcastConfigVO config) {
        voiceBroadcastService.updateBroadcastConfig(config);
        return Result.success();
    }

    @PostMapping("/trigger/test")
    @ApiOperation(value = "测试语音播报")
    public Result<VoiceBroadcastRecordVO> testBroadcast(@RequestParam String content) {
        return Result.success(voiceBroadcastService.triggerTestBroadcast(content));
    }

    @PutMapping("/{id}/retry")
    @ApiOperation(value = "重新播报")
    public Result<Void> retryBroadcast(@PathVariable Long id) {
        voiceBroadcastService.retryBroadcast(id);
        return Result.success();
    }

    @GetMapping("/speakers")
    @ApiOperation(value = "获取音箱设备列表")
    public Result<List<VoiceSpeakerDeviceVO>> getSpeakerDevices() {
        return Result.success(voiceBroadcastService.getSpeakerDevices());
    }

    @PostMapping("/speakers/test")
    @ApiOperation(value = "测试音箱连接")
    public Result<Boolean> testSpeaker(@RequestParam String deviceId) {
        return Result.success(voiceBroadcastService.testSpeakerDevice(deviceId));
    }
}
