package com.solar.ops.analysis.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "清洗计划查询DTO")
public class CleaningPlanQueryDTO extends PageQuery {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "方阵编号")
    private String arrayNumber;

    @ApiModelProperty(value = "状态 0-待执行 1-执行中 2-已完成 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "负责人ID")
    private Long ownerId;

    @ApiModelProperty(value = "计划开始日期")
    private LocalDate startDate;

    @ApiModelProperty(value = "计划结束日期")
    private LocalDate endDate;

    @ApiModelProperty(value = "计划编号/标题关键字")
    private String keyword;
}
