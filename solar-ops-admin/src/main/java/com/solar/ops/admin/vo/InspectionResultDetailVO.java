package com.solar.ops.admin.vo;

import com.solar.ops.admin.entity.InspectionResultItem;
import com.solar.ops.admin.entity.InspectionPhoto;
import com.solar.ops.admin.entity.InspectionAudio;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "巡检结果详情VO")
public class InspectionResultDetailVO {

    @ApiModelProperty(value = "结果ID")
    private Long id;

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
    private java.time.LocalDateTime startTime;

    @ApiModelProperty(value = "巡检结束时间")
    private java.time.LocalDateTime endTime;

    @ApiModelProperty(value = "总检查项数")
    private Integer totalItems;

    @ApiModelProperty(value = "正常项数")
    private Integer normalItems;

    @ApiModelProperty(value = "异常项数")
    private Integer abnormalItems;

    @ApiModelProperty(value = "结果状态")
    private Integer resultStatus;

    @ApiModelProperty(value = "总体评价")
    private String overallRemark;

    @ApiModelProperty(value = "结果明细列表")
    private List<InspectionResultItem> items;

    @ApiModelProperty(value = "照片列表")
    private List<InspectionPhoto> photos;

    @ApiModelProperty(value = "录音列表")
    private List<InspectionAudio> audios;
}
