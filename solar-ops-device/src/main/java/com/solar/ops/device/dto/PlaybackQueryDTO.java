package com.solar.ops.device.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据回放查询DTO
 */
@Data
@ApiModel(value = "数据回放查询参数")
public class PlaybackQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "设备ID（必需）", required = true)
    private String deviceId;

    @ApiModelProperty(value = "开始时间（毫秒时间戳）")
    private Long startTime;

    @ApiModelProperty(value = "结束时间（毫秒时间戳）")
    private Long endTime;

    @ApiModelProperty(value = "查询指标列表：power/voltage/temperature/current")
    private List<String> metrics;

    @ApiModelProperty(value = "聚合方式：mean/max/min，默认mean")
    private String aggregation = "mean";

    @ApiModelProperty(value = "时间间隔：1m/5m/15m/1h，根据时长自动计算")
    private String interval;
}
