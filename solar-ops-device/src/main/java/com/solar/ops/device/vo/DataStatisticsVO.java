package com.solar.ops.device.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据统计信息VO
 */
@Data
@ApiModel(value = "数据统计信息")
public class DataStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "平均功率（W）")
    private Double avgPower;

    @ApiModelProperty(value = "最大功率（W）")
    private Double maxPower;

    @ApiModelProperty(value = "平均温度（℃）")
    private Double avgTemp;

    @ApiModelProperty(value = "最高温度（℃）")
    private Double maxTemp;

    @ApiModelProperty(value = "故障次数")
    private Integer faultCount;

    @ApiModelProperty(value = "数据点总数")
    private Integer dataPointCount;
}
