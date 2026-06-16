package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_task")
@ApiModel(value = "InspectionTask对象", description = "巡检任务")
public class InspectionTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务编号")
    private String taskNo;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "任务类型 1-日常巡检 2-专项巡检 3-定期检修")
    private Integer taskType;

    @ApiModelProperty(value = "优先级 1-低 2-中 3-高")
    private Integer priority;

    @ApiModelProperty(value = "计划开始时间")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    private LocalDateTime planEndTime;

    @ApiModelProperty(value = "实际开始时间")
    private LocalDateTime actualStartTime;

    @ApiModelProperty(value = "实际结束时间")
    private LocalDateTime actualEndTime;

    @ApiModelProperty(value = "状态 0-待下载 1-已下载 2-执行中 3-已完成 4-已取消")
    private Integer status;

    @ApiModelProperty(value = "指派人员ID")
    private Long assigneeId;

    @ApiModelProperty(value = "指派人员姓名")
    private String assigneeName;

    @ApiModelProperty(value = "任务描述")
    private String description;

    @ApiModelProperty(value = "备注")
    private String remark;
}
