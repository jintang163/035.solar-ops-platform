package com.solar.ops.device.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class InfluxDBConfig {

    @Autowired
    private InfluxDBProperties properties;

    @Bean
    public InfluxDB influxDB() {
        InfluxDB influxDB = InfluxDBFactory.connect(
                properties.getUrl(),
                properties.getUsername(),
                properties.getPassword()
        );

        influxDB.setDatabase(properties.getDatabase());
        influxDB.setRetentionPolicy(properties.getRetentionPolicy());
        influxDB.enableBatch(100, 500, TimeUnit.MILLISECONDS);
        influxDB.enableGzip();

        return influxDB;
    }
}
