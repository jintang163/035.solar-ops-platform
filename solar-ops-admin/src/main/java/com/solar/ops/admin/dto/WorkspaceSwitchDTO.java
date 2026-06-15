package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "工作空间切换DTO")
public class WorkspaceSwitchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID，null表示切换到全部")
    private Long stationId;
}
