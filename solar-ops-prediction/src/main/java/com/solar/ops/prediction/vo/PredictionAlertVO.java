package com.solar.ops.prediction.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PredictionAlertVO {

    private Long id;

    private Long stationId;

    private String stationName;

    private Long inverterId;

    private String inverterName;

    private LocalDateTime alertTime;

    private LocalDateTime targetTime;

    private Integer alertType;

    private String alertTypeName;

    private Integer alertLevel;

    private String alertLevelName;

    private String alertContent;

    private BigDecimal predictedValue;

    private BigDecimal actualValue;

    private BigDecimal deviationRate;

    private BigDecimal threshold;

    private Integer status;

    private String statusName;

    private String rootCause;
}
