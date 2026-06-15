package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "积灰等级分布统计VO")
public class DustLevelStatVO {

    @ApiModelProperty(value = "积灰等级")
    private Integer dustLevel;

    @ApiModelProperty(value = "积灰等级描述")
    private String dustLevelDesc;

    @ApiModelProperty(value = "数量")
    private Integer count;

    @ApiModelProperty(value = "占比（0-1）")
    private Double ratio;

    @ApiModelProperty(value = "颜色")
    private String color;
}
