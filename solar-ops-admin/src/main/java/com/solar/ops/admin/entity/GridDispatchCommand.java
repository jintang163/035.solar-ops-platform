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
@TableName("grid_dispatch_command")
@ApiModel(value = "电网调度指令")
public class GridDispatchCommand extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("调度指令编号")
    private String commandNo;

    @ApiModelProperty("调度来源：1-调度主站下发 2-人工干预 3-自动调节")
    private Integer commandSource;

    @ApiModelProperty("指令类型：1-有功功率调节 2-无功功率调节 3-电压调节 4-频率调节 5-启停逆变器")
    private Integer commandType;

    @ApiModelProperty("电站ID")
    private Long stationId;

    @ApiModelProperty("电站名称")
    private String stationName;

    @ApiModelProperty("逆变器ID，为空则全站调节")
    private Long inverterId;

    @ApiModelProperty("逆变器名称")
    private String inverterName;

    @ApiModelProperty("目标有功功率(kW)")
    private BigDecimal targetActivePower;

    @ApiModelProperty("目标无功功率(kVar)")
    private BigDecimal targetReactivePower;

    @ApiModelProperty("目标电压(V)")
    private BigDecimal targetVoltage;

    @ApiModelProperty("目标频率(Hz)")
    private BigDecimal targetFrequency;

    @ApiModelProperty("调节比例(0-100%)")
    private Integer adjustRatio;

    @ApiModelProperty("是否启停：true启动 false停机")
    private Boolean startStop;

    @ApiModelProperty("指令下发时间")
    private LocalDateTime issueTime;

    @ApiModelProperty("期望执行时间")
    private LocalDateTime expectTime;

    @ApiModelProperty("执行开始时间")
    private LocalDateTime executeStartTime;

    @ApiModelProperty("执行完成时间")
    private LocalDateTime executeEndTime;

    @ApiModelProperty("指令状态：0-待执行 1-执行中 2-执行成功 3-执行失败 4-已取消 5-超时")
    private Integer status;

    @ApiModelProperty("执行结果描述")
    private String executeResult;

    @ApiModelProperty("实际有功功率(kW)")
    private BigDecimal actualActivePower;

    @ApiModelProperty("实际无功功率(kVar)")
    private BigDecimal actualReactivePower;

    @ApiModelProperty("实际电压(V)")
    private BigDecimal actualVoltage;

    @ApiModelProperty("实际频率(Hz)")
    private BigDecimal actualFrequency;

    @ApiModelProperty("偏差百分比(%)")
    private BigDecimal deviationPercent;

    @ApiModelProperty("优先级：1-紧急 2-高 3-普通 4-低")
    private Integer priority;

    @ApiModelProperty("操作人ID（人工干预时）")
    private Long operatorId;

    @ApiModelProperty("操作人姓名")
    private String operatorName;

    @ApiModelProperty("失败原因")
    private String failReason;

    @ApiModelProperty("备注")
    private String remark;
}
