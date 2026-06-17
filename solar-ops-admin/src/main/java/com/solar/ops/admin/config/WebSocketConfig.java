package com.solar.ops.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private DashboardWebSocketHandler dashboardWebSocketHandler;

    @Resource
    private VoiceBroadcastWebSocketHandler voiceBroadcastWebSocketHandler;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dashboardWebSocketHandler, "/websocket/dashboard")
                .setAllowedOrigins("*");
        registry.addHandler(voiceBroadcastWebSocketHandler, "/websocket/voice-broadcast")
                .setAllowedOrigins("*");
    }
}
