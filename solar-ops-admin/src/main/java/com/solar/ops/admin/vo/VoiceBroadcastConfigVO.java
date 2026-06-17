package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "语音播报配置VO")
public class VoiceBroadcastConfigVO {

    @ApiModelProperty(value = "是否启用语音播报")
    private Boolean enabled;

    @ApiModelProperty(value = "启用播报的告警级别（>=此级别播报），默认3-高级")
    private Integer minAlarmLevel;

    @ApiModelProperty(value = "启用播报的类型列表")
    private List<Integer> enabledTypes;

    @ApiModelProperty(value = "播报音量 0-100")
    private Integer volume;

    @ApiModelProperty(value = "播报语速 0-100")
    private Integer speed;

    @ApiModelProperty(value = "播报人声音")
    private String voiceName;

    @ApiModelProperty(value = "播报时段开始，如 08:00")
    private String broadcastStartTime;

    @ApiModelProperty(value = "播报时段结束，如 20:00")
    private String broadcastEndTime;

    @ApiModelProperty(value = "夜间是否播报")
    private Boolean nightBroadcast;
}
