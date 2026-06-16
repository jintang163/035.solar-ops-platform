package com.solar.ops.device.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 根因分析VO
 */
@Data
@ApiModel(value = "根因分析")
public class RootCauseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "异常指标（power/voltage/temperature/current）")
    private String metric;

    @ApiModelProperty(value = "异常类型")
    private String abnormalType;

    @ApiModelProperty(value = "异常描述")
    private String description;

    @ApiModelProperty(value = "处理建议")
    private String suggestion;

    @ApiModelProperty(value = "置信度（0-1）")
    private Double confidence;
}
