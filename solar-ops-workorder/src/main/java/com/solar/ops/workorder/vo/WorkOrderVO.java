package com.solar.ops.workorder.vo;

import com.solar.ops.workorder.entity.WorkOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "工单VO")
public class WorkOrderVO extends WorkOrder {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "故障级别描述")
    private String faultLevelDesc;

    @ApiModelProperty(value = "工单操作日志")
    private List<WorkOrderLogVO> logs;

    @ApiModelProperty(value = "是否超时")
    private Boolean overtime;

    @ApiModelProperty(value = "剩余时间(分钟)")
    private Long remainingMinutes;
}
