package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "电站收益排名VO")
public class StationRevenueRankVO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "总收益（元）")
    private BigDecimal totalRevenue;

    @ApiModelProperty(value = "总发电量（kWh）")
    private BigDecimal totalEnergy;

    @ApiModelProperty(value = "平均度电单价（元/kWh）")
    private BigDecimal avgUnitPrice;

    @ApiModelProperty(value = "度电成本（元/kWh）")
    private BigDecimal unitCost;
}
