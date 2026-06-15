package com.solar.ops.analysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cleaning_reminder")
@ApiModel(value = "清洗建议/提醒实体")
public class CleaningReminder extends BaseEntity {

    private static final long serialVersionUID = 1L;

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

    @ApiModelProperty(value = "方阵编号（如：1号、2号、3号方阵）")
    private String arrayNumber;

    @ApiModelProperty(value = "积灰等级 0-无 1-轻度 2-中度 3-重度")
    private Integer dustLevel;

    @ApiModelProperty(value = "发电量衰减率（0-1）")
    private BigDecimal attenuationRate;

    @ApiModelProperty(value = "预估日损失电量（kWh）")
    private BigDecimal estimatedDailyLoss;

    @ApiModelProperty(value = "已关联的积灰记录ID")
    private Long dustRecordId;

    @ApiModelProperty(value = "建议清洗日期")
    private LocalDate suggestCleanDate;

    @ApiModelProperty(value = "建议截止日期")
    private LocalDate deadlineDate;

    @ApiModelProperty(value = "提醒标题（如：3号方阵清洗推荐）")
    private String title;

    @ApiModelProperty(value = "提醒详情描述")
    private String description;

    @ApiModelProperty(value = "状态 0-未处理 1-已创建计划 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "关联的清洗计划ID")
    private Long cleaningPlanId;

    @ApiModelProperty(value = "处理人ID")
    private Long handlerId;

    @ApiModelProperty(value = "处理人姓名")
    private String handlerName;

    @ApiModelProperty(value = "处理时间")
    private LocalDateTime handleTime;
}
