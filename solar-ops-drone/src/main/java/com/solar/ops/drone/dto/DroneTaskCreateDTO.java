package com.solar.ops.drone.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "巡检任务创建DTO")
public class DroneTaskCreateDTO {

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @NotNull(message = "电站ID不能为空")
    @ApiModelProperty(value = "电站ID", required = true)
    private Long stationId;

    @ApiModelProperty(value = "巡检区域")
    private String area;

    @ApiModelProperty(value = "飞行模式 manual-手动 auto-自动 waypoint-航点")
    private String flightMode;

    @ApiModelProperty(value = "无人机编号")
    private String droneCode;

    @ApiModelProperty(value = "飞手")
    private String pilot;

    @ApiModelProperty(value = "任务描述")
    private String description;
}
