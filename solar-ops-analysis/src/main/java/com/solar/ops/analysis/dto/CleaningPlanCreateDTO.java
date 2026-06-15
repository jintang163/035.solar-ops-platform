package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ApiModel(value = "清洗计划创建/更新DTO")
public class CleaningPlanCreateDTO {

    @ApiModelProperty(value = "计划ID（更新时传）")
    private Long id;

    @ApiModelProperty(value = "关联清洗提醒ID")
    private Long reminderId;

    @ApiModelProperty(value = "电站ID", required = true)
    @NotNull(message = "电站ID不能为空")
    private Long stationId;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "逆变器名称/方阵编号")
    private String inverterName;

    @ApiModelProperty(value = "方阵编号")
    private String arrayNumber;

    @ApiModelProperty(value = "计划标题", required = true)
    @NotBlank(message = "计划标题不能为空")
    private String title;

    @ApiModelProperty(value = "计划描述")
    private String description;

    @ApiModelProperty(value = "计划清洗日期", required = true)
    @NotNull(message = "计划清洗日期不能为空")
    private LocalDate planDate;

    @ApiModelProperty(value = "负责人ID")
    private Long ownerId;

    @ApiModelProperty(value = "负责人姓名")
    private String ownerName;

    @ApiModelProperty(value = "参与团队/人员")
    private String teamMembers;

    @ApiModelProperty(value = "清洗方式（人工/机械/机器人）")
    private String cleaningMethod;

    @ApiModelProperty(value = "清洗费用（元）")
    private BigDecimal cleaningCost;
}
