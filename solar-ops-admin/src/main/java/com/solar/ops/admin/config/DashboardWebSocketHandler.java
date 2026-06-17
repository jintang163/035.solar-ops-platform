package com.solar.ops.admin.config;

import com.alibaba.fastjson.JSON;
import com.solar.ops.admin.service.DashboardService;
import com.solar.ops.admin.vo.DashboardRealTimeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@EnableScheduling
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DashboardWebSocketHandler.class);

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Resource
    private DashboardService dashboardService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket连接建立，当前连接数：{}", sessions.size());
        sendDashboardData(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("收到客户端消息：{}", payload);
        if ("heartbeat".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket连接关闭，当前连接数：{}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误", exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }

    @Scheduled(fixedRate = 10000)
    public void pushDashboardData() {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            DashboardRealTimeVO data = dashboardService.getRealTimeDashboard();
            String jsonData = JSON.toJSONString(data);
            TextMessage message = new TextMessage(jsonData);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        logger.error("发送数据失败，sessionId：{}", session.getId(), e);
                    }
                }
            }
            logger.debug("WebSocket推送数据完成，连接数：{}", sessions.size());
        } catch (Exception e) {
            logger.error("定时推送数据失败", e);
        }
    }

    private void sendDashboardData(WebSocketSession session) {
        try {
            DashboardRealTimeVO data = dashboardService.getRealTimeDashboard();
            String jsonData = JSON.toJSONString(data);
            session.sendMessage(new TextMessage(jsonData));
        } catch (Exception e) {
            logger.error("发送初始数据失败", e);
        }
    }

    public static int getSessionCount() {
        return sessions.size();
    }
}
