package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "投资信息DTO")
public class InvestmentInfoDTO {

    @ApiModelProperty(value = "总投资（元）")
    private BigDecimal totalInvestment;

    @ApiModelProperty(value = "年运维成本（元）")
    private BigDecimal annualOperationCost;

    @ApiModelProperty(value = "设计寿命（年）")
    private Integer designLife;
}
