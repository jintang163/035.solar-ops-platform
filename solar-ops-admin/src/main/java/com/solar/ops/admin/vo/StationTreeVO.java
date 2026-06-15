package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "电站树结构VO")
public class StationTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long id;

    @ApiModelProperty(value = "电站编码")
    private String stationCode;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "组织ID")
    private Long orgId;

    @ApiModelProperty(value = "组织名称")
    private String orgName;

    @ApiModelProperty(value = "容量(kW)")
    private BigDecimal capacity;

    @ApiModelProperty(value = "状态")
    private Integer status;
}
