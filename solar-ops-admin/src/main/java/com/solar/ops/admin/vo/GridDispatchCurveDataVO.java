package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "曲线对比数据VO")
public class GridDispatchCurveDataVO {

    @ApiModelProperty("时间点")
    private String time;

    @ApiModelProperty("目标有功功率(kW)")
    private BigDecimal targetActivePower;

    @ApiModelProperty("实际有功功率(kW)")
    private BigDecimal actualActivePower;

    @ApiModelProperty("目标电压(V)")
    private BigDecimal targetVoltage;

    @ApiModelProperty("实际电压(V)")
    private BigDecimal actualVoltage;

    @ApiModelProperty("目标频率(Hz)")
    private BigDecimal targetFrequency;

    @ApiModelProperty("实际频率(Hz)")
    private BigDecimal actualFrequency;
}
