package com.solar.ops.analysis.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "清洗提醒VO")
public class CleaningReminderVO {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "提醒编号")
    private String reminderNo;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称/方阵编号")
    private String inverterName;

    @ApiModelProperty(value = "方阵编号")
    private String arrayNumber;

    @ApiModelProperty(value = "积灰等级 0-无 1-轻度 2-中度 3-重度")
    private Integer dustLevel;

    @ApiModelProperty(value = "积灰等级描述")
    private String dustLevelDesc;

    @ApiModelProperty(value = "积灰等级颜色")
    private String dustLevelColor;

    @ApiModelProperty(value = "发电量衰减率（0-1）")
    private BigDecimal attenuationRate;

    @ApiModelProperty(value = "衰减率百分比（用于展示）")
    private BigDecimal attenuationRatePercent;

    @ApiModelProperty(value = "预估日损失电量（kWh）")
    private BigDecimal estimatedDailyLoss;

    @ApiModelProperty(value = "建议清洗日期")
    private LocalDate suggestCleanDate;

    @ApiModelProperty(value = "建议截止日期")
    private LocalDate deadlineDate;

    @ApiModelProperty(value = "提醒标题")
    private String title;

    @ApiModelProperty(value = "提醒详情描述")
    private String description;

    @ApiModelProperty(value = "状态 0-未处理 1-已创建计划 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "关联的清洗计划ID")
    private Long cleaningPlanId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
