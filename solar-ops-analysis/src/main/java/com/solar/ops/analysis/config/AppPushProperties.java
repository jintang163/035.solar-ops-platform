package com.solar.ops.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.push")
public class AppPushProperties {

    private boolean enabled = true;

    private String provider = "unipush";

    private String appId;

    private String appKey;

    private String appSecret;

    private String masterSecret;

    private String apiUrl = "https://restapi.getui.com/v2/";
}
