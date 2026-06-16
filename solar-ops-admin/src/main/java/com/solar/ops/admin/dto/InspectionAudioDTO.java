package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检录音DTO")
public class InspectionAudioDTO {

    @ApiModelProperty(value = "结果明细ID")
    private Long resultItemId;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "录音URL")
    private String audioUrl;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "录音时长(秒)")
    private Integer duration;

    @ApiModelProperty(value = "录制时间")
    private LocalDateTime recordTime;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "是否离线录制")
    private Integer isOffline;
}
