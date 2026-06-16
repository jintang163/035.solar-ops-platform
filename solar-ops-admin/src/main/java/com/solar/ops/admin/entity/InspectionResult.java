package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_result")
@ApiModel(value = "InspectionResult对象", description = "巡检结果")
public class InspectionResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "结果编号")
    private String resultNo;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "任务编号")
    private String taskNo;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "巡检人员ID")
    private Long inspectorId;

    @ApiModelProperty(value = "巡检人员姓名")
    private String inspectorName;

    @ApiModelProperty(value = "巡检开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "巡检结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "总检查项数")
    private Integer totalItems;

    @ApiModelProperty(value = "正常项数")
    private Integer normalItems;

    @ApiModelProperty(value = "异常项数")
    private Integer abnormalItems;

    @ApiModelProperty(value = "结果状态 1-正常 2-异常 3-待复核")
    private Integer resultStatus;

    @ApiModelProperty(value = "总体评价")
    private String overallRemark;

    @ApiModelProperty(value = "巡检位置经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "巡检位置纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "是否离线提交 0-否 1-是")
    private Integer isOffline;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime uploadTime;

    @ApiModelProperty(value = "同步状态 0-待同步 1-已同步 2-同步失败")
    private Integer syncStatus;
}
