package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "收益趋势VO")
public class RevenueTrendVO {

    @ApiModelProperty(value = "日期（yyyy-MM-dd 或 yyyy-MM）")
    private String period;

    @ApiModelProperty(value = "上网电量（kWh）")
    private BigDecimal gridEnergy;

    @ApiModelProperty(value = "上网电费（元）")
    private BigDecimal gridRevenue;

    @ApiModelProperty(value = "补贴收益（元）")
    private BigDecimal subsidyRevenue;

    @ApiModelProperty(value = "总收益（元）")
    private BigDecimal totalRevenue;
}
