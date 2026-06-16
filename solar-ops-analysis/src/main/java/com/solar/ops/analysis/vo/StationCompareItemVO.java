package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "电站对比指标项VO", description = "单个电站或单个时期的对比指标")
public class StationCompareItemVO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "时期标签（同电站不同时期对比时使用）")
    private String periodLabel;

    @ApiModelProperty(value = "装机容量(kW)")
    private BigDecimal capacity;

    @ApiModelProperty(value = "PR值（性能比）")
    private BigDecimal prValue;

    @ApiModelProperty(value = "系统效率")
    private BigDecimal systemEfficiency;

    @ApiModelProperty(value = "等效利用小时数")
    private BigDecimal equivalentHours;

    @ApiModelProperty(value = "总发电量（kWh）")
    private BigDecimal totalEnergy;

    @ApiModelProperty(value = "故障率")
    private BigDecimal faultRate;

    @ApiModelProperty(value = "健康度评分（0-100）")
    private BigDecimal healthScore;

    @ApiModelProperty(value = "逆变器数量")
    private Integer inverterCount;

    @ApiModelProperty(value = "在线率")
    private BigDecimal onlineRate;

    @ApiModelProperty(value = "收益（元）")
    private BigDecimal revenue;
}
