package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "发电量趋势VO")
public class GenerationTrendVO {

    @ApiModelProperty(value = "日期")
    private String date;

    @ApiModelProperty(value = "发电量(kWh)")
    private BigDecimal generation;
}
