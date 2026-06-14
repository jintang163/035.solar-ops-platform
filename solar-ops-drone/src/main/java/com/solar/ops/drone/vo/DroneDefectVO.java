package com.solar.ops.drone.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "缺陷VO")
public class DroneDefectVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "图像ID")
    private Long imageId;

    @ApiModelProperty(value = "缺陷编号")
    private String defectCode;

    @ApiModelProperty(value = "缺陷类型")
    private String defectType;

    @ApiModelProperty(value = "缺陷类型描述")
    private String defectTypeDesc;

    @ApiModelProperty(value = "缺陷等级")
    private Integer defectLevel;

    @ApiModelProperty(value = "缺陷等级描述")
    private String defectLevelDesc;

    @ApiModelProperty(value = "置信度")
    private BigDecimal confidence;

    @ApiModelProperty(value = "边界框")
    private int[] bbox;

    @ApiModelProperty(value = "中心点坐标")
    private int[] center;

    @ApiModelProperty(value = "温度(℃)")
    private BigDecimal temperature;

    @ApiModelProperty(value = "温度差(℃)")
    private BigDecimal deltaTemperature;

    @ApiModelProperty(value = "缺陷GPS经度")
    private BigDecimal gpsLongitude;

    @ApiModelProperty(value = "缺陷GPS纬度")
    private BigDecimal gpsLatitude;

    @ApiModelProperty(value = "组件编号")
    private String componentCode;

    @ApiModelProperty(value = "关联工单ID")
    private Long workorderId;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "缺陷描述")
    private String description;

    @ApiModelProperty(value = "处理建议")
    private String suggestion;

    @ApiModelProperty(value = "是否人工确认")
    private Integer verified;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
