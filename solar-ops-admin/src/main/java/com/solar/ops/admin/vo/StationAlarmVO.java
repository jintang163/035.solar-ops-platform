package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "电站告警VO")
public class StationAlarmVO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "告警数量")
    private Integer alarmCount;

    @ApiModelProperty(value = "最高告警级别")
    private Integer maxAlarmLevel;

    @ApiModelProperty(value = "健康颜色")
    private String healthColor;
}
