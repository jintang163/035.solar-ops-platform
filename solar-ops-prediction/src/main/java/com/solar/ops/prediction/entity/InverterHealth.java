package com.solar.ops.prediction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inverter_health")
@ApiModel(value = "InverterHealth对象", description = "逆变器健康度记录")
public class InverterHealth extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "记录日期")
    private LocalDate recordDate;

    @ApiModelProperty(value = "平均温度(℃)")
    private BigDecimal avgTemperature;

    @ApiModelProperty(value = "最高温度(℃)")
    private BigDecimal maxTemperature;

    @ApiModelProperty(value = "工作时长(小时)")
    private BigDecimal operatingHours;

    @ApiModelProperty(value = "故障次数")
    private Integer faultCount;

    @ApiModelProperty(value = "故障严重程度(1-5)")
    private Integer faultSeverity;

    @ApiModelProperty(value = "输出功率比(0-1)")
    private BigDecimal outputPowerRatio;

    @ApiModelProperty(value = "效率下降率(%)")
    private BigDecimal efficiencyDrop;

    @ApiModelProperty(value = "健康度评分(0-1)")
    private BigDecimal healthScore;

    @ApiModelProperty(value = "评估时间")
    private LocalDateTime assessmentTime;
}
