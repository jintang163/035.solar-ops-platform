package com.solar.ops.drone.entity;

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
@TableName("drone_inspection_image")
@ApiModel(value = "巡检图像实体")
public class DroneInspectionImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "巡检任务ID")
    private Long taskId;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "图片名称")
    private String imageName;

    @ApiModelProperty(value = "图片访问URL")
    private String imageUrl;

    @ApiModelProperty(value = "标注后图片URL")
    private String annotatedImageUrl;

    @ApiModelProperty(value = "图片存储路径")
    private String imagePath;

    @ApiModelProperty(value = "缩略图路径")
    private String thumbnailPath;

    @ApiModelProperty(value = "标注后图片路径")
    private String annotatedPath;

    @ApiModelProperty(value = "图像类型 visible-可见光 infrared-红外 thermal-热成像")
    private String imageType;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long imageSize;

    @ApiModelProperty(value = "拍摄经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "拍摄纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "飞行高度(米)")
    private BigDecimal altitude;

    @ApiModelProperty(value = "拍摄时间")
    private LocalDateTime shootTime;

    @ApiModelProperty(value = "相机型号")
    private String cameraModel;

    @ApiModelProperty(value = "图像宽度(px)")
    private Integer imageWidth;

    @ApiModelProperty(value = "图像高度(px)")
    private Integer imageHeight;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "检测状态 0-待检测 1-检测中 2-检测完成 3-检测失败")
    private Integer detectStatus;

    @ApiModelProperty(value = "检测开始时间")
    private LocalDateTime detectStartTime;

    @ApiModelProperty(value = "检测结束时间")
    private LocalDateTime detectEndTime;

    @ApiModelProperty(value = "AI检测结果JSON")
    private String detectResult;

    @ApiModelProperty(value = "检测缺陷数量")
    private Integer defectCount;

    @ApiModelProperty(value = "检测完成时间")
    private LocalDateTime detectTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
