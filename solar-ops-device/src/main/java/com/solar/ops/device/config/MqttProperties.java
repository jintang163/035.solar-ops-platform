package com.solar.ops.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String broker;
    private String clientId;
    private String username;
    private String password;
    private TopicProperties topic = new TopicProperties();

    @Data
    public static class TopicProperties {
        private String deviceData;
        private String deviceStatus;
    }
}
