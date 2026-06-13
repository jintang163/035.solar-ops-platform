package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_log")
@ApiModel(value = "工单日志实体")
public class WorkOrderLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工单ID")
    private Long orderId;

    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "操作动作")
    private String action;

    @ApiModelProperty(value = "备注")
    private String remark;
}
