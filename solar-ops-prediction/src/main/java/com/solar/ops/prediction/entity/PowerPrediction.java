package com.solar.ops.prediction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("power_prediction")
public class PowerPrediction extends BaseEntity {

    private Long stationId;

    private Long inverterId;

    private LocalDateTime predictTime;

    private LocalDateTime targetTime;

    private Integer horizon;

    private BigDecimal predictedPower;

    private BigDecimal actualPower;

    private BigDecimal deviation;

    private BigDecimal deviationRate;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private BigDecimal irradiance;

    private BigDecimal cloudCover;

    private String modelVersion;

    private Integer status;
}
