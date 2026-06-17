package com.solar.ops.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "voice.speaker")
public class VoiceSpeakerProperties {
    private Boolean enabled = false;
    private String apiUrl;
    private String apiKey;
    private List<String> deviceIds;
    private Integer timeout = 10;
    private String pushMethod = "http";
    private String mqttBroker;
    private String mqttTopic = "solar/speaker/broadcast";
}
