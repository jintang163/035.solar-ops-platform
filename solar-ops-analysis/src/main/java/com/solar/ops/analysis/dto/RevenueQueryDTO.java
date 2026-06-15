package com.solar.ops.analysis.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "收益统计查询DTO")
public class RevenueQueryDTO extends PageQuery {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "开始日期")
    private LocalDate startDate;

    @ApiModelProperty(value = "结束日期")
    private LocalDate endDate;

    @ApiModelProperty(value = "统计类型：1日 2周 3月 4年")
    private Integer statisticsType;
}
