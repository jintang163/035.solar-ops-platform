package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "移动端驾驶舱VO")
public class MobileDashboardVO {

    @ApiModelProperty(value = "实时总功率(kW)")
    private BigDecimal totalPower;

    @ApiModelProperty(value = "今日发电量(kWh)")
    private BigDecimal todayGeneration;

    @ApiModelProperty(value = "累计减排(tCO2)")
    private BigDecimal totalEmissionReduction;

    @ApiModelProperty(value = "设备在线率(%)")
    private BigDecimal onlineRate;

    @ApiModelProperty(value = "告警数量")
    private Integer alarmCount;

    @ApiModelProperty(value = "未处理工单数量")
    private Integer unhandledWorkOrderCount;

    @ApiModelProperty(value = "功率变化趋势（up/down/neutral）")
    private String powerTrend;

    @ApiModelProperty(value = "功率变化百分比")
    private BigDecimal powerChangePercent;

    @ApiModelProperty(value = "发电量变化趋势")
    private String generationTrend;

    @ApiModelProperty(value = "发电量变化百分比")
    private BigDecimal generationChangePercent;

    @ApiModelProperty(value = "电站健康统计")
    private List<StationHealthStatVO> stationHealthStats;

    @ApiModelProperty(value = "告警电站列表（TOP 5）")
    private List<StationAlarmVO> alarmStations;

    @ApiModelProperty(value = "数据更新时间")
    private LocalDateTime updateTime;
}
