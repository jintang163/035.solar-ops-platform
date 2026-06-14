package com.solar.ops.prediction.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PredictionCurveVO {

    private Long stationId;

    private Long inverterId;

    private List<String> timeAxis;

    private List<BigDecimal> predictedPower;

    private List<BigDecimal> actualPower;

    private List<BigDecimal> deviationRate;

    private BigDecimal maxDeviation;

    private BigDecimal avgDeviation;

    private Integer alertCount;
}
