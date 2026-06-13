package com.solar.ops.workorder.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(value = "工单创建DTO")
public class WorkOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID", required = true)
    @NotNull(message = "电站ID不能为空")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "故障码", required = true)
    @NotBlank(message = "故障码不能为空")
    private String faultCode;

    @ApiModelProperty(value = "故障描述")
    private String description;

    @ApiModelProperty(value = "故障级别 1-低级 2-中级 3-高级 4-紧急")
    private Integer faultLevel;

    @ApiModelProperty(value = "预计完成时间(小时)")
    private Integer expectHours;
}
