package com.solar.ops.prediction.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
@ConditionalOnProperty(name = "weather.provider", havingValue = "qweather")
public class QWeatherServiceImpl implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(QWeatherServiceImpl.class);

    @Autowired
    private WeatherProperties weatherProperties;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX");

    private static final Map<String, Double> TEXT_CLOUD_MAP = new HashMap<>();

    static {
        TEXT_CLOUD_MAP.put("晴", 10.0);
        TEXT_CLOUD_MAP.put("多云", 50.0);
        TEXT_CLOUD_MAP.put("阴", 90.0);
        TEXT_CLOUD_MAP.put("小雨", 80.0);
        TEXT_CLOUD_MAP.put("中雨", 90.0);
        TEXT_CLOUD_MAP.put("大雨", 95.0);
        TEXT_CLOUD_MAP.put("雷阵雨", 85.0);
        TEXT_CLOUD_MAP.put("小雪", 70.0);
        TEXT_CLOUD_MAP.put("中雪", 85.0);
        TEXT_CLOUD_MAP.put("大雪", 95.0);
        TEXT_CLOUD_MAP.put("雾", 90.0);
        TEXT_CLOUD_MAP.put("霾", 75.0);
    }

    @Override
    public WeatherDataDTO fetchRealtimeWeather(Long stationId, String cityCode, Double longitude, Double latitude) {
        try {
            WeatherProperties.QWeatherProperties qw = weatherProperties.getQweather();
            if (StrUtil.isBlank(qw.getApiKey()) || "your-qweather-api-key".equals(qw.getApiKey())) {
                return generateMockData(stationId);
            }

            String location = longitude != null && latitude != null
                    ? longitude + "," + latitude
                    : (cityCode != null ? cityCode : "101010100");

            Map<String, Object> params = new HashMap<>();
            params.put("key", qw.getApiKey());
            params.put("location", location);

            String response = HttpUtil.get(qw.getWeatherNowUrl(), params);
            JSONObject json = JSONUtil.parseObj(response);

            if (!"200".equals(json.getStr("code"))) {
                log.warn("和风天气API返回异常: code={}", json.getStr("code"));
                return generateMockData(stationId);
            }

            JSONObject now = json.getJSONObject("now");

            WeatherDataDTO dto = new WeatherDataDTO();
            dto.setStationId(stationId);
            dto.setFetchTime(LocalDateTime.now());
            dto.setTemperature(parseDouble(now.getStr("temp")));
            dto.setHumidity(parseDouble(now.getStr("humidity")));
            dto.setWeather(now.getStr("text"));
            dto.setWindDirection(now.getStr("windDir"));
            dto.setWindSpeed(parseDouble(now.getStr("windSpeed")));
            dto.setPressure(parseDouble(now.getStr("pressure")));
            Double cloudCover = parseDouble(now.getStr("cloud"));
            if (cloudCover == null) {
                cloudCover = estimateCloudCover(dto.getWeather());
            }
            dto.setCloudCover(cloudCover);
            dto.setIrradiance(calculateIrradiance(cloudCover, LocalDateTime.now()));

            return dto;
        } catch (Exception e) {
            log.error("获取和风天气数据失败", e);
            return generateMockData(stationId);
        }
    }

    @Override
    public WeatherDataDTO fetchHourlyForecast(Long stationId, String cityCode, Double longitude, Double latitude, int hours) {
        try {
            WeatherProperties.QWeatherProperties qw = weatherProperties.getQweather();
            if (StrUtil.isBlank(qw.getApiKey()) || "your-qweather-api-key".equals(qw.getApiKey())) {
                return generateMockForecastData(stationId, hours);
            }

            WeatherDataDTO dto = fetchRealtimeWeather(stationId, cityCode, longitude, latitude);
            List<WeatherDataDTO.HourlyWeather> hourlyList = new ArrayList<>();

            String location = longitude != null && latitude != null
                    ? longitude + "," + latitude
                    : (cityCode != null ? cityCode : "101010100");

            Map<String, Object> params = new HashMap<>();
            params.put("key", qw.getApiKey());
            params.put("location", location);

            String response = HttpUtil.get("https://devapi.qweather.com/v7/weather/24h", params);
            JSONObject json = JSONUtil.parseObj(response);

            if ("200".equals(json.getStr("code"))) {
                JSONArray hourly = json.getJSONArray("hourly");
                int count = Math.min(hours, hourly.size());
                for (int i = 0; i < count; i++) {
                    JSONObject h = hourly.getJSONObject(i);
                    WeatherDataDTO.HourlyWeather hw = new WeatherDataDTO.HourlyWeather();
                    hw.setForecastTime(LocalDateTime.parse(h.getStr("fxTime"), FORMATTER));
                    hw.setTemperature(parseDouble(h.getStr("temp")));
                    hw.setHumidity(parseDouble(h.getStr("humidity")));
                    hw.setWeather(h.getStr("text"));
                    Double cloudCover = parseDouble(h.getStr("cloud"));
                    if (cloudCover == null) {
                        cloudCover = estimateCloudCover(hw.getWeather());
                    }
                    hw.setCloudCover(cloudCover);
                    hw.setIrradiance(calculateIrradiance(cloudCover, hw.getForecastTime()));
                    hourlyList.add(hw);
                }
            } else {
                LocalDateTime now = LocalDateTime.now();
                for (int i = 1; i <= hours; i++) {
                    LocalDateTime forecastTime = now.plusHours(i);
                    WeatherDataDTO.HourlyWeather hw = new WeatherDataDTO.HourlyWeather();
                    hw.setForecastTime(forecastTime);
                    hw.setTemperature(dto.getTemperature() + (Math.random() - 0.5) * 4);
                    hw.setHumidity(dto.getHumidity() + (Math.random() - 0.5) * 20);
                    hw.setWeather(dto.getWeather());
                    Double cloudCover = dto.getCloudCover() + (Math.random() - 0.5) * 20;
                    hw.setCloudCover(cloudCover);
                    hw.setIrradiance(calculateIrradiance(cloudCover, forecastTime));
                    hourlyList.add(hw);
                }
            }

            dto.setHourlyForecast(hourlyList);
            return dto;
        } catch (Exception e) {
            log.error("获取和风天气预报数据失败", e);
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
        return TEXT_CLOUD_MAP.getOrDefault(weather, 50.0);
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
        dto.setTemperature(22 + Math.random() * 8);
        dto.setHumidity(45 + Math.random() * 25);
        dto.setWeather("晴");
        dto.setWindDirection("东南");
        dto.setWindSpeed(2.5 + Math.random() * 2.5);
        dto.setPressure(1013.0);
        Double cloudCover = 15.0;
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
            hw.setTemperature(22 + Math.random() * 8);
            hw.setHumidity(45 + Math.random() * 25);
            hw.setWeather("晴");
            Double cloudCover = 15.0;
            hw.setCloudCover(cloudCover);
            hw.setIrradiance(calculateIrradiance(cloudCover, forecastTime));
            hourlyList.add(hw);
        }

        dto.setHourlyForecast(hourlyList);
        return dto;
    }
}
