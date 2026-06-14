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
@TableName("drone_defect")
@ApiModel(value = "缺陷识别实体")
public class DroneDefect extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "巡检任务ID")
    private Long taskId;

    @ApiModelProperty(value = "图像ID")
    private Long imageId;

    @ApiModelProperty(value = "缺陷编号")
    private String defectCode;

    @ApiModelProperty(value = "缺陷类型 hot_spot-热斑 microcrack-隐裂 shadow-遮挡 delamination-脱层 broken-破损 dirt-脏污")
    private String defectType;

    @ApiModelProperty(value = "缺陷等级 1-轻微 2-一般 3-严重 4-紧急")
    private Integer defectLevel;

    @ApiModelProperty(value = "置信度 0-1")
    private BigDecimal confidence;

    @ApiModelProperty(value = "边界框左上角X坐标")
    private Integer bboxX1;

    @ApiModelProperty(value = "边界框左上角Y坐标")
    private Integer bboxY1;

    @ApiModelProperty(value = "边界框右下角X坐标")
    private Integer bboxX2;

    @ApiModelProperty(value = "边界框右下角Y坐标")
    private Integer bboxY2;

    @ApiModelProperty(value = "边界框左上角X坐标(兼容)")
    private Integer xMin;

    @ApiModelProperty(value = "边界框左上角Y坐标(兼容)")
    private Integer yMin;

    @ApiModelProperty(value = "边界框右下角X坐标(兼容)")
    private Integer xMax;

    @ApiModelProperty(value = "边界框右下角Y坐标(兼容)")
    private Integer yMax;

    @ApiModelProperty(value = "中心点X坐标")
    private Integer centerX;

    @ApiModelProperty(value = "中心点Y坐标")
    private Integer centerY;

    @ApiModelProperty(value = "边界框宽度")
    private Integer bboxWidth;

    @ApiModelProperty(value = "边界框高度")
    private Integer bboxHeight;

    @ApiModelProperty(value = "缺陷占比(%)")
    private BigDecimal areaRatio;

    @ApiModelProperty(value = "温度(℃) 红外图像专用")
    private BigDecimal temperature;

    @ApiModelProperty(value = "最高温度(℃)")
    private BigDecimal maxTemperature;

    @ApiModelProperty(value = "最低温度(℃)")
    private BigDecimal minTemperature;

    @ApiModelProperty(value = "温度差(℃)")
    private BigDecimal deltaTemperature;

    @ApiModelProperty(value = "组件行号")
    private Integer componentRow;

    @ApiModelProperty(value = "组件列号")
    private Integer componentCol;

    @ApiModelProperty(value = "组件编号")
    private String componentCode;

    @ApiModelProperty(value = "缺陷GPS经度")
    private BigDecimal gpsLongitude;

    @ApiModelProperty(value = "缺陷GPS纬度")
    private BigDecimal gpsLatitude;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "检测时间")
    private LocalDateTime detectTime;

    @ApiModelProperty(value = "是否确认 0-否 1-是")
    private Integer confirmed;

    @ApiModelProperty(value = "确认人")
    private Long confirmBy;

    @ApiModelProperty(value = "确认时间")
    private LocalDateTime confirmTime;

    @ApiModelProperty(value = "确认备注")
    private String confirmRemark;

    @ApiModelProperty(value = "关联工单ID")
    private Long workOrderId;

    @ApiModelProperty(value = "关联工单编号")
    private String workOrderNo;

    @ApiModelProperty(value = "工单状态")
    private Integer workOrderStatus;

    @ApiModelProperty(value = "关联工单ID(兼容)")
    private Long workorderId;

    @ApiModelProperty(value = "状态 0-待处理 1-处理中 2-已修复 3-已忽略")
    private Integer status;

    @ApiModelProperty(value = "缺陷描述")
    private String description;

    @ApiModelProperty(value = "处理建议")
    private String suggestion;

    @ApiModelProperty(value = "是否人工确认 0-否 1-是(兼容)")
    private Integer verified;

    @ApiModelProperty(value = "确认人(兼容)")
    private Long verifiedBy;

    @ApiModelProperty(value = "确认时间(兼容)")
    private LocalDateTime verifiedTime;
}
