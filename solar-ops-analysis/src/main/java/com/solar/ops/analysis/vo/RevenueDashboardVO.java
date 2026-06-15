package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "收益仪表盘VO")
public class RevenueDashboardVO {

    @ApiModelProperty(value = "今日收益（元）")
    private BigDecimal todayRevenue;

    @ApiModelProperty(value = "本月收益（元）")
    private BigDecimal monthRevenue;

    @ApiModelProperty(value = "本年收益（元）")
    private BigDecimal yearRevenue;

    @ApiModelProperty(value = "累计总收益（元）")
    private BigDecimal totalRevenue;

    @ApiModelProperty(value = "今日上网电量（kWh）")
    private BigDecimal todayEnergy;

    @ApiModelProperty(value = "本月上网电量（kWh）")
    private BigDecimal monthEnergy;

    @ApiModelProperty(value = "平均度电成本（元/kWh）")
    private BigDecimal avgUnitCost;

    @ApiModelProperty(value = "平均上网电价（元/kWh）")
    private BigDecimal avgGridPrice;

    @ApiModelProperty(value = "投资回报率（%）")
    private BigDecimal roi;

    @ApiModelProperty(value = "投资回收年限（年）")
    private BigDecimal paybackPeriod;

    @ApiModelProperty(value = "收益趋势（近30天/近12月）")
    private List<RevenueTrendVO> revenueTrend;

    @ApiModelProperty(value = "度电成本趋势")
    private List<RevenueTrendVO> costTrend;

    @ApiModelProperty(value = "各电站收益排名")
    private List<StationRevenueRankVO> stationRank;
}
