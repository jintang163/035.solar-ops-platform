package com.solar.ops.prediction.dto;

import com.solar.ops.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "寿命预警查询DTO")
public class LifetimeAlertQueryDTO extends PageQuery {

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "预警级别")
    private Integer alertLevel;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "预警类型")
    private Integer alertType;
}
