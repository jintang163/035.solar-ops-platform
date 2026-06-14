package com.solar.ops.prediction.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherOverviewVO {

    private Long stationId;

    private String stationName;

    private LocalDateTime fetchTime;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private BigDecimal irradiance;

    private BigDecimal cloudCover;

    private String weather;

    private String windDirection;

    private BigDecimal windSpeed;

    private List<HourlyWeatherVO> hourlyForecast;

    @Data
    public static class HourlyWeatherVO {
        private LocalDateTime forecastTime;
        private BigDecimal temperature;
        private BigDecimal humidity;
        private BigDecimal irradiance;
        private BigDecimal cloudCover;
        private String weather;
    }
}
