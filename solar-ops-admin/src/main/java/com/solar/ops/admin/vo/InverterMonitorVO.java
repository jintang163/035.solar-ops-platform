package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "逆变器监控VO")
public class InverterMonitorVO {

    @ApiModelProperty(value = "逆变器ID")
    private Long id;

    @ApiModelProperty(value = "设备序列号")
    private String deviceSn;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "设备型号")
    private String deviceModel;

    @ApiModelProperty(value = "额定功率(kW)")
    private BigDecimal ratedPower;

    @ApiModelProperty(value = "当前功率(kW)")
    private BigDecimal currentPower;

    @ApiModelProperty(value = "当日发电量(kWh)")
    private BigDecimal dayGeneration;

    @ApiModelProperty(value = "电压(V)")
    private BigDecimal voltage;

    @ApiModelProperty(value = "电流(A)")
    private BigDecimal current;

    @ApiModelProperty(value = "温度(℃)")
    private BigDecimal temperature;

    @ApiModelProperty(value = "转换效率(%)")
    private BigDecimal efficiency;

    @ApiModelProperty(value = "运行时长(小时)")
    private BigDecimal runHours;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "安装位置")
    private String installLocation;

    @ApiModelProperty(value = "在线状态 0离线 1在线")
    private Integer onlineStatus;

    @ApiModelProperty(value = "健康等级：1优秀(绿) 2良好(黄) 3差(红)")
    private Integer healthLevel;

    @ApiModelProperty(value = "健康颜色：green/yellow/red")
    private String healthColor;

    @ApiModelProperty(value = "故障码")
    private String faultCode;

    @ApiModelProperty(value = "告警信息")
    private String alarmMessage;

    @ApiModelProperty(value = "最后在线时间")
    private LocalDateTime lastOnlineTime;

    @ApiModelProperty(value = "数据更新时间")
    private LocalDateTime updateTime;
}
