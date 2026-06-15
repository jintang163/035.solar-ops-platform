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
@TableName("revenue_statistics")
@ApiModel(value = "电费收益统计实体")
public class RevenueStatistics extends BaseEntity {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电价方案ID")
    private Long priceSchemeId;

    @ApiModelProperty(value = "统计日期")
    private LocalDate statisticsDate;

    @ApiModelProperty(value = "统计类型：1日 2周 3月 4年")
    private Integer statisticsType;

    @ApiModelProperty(value = "上网电量（kWh）")
    private BigDecimal gridEnergy;

    @ApiModelProperty(value = "上网电费（元）")
    private BigDecimal gridRevenue;

    @ApiModelProperty(value = "国家补贴金额（元）")
    private BigDecimal nationalSubsidyRevenue;

    @ApiModelProperty(value = "省级补贴金额（元）")
    private BigDecimal provincialSubsidyRevenue;

    @ApiModelProperty(value = "市级补贴金额（元）")
    private BigDecimal municipalSubsidyRevenue;

    @ApiModelProperty(value = "补贴总金额（元）")
    private BigDecimal totalSubsidyRevenue;

    @ApiModelProperty(value = "总收益（元）= 上网电费 + 补贴总金额")
    private BigDecimal totalRevenue;

    @ApiModelProperty(value = "度电成本（元/kWh）")
    private BigDecimal unitEnergyCost;

    @ApiModelProperty(value = "运维成本（元）")
    private BigDecimal operationCost;

    @ApiModelProperty(value = "实际上网电价（元/kWh）")
    private BigDecimal effectiveGridPrice;

    @ApiModelProperty(value = "结算状态：0未结算 1已结算")
    private Integer settlementStatus;
}
