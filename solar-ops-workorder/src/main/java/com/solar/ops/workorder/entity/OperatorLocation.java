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
@TableName("operator_location")
@ApiModel(value = "运维人员位置", description = "运维人员实时位置")
public class OperatorLocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "运维人员ID")
    private Long userId;

    @ApiModelProperty(value = "运维人员姓名")
    private String userName;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "定位精度(米)")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "速度(km/h)")
    private BigDecimal speed;

    @ApiModelProperty(value = "方向角度(0-360)")
    private BigDecimal heading;

    @ApiModelProperty(value = "定位方式 GPS/WIFI/BASE")
    private String locationType;

    @ApiModelProperty(value = "最后上报时间戳")
    private Long reportTime;
}
