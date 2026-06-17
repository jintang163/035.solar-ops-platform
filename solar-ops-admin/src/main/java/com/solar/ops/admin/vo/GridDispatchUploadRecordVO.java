package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "调度上传记录VO")
public class GridDispatchUploadRecordVO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("协议类型：1-IEC104 2-Modbus TCP")
    private Integer protocolType;

    @ApiModelProperty("协议类型描述")
    private String protocolTypeDesc;

    @ApiModelProperty("数据类型：1-实时功率 2-电压 3-频率 4-发电量 5-设备状态")
    private Integer dataType;

    @ApiModelProperty("数据类型描述")
    private String dataTypeDesc;

    @ApiModelProperty("电站ID")
    private Long stationId;

    @ApiModelProperty("电站名称")
    private String stationName;

    @ApiModelProperty("逆变器ID")
    private Long inverterId;

    @ApiModelProperty("逆变器名称")
    private String inverterName;

    @ApiModelProperty("总有功功率(kW)")
    private BigDecimal totalActivePower;

    @ApiModelProperty("总无功功率(kVar)")
    private BigDecimal totalReactivePower;

    @ApiModelProperty("A相电压(V)")
    private BigDecimal voltageA;

    @ApiModelProperty("B相电压(V)")
    private BigDecimal voltageB;

    @ApiModelProperty("C相电压(V)")
    private BigDecimal voltageC;

    @ApiModelProperty("频率(Hz)")
    private BigDecimal frequency;

    @ApiModelProperty("功率因数")
    private BigDecimal powerFactor;

    @ApiModelProperty("日发电量(kWh)")
    private BigDecimal dailyGeneration;

    @ApiModelProperty("总发电量(kWh)")
    private BigDecimal totalGeneration;

    @ApiModelProperty("设备状态：1-运行 2-停机 3-故障 4-离线")
    private Integer deviceStatus;

    @ApiModelProperty("设备状态描述")
    private String deviceStatusDesc;

    @ApiModelProperty("上传时间")
    private LocalDateTime uploadTime;

    @ApiModelProperty("上传状态：0-待上传 1-上传成功 2-上传失败")
    private Integer uploadStatus;

    @ApiModelProperty("上传状态描述")
    private String uploadStatusDesc;

    @ApiModelProperty("调度主站响应码")
    private String responseCode;

    @ApiModelProperty("失败原因")
    private String failReason;

    @ApiModelProperty("重试次数")
    private Integer retryCount;

    @ApiModelProperty("是否已缓存到Redis")
    private Integer cached;

    @ApiModelProperty("缓存描述")
    private String cachedDesc;
}
