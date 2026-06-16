package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("station")
@ApiModel(value = "电站（调度用）")
public class DispatchStation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站名称")
    private String stationName;

    @ApiModelProperty(value = "电站编号")
    private String stationCode;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "状态 0停用 1启用")
    private Integer status;
}
