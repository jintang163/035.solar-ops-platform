package com.solar.ops.analysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("station_health")
@ApiModel(value = "电站健康度实体")
public class StationHealth extends BaseEntity {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "健康等级：1优秀(绿) 2良好(黄) 3差(红)")
    private Integer healthLevel;

    @ApiModelProperty(value = "PR值")
    private BigDecimal prValue;

    @ApiModelProperty(value = "故障数量")
    private Integer faultCount;

    @ApiModelProperty(value = "效率评分（0-100）")
    private BigDecimal efficiencyScore;

    @ApiModelProperty(value = "评估时间")
    private LocalDateTime assessmentTime;
}
