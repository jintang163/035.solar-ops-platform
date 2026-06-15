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
@TableName("cleaning_plan")
@ApiModel(value = "清洁计划实体")
public class CleaningPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "计划编号")
    private String planNo;

    @ApiModelProperty(value = "关联清洗提醒ID")
    private Long reminderId;

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

    @ApiModelProperty(value = "计划标题")
    private String title;

    @ApiModelProperty(value = "计划描述")
    private String description;

    @ApiModelProperty(value = "计划清洗日期")
    private LocalDate planDate;

    @ApiModelProperty(value = "实际开始时间")
    private LocalDateTime actualStartTime;

    @ApiModelProperty(value = "实际完成时间")
    private LocalDateTime actualEndTime;

    @ApiModelProperty(value = "状态 0-待执行 1-执行中 2-已完成 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "负责人ID")
    private Long ownerId;

    @ApiModelProperty(value = "负责人姓名")
    private String ownerName;

    @ApiModelProperty(value = "参与团队/人员")
    private String teamMembers;

    @ApiModelProperty(value = "清洗前照片URL（多个逗号分隔）")
    private String beforeCleanPhotos;

    @ApiModelProperty(value = "清洗后照片URL（多个逗号分隔）")
    private String afterCleanPhotos;

    @ApiModelProperty(value = "清洗前发电量（kWh，取清洗前7日平均）")
    private BigDecimal beforeCleanEnergy;

    @ApiModelProperty(value = "清洗后发电量（kWh，取清洗后7日平均）")
    private BigDecimal afterCleanEnergy;

    @ApiModelProperty(value = "发电量提升值（kWh）")
    private BigDecimal improvedEnergy;

    @ApiModelProperty(value = "发电量提升率（0-1）")
    private BigDecimal improvementRate;

    @ApiModelProperty(value = "清洗方式（人工/机械/机器人）")
    private String cleaningMethod;

    @ApiModelProperty(value = "用水量（升）")
    private BigDecimal waterUsage;

    @ApiModelProperty(value = "清洗费用（元）")
    private BigDecimal cleaningCost;

    @ApiModelProperty(value = "验收人ID")
    private Long inspectorId;

    @ApiModelProperty(value = "验收人姓名")
    private String inspectorName;

    @ApiModelProperty(value = "验收时间")
    private LocalDateTime inspectionTime;

    @ApiModelProperty(value = "验收备注")
    private String inspectionRemark;

    @ApiModelProperty(value = "清洗工作备注")
    private String workRemark;
}
