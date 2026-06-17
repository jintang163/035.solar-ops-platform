package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "功率趋势VO")
public class PowerTrendVO {

    @ApiModelProperty(value = "时间点")
    private String time;

    @ApiModelProperty(value = "功率(kW)")
    private BigDecimal power;
}
