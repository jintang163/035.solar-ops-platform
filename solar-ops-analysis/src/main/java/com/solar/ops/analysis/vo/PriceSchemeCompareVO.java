package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "电价方案对比VO")
public class PriceSchemeCompareVO {

    @ApiModelProperty(value = "方案ID")
    private Long schemeId;

    @ApiModelProperty(value = "方案名称")
    private String schemeName;

    @ApiModelProperty(value = "上网电价（元/kWh）")
    private BigDecimal gridPrice;

    @ApiModelProperty(value = "国家补贴（元/kWh）")
    private BigDecimal nationalSubsidy;

    @ApiModelProperty(value = "省级补贴（元/kWh）")
    private BigDecimal provincialSubsidy;

    @ApiModelProperty(value = "市级补贴（元/kWh）")
    private BigDecimal municipalSubsidy;

    @ApiModelProperty(value = "总电价（元/kWh）")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "预估年收益（元）")
    private BigDecimal estimatedYearRevenue;

    @ApiModelProperty(value = "预估年发电量（kWh）")
    private BigDecimal estimatedYearEnergy;

    @ApiModelProperty(value = "投资回报率（%）")
    private BigDecimal roi;

    @ApiModelProperty(value = "投资回收年限（年）")
    private BigDecimal paybackPeriod;
}
