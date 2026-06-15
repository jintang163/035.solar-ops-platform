package com.solar.ops.analysis.vo;

import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "电价方案VO")
public class ElectricityPriceSchemeVO extends ElectricityPriceScheme {

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "是否默认描述")
    private String isDefaultDesc;

    @ApiModelProperty(value = "是否平价上网描述")
    private String isParityDesc;

    @ApiModelProperty(value = "有效总电价（元/kWh）")
    private BigDecimal effectiveTotalPrice;
}
