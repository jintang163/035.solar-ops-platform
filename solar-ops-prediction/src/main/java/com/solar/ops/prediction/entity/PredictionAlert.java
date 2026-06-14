package com.solar.ops.prediction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prediction_alert")
public class PredictionAlert extends BaseEntity {

    private Long stationId;

    private Long inverterId;

    private Long predictionId;

    private LocalDateTime alertTime;

    private LocalDateTime targetTime;

    private Integer alertType;

    private Integer alertLevel;

    private String alertContent;

    private BigDecimal predictedValue;

    private BigDecimal actualValue;

    private BigDecimal deviationRate;

    private BigDecimal threshold;

    private Integer status;

    private LocalDateTime handleTime;

    private String handleRemark;

    private String rootCause;
}
