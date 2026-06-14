package com.solar.ops.prediction.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PredictionInputDTO {

    private Long stationId;

    private Long inverterId;

    private Integer horizon;

    private List<FeaturePoint> features;

    @Data
    public static class FeaturePoint {
        private LocalDateTime time;
        private Double temperature;
        private Double humidity;
        private Double irradiance;
        private Double cloudCover;
        private Double hour;
        private Double dayOfYear;
        private Double historicalPower;
    }
}
