package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "巡检结果提交DTO")
public class InspectionResultSubmitDTO {

    @ApiModelProperty(value = "任务ID", required = true)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @ApiModelProperty(value = "任务编号")
    private String taskNo;

    @ApiModelProperty(value = "电站ID", required = true)
    @NotNull(message = "电站ID不能为空")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "巡检开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "巡检结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "总体评价")
    private String overallRemark;

    @ApiModelProperty(value = "经度")
    private java.math.BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private java.math.BigDecimal latitude;

    @ApiModelProperty(value = "是否离线提交")
    private Integer isOffline;

    @ApiModelProperty(value = "检查项结果列表")
    private List<InspectionResultItemDTO> items;

    @ApiModelProperty(value = "照片列表")
    private List<InspectionPhotoDTO> photos;

    @ApiModelProperty(value = "录音列表")
    private List<InspectionAudioDTO> audios;
}
