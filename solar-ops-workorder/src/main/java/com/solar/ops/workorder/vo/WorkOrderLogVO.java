package com.solar.ops.workorder.vo;

import com.solar.ops.workorder.entity.WorkOrderLog;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "工单日志VO")
public class WorkOrderLogVO extends WorkOrderLog {

    private static final long serialVersionUID = 1L;
}
