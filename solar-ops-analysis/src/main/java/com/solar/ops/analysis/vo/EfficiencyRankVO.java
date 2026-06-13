package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "效率排名VO")
public class EfficiencyRankVO {

    @ApiModelProperty(value = "排名")
    private Integer rank;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "PR值")
    private BigDecimal prValue;

    @ApiModelProperty(value = "系统效率")
    private BigDecimal systemEfficiency;

    @ApiModelProperty(value = "等效利用小时数")
    private BigDecimal equivalentHours;

    @ApiModelProperty(value = "总发电量")
    private BigDecimal totalEnergy;
}
