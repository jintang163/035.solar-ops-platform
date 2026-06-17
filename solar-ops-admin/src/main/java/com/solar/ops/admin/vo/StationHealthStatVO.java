package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "电站健康统计VO")
public class StationHealthStatVO {

    @ApiModelProperty(value = "健康等级：1优秀(绿) 2良好(黄) 3差(红)")
    private Integer healthLevel;

    @ApiModelProperty(value = "健康颜色：green/yellow/red")
    private String healthColor;

    @ApiModelProperty(value = "健康等级描述")
    private String healthLevelDesc;

    @ApiModelProperty(value = "电站数量")
    private Integer count;

    @ApiModelProperty(value = "占比(%)")
    private java.math.BigDecimal percentage;
}
