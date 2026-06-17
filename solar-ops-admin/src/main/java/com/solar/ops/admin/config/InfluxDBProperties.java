package com.solar.ops.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBProperties {
    private String url;
    private String username;
    private String password;
    private String database;
    private String retentionPolicy = "autogen";
    private int connectTimeout = 10;
    private int readTimeout = 30;
    private int writeTimeout = 10;
}
