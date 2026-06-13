package com.solar.ops.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "netty.tcp")
public class NettyProperties {

    private int port = 9000;
    private int bossThreads = 1;
    private int workerThreads = 8;
    private boolean keepAlive = true;
    private int soBacklog = 1024;
    private int idleTimeout = 300;
}
