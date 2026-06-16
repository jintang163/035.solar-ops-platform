package com.solar.ops.admin.vo;

import com.solar.ops.admin.entity.InspectionTaskItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "巡检任务详情VO")
public class InspectionTaskDetailVO {

    @ApiModelProperty(value = "任务ID")
    private Long id;

    @ApiModelProperty(value = "任务编号")
    private String taskNo;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "任务类型")
    private Integer taskType;

    @ApiModelProperty(value = "优先级")
    private Integer priority;

    @ApiModelProperty(value = "计划开始时间")
    private java.time.LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    private java.time.LocalDateTime planEndTime;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "指派人员ID")
    private Long assigneeId;

    @ApiModelProperty(value = "指派人员姓名")
    private String assigneeName;

    @ApiModelProperty(value = "任务描述")
    private String description;

    @ApiModelProperty(value = "检查项列表")
    private List<TaskItemWithDetailVO> items;
}
