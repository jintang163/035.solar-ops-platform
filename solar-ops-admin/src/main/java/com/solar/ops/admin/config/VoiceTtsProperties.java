package com.solar.ops.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "voice.tts")
public class VoiceTtsProperties {
    private Boolean enabled = false;
    private String provider = "xunfei";
    private String xunfeiAppId;
    private String xunfeiApiKey;
    private String xunfeiApiSecret;
    private String xunfeiVoiceName = "xiaoyan";
    private Integer xunfeiSpeed = 50;
    private Integer xunfeiVolume = 50;
    private String audioSavePath = "/data/voice-broadcast/audio";
    private String audioBaseUrl = "http://localhost:8080/admin/voice-broadcast/audio";
}
