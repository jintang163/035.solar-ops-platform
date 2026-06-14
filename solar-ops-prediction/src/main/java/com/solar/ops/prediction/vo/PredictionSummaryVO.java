package com.solar.ops.prediction.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PredictionSummaryVO {

    private Long stationId;

    private String stationName;

    private Integer totalPredictions;

    private BigDecimal avgAccuracy;

    private Integer alertCount;

    private Integer pendingAlertCount;

    private BigDecimal todayPredictedEnergy;

    private BigDecimal todayActualEnergy;

    private BigDecimal energyDeviationRate;
}
