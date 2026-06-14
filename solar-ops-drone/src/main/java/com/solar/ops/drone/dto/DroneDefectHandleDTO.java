package com.solar.ops.drone.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "缺陷处理DTO")
public class DroneDefectHandleDTO {

    @NotNull(message = "缺陷ID不能为空")
    @ApiModelProperty(value = "缺陷ID", required = true)
    private Long id;

    @ApiModelProperty(value = "状态 0-待处理 1-处理中 2-已修复 3-已忽略")
    private Integer status;

    @ApiModelProperty(value = "处理备注")
    private String handleRemark;

    @ApiModelProperty(value = "是否生成工单")
    private Boolean createWorkorder;

    @ApiModelProperty(value = "处理人ID")
    private Long handlerId;

    @ApiModelProperty(value = "处理人姓名")
    private String handlerName;
}
