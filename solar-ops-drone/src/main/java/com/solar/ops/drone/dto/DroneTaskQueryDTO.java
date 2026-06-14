package com.solar.ops.drone.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检任务查询DTO")
public class DroneTaskQueryDTO {

    @ApiModelProperty(value = "任务编号")
    private String taskCode;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "状态 0-待执行 1-执行中 2-已完成 3-已取消 4-异常")
    private Integer status;

    @ApiModelProperty(value = "飞手")
    private String pilot;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}
