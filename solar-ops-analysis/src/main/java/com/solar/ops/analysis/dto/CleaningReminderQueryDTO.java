package com.solar.ops.analysis.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "清洗提醒查询DTO")
public class CleaningReminderQueryDTO extends PageQuery {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "方阵编号")
    private String arrayNumber;

    @ApiModelProperty(value = "积灰等级 0-无 1-轻度 2-中度 3-重度")
    private Integer dustLevel;

    @ApiModelProperty(value = "状态 0-未处理 1-已创建计划 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "建议开始日期")
    private LocalDate startDate;

    @ApiModelProperty(value = "建议结束日期")
    private LocalDate endDate;
}
