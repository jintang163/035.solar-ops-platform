package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@ApiModel(value = "时期对比DTO", description = "同电站不同时期对比参数")
public class PeriodCompareDTO {

    @ApiModelProperty(value = "时期标签，如'本期'、'同期'、'上月'")
    private String label;

    @ApiModelProperty(value = "开始日期")
    private LocalDate startTime;

    @ApiModelProperty(value = "结束日期")
    private LocalDate endTime;
}
