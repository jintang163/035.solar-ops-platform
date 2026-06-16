package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "巡检照片DTO")
public class InspectionPhotoDTO {

    @ApiModelProperty(value = "结果明细ID")
    private Long resultItemId;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "资产ID")
    private Long assetId;

    @ApiModelProperty(value = "照片类型 1-普通照片 2-红外照片 3-仪表照片")
    private Integer photoType;

    @ApiModelProperty(value = "照片URL")
    private String photoUrl;

    @ApiModelProperty(value = "缩略图URL")
    private String thumbnailUrl;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "水印时间")
    private LocalDateTime watermarkTime;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "是否有水印")
    private Integer hasWatermark;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "是否离线拍摄")
    private Integer isOffline;
}
