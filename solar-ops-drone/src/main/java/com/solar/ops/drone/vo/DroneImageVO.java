package com.solar.ops.drone.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "巡检图像VO")
public class DroneImageVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "图片名称")
    private String imageName;

    @ApiModelProperty(value = "图片路径")
    private String imagePath;

    @ApiModelProperty(value = "缩略图路径")
    private String thumbnailPath;

    @ApiModelProperty(value = "标注后图片路径")
    private String annotatedPath;

    @ApiModelProperty(value = "图像类型")
    private String imageType;

    @ApiModelProperty(value = "拍摄经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "拍摄纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "飞行高度")
    private BigDecimal altitude;

    @ApiModelProperty(value = "拍摄时间")
    private LocalDateTime shootTime;

    @ApiModelProperty(value = "检测状态")
    private Integer detectStatus;

    @ApiModelProperty(value = "检测状态描述")
    private String detectStatusDesc;

    @ApiModelProperty(value = "检测缺陷数量")
    private Integer defectCount;

    @ApiModelProperty(value = "检测时间")
    private LocalDateTime detectTime;

    @ApiModelProperty(value = "缺陷列表")
    private List<DroneDefectVO> defects;
}
