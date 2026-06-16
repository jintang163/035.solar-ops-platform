package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检任务查询DTO")
public class InspectionTaskQueryDTO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "任务状态")
    private Integer status;

    @ApiModelProperty(value = "任务类型")
    private Integer taskType;

    @ApiModelProperty(value = "优先级")
    private Integer priority;

    @ApiModelProperty(value = "关键词")
    private String keyword;

    @ApiModelProperty(value = "计划开始时间-开始")
    private LocalDateTime planStartTimeStart;

    @ApiModelProperty(value = "计划开始时间-结束")
    private LocalDateTime planStartTimeEnd;
}
