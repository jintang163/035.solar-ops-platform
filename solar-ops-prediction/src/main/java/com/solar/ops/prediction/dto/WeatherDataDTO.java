package com.solar.ops.prediction.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherDataDTO {

    private Long stationId;

    private String stationName;

    private LocalDateTime fetchTime;

    private Double temperature;

    private Double humidity;

    private Double irradiance;

    private Double cloudCover;

    private String weather;

    private String windDirection;

    private Double windSpeed;

    private Double pressure;

    private List<HourlyWeather> hourlyForecast;

    @Data
    public static class HourlyWeather {
        private LocalDateTime forecastTime;
        private Double temperature;
        private Double humidity;
        private Double irradiance;
        private Double cloudCover;
        private String weather;
    }
}
