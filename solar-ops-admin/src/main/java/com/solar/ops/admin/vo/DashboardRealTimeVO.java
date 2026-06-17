package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "大屏实时数据VO")
public class DashboardRealTimeVO {

    @ApiModelProperty(value = "实时总功率(kW)")
    private BigDecimal totalPower;

    @ApiModelProperty(value = "今日发电量(kWh)")
    private BigDecimal todayGeneration;

    @ApiModelProperty(value = "累计发电量(kWh)")
    private BigDecimal totalGeneration;

    @ApiModelProperty(value = "累计减排(tCO2)")
    private BigDecimal totalEmissionReduction;

    @ApiModelProperty(value = "设备在线率(%)")
    private BigDecimal onlineRate;

    @ApiModelProperty(value = "在线设备数")
    private Integer onlineCount;

    @ApiModelProperty(value = "离线设备数")
    private Integer offlineCount;

    @ApiModelProperty(value = "告警数量")
    private Integer alarmCount;

    @ApiModelProperty(value = "未处理工单数量")
    private Integer unhandledWorkOrderCount;

    @ApiModelProperty(value = "电站总数")
    private Integer stationCount;

    @ApiModelProperty(value = "逆变器总数")
    private Integer inverterCount;

    @ApiModelProperty(value = "数据更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "电站地图数据")
    private List<StationMapVO> stationMapList;

    @ApiModelProperty(value = "功率趋势数据（近24小时）")
    private List<PowerTrendVO> powerTrend;

    @ApiModelProperty(value = "发电量趋势数据（近7天）")
    private List<GenerationTrendVO> generationTrend;
}
