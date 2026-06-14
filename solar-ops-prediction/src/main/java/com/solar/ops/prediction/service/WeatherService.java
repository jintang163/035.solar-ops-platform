package com.solar.ops.prediction.service;

import com.solar.ops.prediction.dto.WeatherDataDTO;

public interface WeatherService {

    WeatherDataDTO fetchRealtimeWeather(Long stationId, String cityCode, Double longitude, Double latitude);

    WeatherDataDTO fetchHourlyForecast(Long stationId, String cityCode, Double longitude, Double latitude, int hours);
}
