package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "电站地图VO")
public class StationMapVO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "电站编号")
    private String stationCode;

    @ApiModelProperty(value = "装机容量(kW)")
    private BigDecimal capacity;

    @ApiModelProperty(value = "当前功率(kW)")
    private BigDecimal currentPower;

    @ApiModelProperty(value = "今日发电量(kWh)")
    private BigDecimal todayGeneration;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "地址")
    private String address;

    @ApiModelProperty(value = "健康等级：1优秀(绿) 2良好(黄) 3差(红)")
    private Integer healthLevel;

    @ApiModelProperty(value = "健康颜色：green/yellow/red")
    private String healthColor;

    @ApiModelProperty(value = "健康度评分(0-100)")
    private BigDecimal healthScore;

    @ApiModelProperty(value = "在线状态 0离线 1在线")
    private Integer onlineStatus;

    @ApiModelProperty(value = "逆变器数量")
    private Integer inverterCount;

    @ApiModelProperty(value = "在线逆变器数量")
    private Integer onlineInverterCount;

    @ApiModelProperty(value = "告警数量")
    private Integer alarmCount;

    @ApiModelProperty(value = "未处理工单数量")
    private Integer unhandledOrderCount;
}
