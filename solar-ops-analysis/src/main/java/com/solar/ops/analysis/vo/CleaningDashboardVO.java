package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "清洗统计仪表盘VO")
public class CleaningDashboardVO {

    @ApiModelProperty(value = "总清洗次数")
    private Integer totalCleaningCount;

    @ApiModelProperty(value = "本月清洗次数")
    private Integer monthlyCleaningCount;

    @ApiModelProperty(value = "待执行清洗计划数")
    private Integer pendingPlanCount;

    @ApiModelProperty(value = "执行中清洗计划数")
    private Integer inProgressPlanCount;

    @ApiModelProperty(value = "未处理清洗提醒数")
    private Integer unhandledReminderCount;

    @ApiModelProperty(value = "累计提升发电量（kWh）")
    private BigDecimal totalImprovedEnergy;

    @ApiModelProperty(value = "本月提升发电量（kWh）")
    private BigDecimal monthlyImprovedEnergy;

    @ApiModelProperty(value = "累计节省费用估算（元）")
    private BigDecimal totalSavedCost;

    @ApiModelProperty(value = "本月清洗费用（元）")
    private BigDecimal monthlyCleaningCost;

    @ApiModelProperty(value = "各积灰等级分布统计")
    private List<DustLevelStatVO> dustLevelStats;

    @ApiModelProperty(value = "近30天清洗提升发电量趋势")
    private List<CleaningTrendVO> improvementTrend;

    @ApiModelProperty(value = "各电站清洗次数排名")
    private List<StationCleaningRankVO> stationRank;
}
