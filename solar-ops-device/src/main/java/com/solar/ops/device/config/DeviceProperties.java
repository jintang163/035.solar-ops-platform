package com.solar.ops.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "device")
public class DeviceProperties {

    private int offlineTimeout = 300;

    private AbnormalProperties abnormal = new AbnormalProperties();

    @Data
    public static class AbnormalProperties {
        private double voltageMax = 450;
        private double voltageMin = 0;
        private double currentMax = 100;
        private double currentMin = 0;
        private double temperatureMax = 85;
        private double powerMax = 5000;
    }
}
