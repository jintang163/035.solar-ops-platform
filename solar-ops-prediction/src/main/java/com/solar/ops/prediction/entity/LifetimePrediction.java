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
@TableName("lifetime_prediction")
@ApiModel(value = "LifetimePrediction对象", description = "设备寿命预测记录")
public class LifetimePrediction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "电站ID")
    private Long stationId;

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "预测时间")
    private LocalDateTime predictionTime;

    @ApiModelProperty(value = "当前健康度评分(0-1)")
    private BigDecimal currentHealthScore;

    @ApiModelProperty(value = "预测剩余寿命(天)")
    private Integer remainingLifeDays;

    @ApiModelProperty(value = "预测未来天数")
    private Integer forecastDays;

    @ApiModelProperty(value = "健康度趋势数据(JSON数组)")
    private String healthTrend;

    @ApiModelProperty(value = "置信度数据(JSON数组)")
    private String confidenceTrend;

    @ApiModelProperty(value = "模型版本")
    private String modelVersion;

    @ApiModelProperty(value = "是否需要更换备件(0-否 1-是)")
    private Integer replacementAdvice;

    @ApiModelProperty(value = "预警级别：1-正常 2-注意 3-警告 4-紧急")
    private Integer alertLevel;

    @ApiModelProperty(value = "预测说明")
    private String remark;
}
