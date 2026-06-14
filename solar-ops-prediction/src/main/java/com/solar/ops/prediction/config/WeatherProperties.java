package com.solar.ops.prediction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    private String provider = "amap";

    private AmapProperties amap = new AmapProperties();

    private QWeatherProperties qweather = new QWeatherProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public AmapProperties getAmap() {
        return amap;
    }

    public void setAmap(AmapProperties amap) {
        this.amap = amap;
    }

    public QWeatherProperties getQweather() {
        return qweather;
    }

    public void setQweather(QWeatherProperties qweather) {
        this.qweather = qweather;
    }

    public static class AmapProperties {
        private String apiKey;
        private String weatherUrl;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWeatherUrl() {
            return weatherUrl;
        }

        public void setWeatherUrl(String weatherUrl) {
            this.weatherUrl = weatherUrl;
        }
    }

    public static class QWeatherProperties {
        private String apiKey;
        private String weatherNowUrl;
        private String weatherForecastUrl;
        private String weatherIndicesUrl;
        private String weatherHourlyUrl;
        private String solarRadiationUrl;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWeatherNowUrl() {
            return weatherNowUrl;
        }

        public void setWeatherNowUrl(String weatherNowUrl) {
            this.weatherNowUrl = weatherNowUrl;
        }

        public String getWeatherForecastUrl() {
            return weatherForecastUrl;
        }

        public void setWeatherForecastUrl(String weatherForecastUrl) {
            this.weatherForecastUrl = weatherForecastUrl;
        }

        public String getWeatherIndicesUrl() {
            return weatherIndicesUrl;
        }

        public void setWeatherIndicesUrl(String weatherIndicesUrl) {
            this.weatherIndicesUrl = weatherIndicesUrl;
        }

        public String getWeatherHourlyUrl() {
            return weatherHourlyUrl;
        }

        public void setWeatherHourlyUrl(String weatherHourlyUrl) {
            this.weatherHourlyUrl = weatherHourlyUrl;
        }

        public String getSolarRadiationUrl() {
            return solarRadiationUrl;
        }

        public void setSolarRadiationUrl(String solarRadiationUrl) {
            this.solarRadiationUrl = solarRadiationUrl;
        }
    }
}
