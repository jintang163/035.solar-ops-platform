package com.solar.ops.analysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("efficiency_statistics")
@ApiModel(value = "效率统计实体")
public class EfficiencyStatistics extends BaseEntity {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "统计日期")
    private LocalDate statisticsDate;

    @ApiModelProperty(value = "统计类型：1日 2周 3月 4年")
    private Integer statisticsType;

    @ApiModelProperty(value = "PR值（性能比）")
    private BigDecimal prValue;

    @ApiModelProperty(value = "系统效率")
    private BigDecimal systemEfficiency;

    @ApiModelProperty(value = "等效利用小时数")
    private BigDecimal equivalentHours;

    @ApiModelProperty(value = "总发电量（kWh）")
    private BigDecimal totalEnergy;
}
