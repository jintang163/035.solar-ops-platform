package com.solar.ops.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "grid.dispatch")
public class GridDispatchProperties {
    private Boolean enabled = true;
    private String defaultProtocol = "IEC104";
    private Integer uploadInterval = 5;
    private Integer commandTimeout = 60;
    private Integer maxRetryCount = 3;
    private String redisCacheKey = "grid:dispatch:cache:";
    private Integer redisCacheTtl = 3600;
    private Boolean alarmOnFail = true;
}
