package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ApiModel(value = "清洗计划执行DTO（开始/完成/上传照片等）")
public class CleaningPlanExecuteDTO {

    @ApiModelProperty(value = "计划ID", required = true)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "清洗前照片URL（多个逗号分隔）")
    private String beforeCleanPhotos;

    @ApiModelProperty(value = "清洗后照片URL（多个逗号分隔）")
    private String afterCleanPhotos;

    @ApiModelProperty(value = "清洗方式（人工/机械/机器人）")
    private String cleaningMethod;

    @ApiModelProperty(value = "用水量（升）")
    private BigDecimal waterUsage;

    @ApiModelProperty(value = "清洗费用（元）")
    private BigDecimal cleaningCost;

    @ApiModelProperty(value = "清洗工作备注")
    private String workRemark;

    @ApiModelProperty(value = "验收备注")
    private String inspectionRemark;
}
