package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "健康度评估VO")
public class HealthAssessmentVO {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "健康等级：1优秀(绿) 2良好(黄) 3差(红)")
    private Integer healthLevel;

    @ApiModelProperty(value = "健康等级描述")
    private String healthLevelDesc;

    @ApiModelProperty(value = "健康颜色：green/yellow/red")
    private String healthColor;

    @ApiModelProperty(value = "PR值")
    private BigDecimal prValue;

    @ApiModelProperty(value = "故障数量")
    private Integer faultCount;

    @ApiModelProperty(value = "效率评分（0-100）")
    private BigDecimal efficiencyScore;

    @ApiModelProperty(value = "评估时间")
    private LocalDateTime assessmentTime;
}
