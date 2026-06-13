package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inverter")
@ApiModel(value = "Inverter对象", description = "逆变器设备")
public class Inverter extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "设备序列号")
    private String deviceSn;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "设备型号")
    private String deviceModel;

    @ApiModelProperty(value = "额定功率(kW)")
    private BigDecimal ratedPower;

    @ApiModelProperty(value = "安装位置")
    private String installLocation;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "状态 0停用 1启用")
    private Integer status;

    @ApiModelProperty(value = "在线状态 0离线 1在线")
    private Integer onlineStatus;

    @ApiModelProperty(value = "最后在线时间")
    private LocalDateTime lastOnlineTime;
}
