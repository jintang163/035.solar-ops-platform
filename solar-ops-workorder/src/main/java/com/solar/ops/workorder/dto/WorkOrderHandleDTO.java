package com.solar.ops.workorder.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "工单处理DTO")
public class WorkOrderHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long orderId;

    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "处理人ID(派单时使用)")
    private Long handlerId;

    @ApiModelProperty(value = "处理人姓名(派单时使用)")
    private String handlerName;

    @ApiModelProperty(value = "处理备注")
    private String remark;

    @ApiModelProperty(value = "维修照片URL列表")
    private List<String> photoUrls;

    @ApiModelProperty(value = "解决方案")
    private String solution;
}
