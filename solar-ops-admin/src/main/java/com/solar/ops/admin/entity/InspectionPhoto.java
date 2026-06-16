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
@TableName("inspection_photo")
@ApiModel(value = "InspectionPhoto对象", description = "巡检照片")
public class InspectionPhoto extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "照片编号")
    private String photoNo;

    @ApiModelProperty(value = "结果ID")
    private Long resultId;

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

    @ApiModelProperty(value = "拍摄位置经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "拍摄位置纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "是否有水印 0-否 1-是")
    private Integer hasWatermark;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "是否离线拍摄 0-否 1-是")
    private Integer isOffline;

    @ApiModelProperty(value = "同步状态 0-待同步 1-已同步 2-同步失败")
    private Integer syncStatus;
}
