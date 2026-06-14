package com.solar.ops.prediction.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.prediction.config.WeatherProperties;
import com.solar.ops.prediction.dto.WeatherDataDTO;
import com.solar.ops.prediction.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "weather.provider", havingValue = "amap", matchIfMissing = true)
public class AmapWeatherServiceImpl implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(AmapWeatherServiceImpl.class);

    @Autowired
    private WeatherProperties weatherProperties;

    private static final Map<String, Double> WEATHER_CLOUD_MAP = new HashMap<>();

    static {
        WEATHER_CLOUD_MAP.put("晴", 10.0);
        WEATHER_CLOUD_MAP.put("多云", 50.0);
        WEATHER_CLOUD_MAP.put("阴", 90.0);
        WEATHER_CLOUD_MAP.put("小雨", 80.0);
        WEATHER_CLOUD_MAP.put("中雨", 90.0);
        WEATHER_CLOUD_MAP.put("大雨", 95.0);
        WEATHER_CLOUD_MAP.put("雷阵雨", 85.0);
        WEATHER_CLOUD_MAP.put("小雪", 70.0);
        WEATHER_CLOUD_MAP.put("中雪", 85.0);
        WEATHER_CLOUD_MAP.put("大雪", 95.0);
        WEATHER_CLOUD_MAP.put("雾", 90.0);
        WEATHER_CLOUD_MAP.put("霾", 75.0);
    }

    @Override
    public WeatherDataDTO fetchRealtimeWeather(Long stationId, String cityCode, Double longitude, Double latitude) {
        try {
            WeatherProperties.AmapProperties amap = weatherProperties.getAmap();
            if (StrUtil.isBlank(amap.getApiKey()) || "your-amap-api-key".equals(amap.getApiKey())) {
                return generateMockData(stationId);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("key", amap.getApiKey());
            params.put("city", cityCode != null ? cityCode : "110000");
            params.put("extensions", "base");

            String response = HttpUtil.get(amap.getWeatherUrl(), params);
            JSONObject json = JSONUtil.parseObj(response);

            if (json.getInt("status") != 1) {
                log.warn("高德天气API返回异常: {}", json.getStr("info"));
                return generateMockData(stationId);
            }

            JSONObject lives = json.getJSONArray("lives").getJSONObject(0);

            WeatherDataDTO dto = new WeatherDataDTO();
            dto.setStationId(stationId);
            dto.setFetchTime(LocalDateTime.now());
            dto.setTemperature(parseDouble(lives.getStr("temperature")));
            dto.setHumidity(parseDouble(lives.getStr("humidity")));
            dto.setWeather(lives.getStr("weather"));
            dto.setWindDirection(lives.getStr("winddirection"));
            dto.setWindSpeed(parseDouble(lives.getStr("windpower")));
            dto.setPressure(parseDouble(lives.getStr("pressure")));
            Double cloudCover = estimateCloudCover(dto.getWeather());
            dto.setCloudCover(cloudCover);
            dto.setIrradiance(calculateIrradiance(cloudCover, LocalDateTime.now()));

            return dto;
        } catch (Exception e) {
            log.error("获取高德天气数据失败", e);
            return generateMockData(stationId);
        }
    }

    @Override
    public WeatherDataDTO fetchHourlyForecast(Long stationId, String cityCode, Double longitude, Double latitude, int hours) {
        try {
            WeatherProperties.AmapProperties amap = weatherProperties.getAmap();
            if (StrUtil.isBlank(amap.getApiKey()) || "your-amap-api-key".equals(amap.getApiKey())) {
                return generateMockForecastData(stationId, hours);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("key", amap.getApiKey());
            params.put("city", cityCode != null ? cityCode : "110000");
            params.put("extensions", "all");

            String response = HttpUtil.get(amap.getWeatherUrl(), params);
            JSONObject json = JSONUtil.parseObj(response);

            if (json.getInt("status") != 1) {
                log.warn("高德天气API返回异常: {}", json.getStr("info"));
                return generateMockForecastData(stationId, hours);
            }

            WeatherDataDTO dto = fetchRealtimeWeather(stationId, cityCode, longitude, latitude);

            List<WeatherDataDTO.HourlyWeather> hourlyList = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 1; i <= hours; i++) {
                LocalDateTime forecastTime = now.plusHours(i);
                WeatherDataDTO.HourlyWeather hw = new WeatherDataDTO.HourlyWeather();
                hw.setForecastTime(forecastTime);
                hw.setTemperature(dto.getTemperature() + (Math.random() - 0.5) * 4);
                hw.setHumidity(Math.max(20, Math.min(95, dto.getHumidity() + (Math.random() - 0.5) * 20)));
                hw.setWeather(dto.getWeather());
                Double cloudCover = dto.getCloudCover() + (Math.random() - 0.5) * 20;
                hw.setCloudCover(cloudCover);
                hw.setIrradiance(calculateIrradiance(cloudCover, forecastTime));
                hourlyList.add(hw);
            }

            dto.setHourlyForecast(hourlyList);
            return dto;
        } catch (Exception e) {
            log.error("获取高德天气预报数据失败", e);
            return generateMockForecastData(stationId, hours);
        }
    }

    private Double calculateIrradiance(Double cloudCover, LocalDateTime time) {
        if (time == null) {
            time = LocalDateTime.now();
        }
        int hour = time.getHour();
        if (hour < 6 || hour > 18) {
            return 0.0;
        }

        double solarAltitude = Math.sin(Math.PI * (hour - 6) / 12);
        double cloudFactor = cloudCover != null ? (1.0 - cloudCover / 150.0) : 0.7;
        double solarConstant = 1000.0;
        double irradiance = solarConstant * solarAltitude * cloudFactor;

        return Math.round(Math.max(0, irradiance) * 100.0) / 100.0;
    }

    private Double estimateCloudCover(String weather) {
        return WEATHER_CLOUD_MAP.getOrDefault(weather, 50.0);
    }

    private Double parseDouble(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WeatherDataDTO generateMockData(Long stationId) {
        LocalDateTime now = LocalDateTime.now();
        WeatherDataDTO dto = new WeatherDataDTO();
        dto.setStationId(stationId);
        dto.setFetchTime(now);
        dto.setTemperature(20 + Math.random() * 10);
        dto.setHumidity(40 + Math.random() * 30);
        dto.setWeather("晴");
        dto.setWindDirection("东南");
        dto.setWindSpeed(2 + Math.random() * 3);
        dto.setPressure(1013.0);
        Double cloudCover = 10.0;
        dto.setCloudCover(cloudCover);
        dto.setIrradiance(calculateIrradiance(cloudCover, now));
        return dto;
    }

    private WeatherDataDTO generateMockForecastData(Long stationId, int hours) {
        WeatherDataDTO dto = generateMockData(stationId);
        List<WeatherDataDTO.HourlyWeather> hourlyList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= hours; i++) {
            LocalDateTime forecastTime = now.plusHours(i);
            WeatherDataDTO.HourlyWeather hw = new WeatherDataDTO.HourlyWeather();
            hw.setForecastTime(forecastTime);
            hw.setTemperature(20 + Math.random() * 10);
            hw.setHumidity(40 + Math.random() * 30);
            hw.setWeather("晴");
            Double cloudCover = 10.0;
            hw.setCloudCover(cloudCover);
            hw.setIrradiance(calculateIrradiance(cloudCover, forecastTime));
            hourlyList.add(hw);
        }

        dto.setHourlyForecast(hourlyList);
        return dto;
    }
}
