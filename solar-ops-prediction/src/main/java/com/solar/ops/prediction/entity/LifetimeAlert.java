package com.solar.ops.prediction.entity;

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
@TableName("lifetime_alert")
@ApiModel(value = "LifetimeAlert对象", description = "设备寿命预警")
public class LifetimeAlert extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "预警时间")
    private LocalDateTime alertTime;

    @ApiModelProperty(value = "预警类型：1-寿命预警 2-备件更换建议")
    private Integer alertType;

    @ApiModelProperty(value = "预警级别：1-低 2-中 3-高 4-紧急")
    private Integer alertLevel;

    @ApiModelProperty(value = "预警标题")
    private String alertTitle;

    @ApiModelProperty(value = "预警内容")
    private String alertContent;

    @ApiModelProperty(value = "剩余寿命(天)")
    private Integer remainingLifeDays;

    @ApiModelProperty(value = "当前健康度")
    private BigDecimal currentHealth;

    @ApiModelProperty(value = "建议备件")
    private String sparePart;

    @ApiModelProperty(value = "预估费用")
    private BigDecimal estimatedCost;

    @ApiModelProperty(value = "状态：0-未处理 1-已处理 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "处理时间")
    private LocalDateTime handleTime;

    @ApiModelProperty(value = "处理人")
    private String handlePerson;

    @ApiModelProperty(value = "处理备注")
    private String handleRemark;
}
