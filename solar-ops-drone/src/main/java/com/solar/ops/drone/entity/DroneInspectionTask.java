package com.solar.ops.drone.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drone_inspection_task")
@ApiModel(value = "无人机巡检任务实体")
public class DroneInspectionTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务编号")
    private String taskCode;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "巡检区域")
    private String area;

    @ApiModelProperty(value = "飞行模式 manual-手动 auto-自动 waypoint-航点")
    private String flightMode;

    @ApiModelProperty(value = "无人机编号")
    private String droneCode;

    @ApiModelProperty(value = "飞手")
    private String pilot;

    @ApiModelProperty(value = "巡检时间")
    private LocalDateTime inspectionTime;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "状态 0-待执行 1-执行中 2-已完成 3-已取消 4-异常")
    private Integer status;

    @ApiModelProperty(value = "拍摄图片数量")
    private Integer imageCount;

    @ApiModelProperty(value = "已检测图片数量")
    private Integer detectedImageCount;

    @ApiModelProperty(value = "检测缺陷数量")
    private Integer defectCount;

    @ApiModelProperty(value = "已确认缺陷数量")
    private Integer confirmedDefectCount;

    @ApiModelProperty(value = "生成工单数量")
    private Integer workorderCount;

    @ApiModelProperty(value = "任务描述")
    private String description;

    @ApiModelProperty(value = "创建人")
    private Long createBy;

    @ApiModelProperty(value = "巡检人员")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;
}
