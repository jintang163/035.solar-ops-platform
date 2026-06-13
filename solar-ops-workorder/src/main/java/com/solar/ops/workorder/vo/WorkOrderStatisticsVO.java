package com.solar.ops.workorder.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@ApiModel(value = "工单统计VO")
public class WorkOrderStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工单总数")
    private Long totalCount;

    @ApiModelProperty(value = "待接单数量")
    private Long pendingCount;

    @ApiModelProperty(value = "处理中数量")
    private Long processingCount;

    @ApiModelProperty(value = "已完成数量")
    private Long completedCount;

    @ApiModelProperty(value = "已关闭数量")
    private Long closedCount;

    @ApiModelProperty(value = "超时工单数量")
    private Long overtimeCount;

    @ApiModelProperty(value = "按故障级别统计")
    private Map<String, Long> levelStatistics;

    @ApiModelProperty(value = "按状态统计")
    private Map<String, Long> statusStatistics;
}
