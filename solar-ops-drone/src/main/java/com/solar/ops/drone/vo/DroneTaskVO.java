package com.solar.ops.drone.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检任务VO")
public class DroneTaskVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "任务编号")
    private String taskCode;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "巡检区域")
    private String area;

    @ApiModelProperty(value = "飞行模式")
    private String flightMode;

    @ApiModelProperty(value = "无人机编号")
    private String droneCode;

    @ApiModelProperty(value = "飞手")
    private String pilot;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "状态 0-待执行 1-执行中 2-已完成 3-已取消 4-异常")
    private Integer status;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "拍摄图片数量")
    private Integer imageCount;

    @ApiModelProperty(value = "检测缺陷数量")
    private Integer defectCount;

    @ApiModelProperty(value = "生成工单数量")
    private Integer workorderCount;

    @ApiModelProperty(value = "任务描述")
    private String description;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
