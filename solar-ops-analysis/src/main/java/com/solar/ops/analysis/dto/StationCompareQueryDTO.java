package com.solar.ops.analysis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

@Data
@ApiModel(value = "电站对比查询DTO", description = "电站对比分析查询参数")
public class StationCompareQueryDTO {

    @NotEmpty(message = "电站ID列表不能为空")
    @ApiModelProperty(value = "电站ID列表", required = true)
    private List<Long> stationIds;

    @ApiModelProperty(value = "开始日期")
    private LocalDate startTime;

    @ApiModelProperty(value = "结束日期")
    private LocalDate endTime;

    @ApiModelProperty(value = "统计类型：1日 2周 3月 4年，默认3月")
    private Integer statisticsType = 3;

    @ApiModelProperty(value = "对比时期列表（同电站不同时期对比时使用）")
    private List<PeriodCompareDTO> periods;
}
