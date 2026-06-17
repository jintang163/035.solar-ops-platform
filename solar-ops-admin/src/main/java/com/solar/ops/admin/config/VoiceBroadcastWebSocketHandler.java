package com.solar.ops.admin.config;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class VoiceBroadcastWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceBroadcastWebSocketHandler.class);

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("[语音播报] WebSocket连接建立，当前连接数：{}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("[语音播报] 收到客户端消息：{}", payload);
        if ("heartbeat".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("[语音播报] WebSocket连接关闭，当前连接数：{}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("[语音播报] WebSocket传输错误", exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }

    public void broadcastMessage(Map<String, Object> data) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "voice_broadcast");
            message.put("data", data);
            String jsonData = JSON.toJSONString(message);
            TextMessage textMessage = new TextMessage(jsonData);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("[语音播报] 发送数据失败，sessionId：{}", session.getId(), e);
                    }
                }
            }
            logger.debug("[语音播报] WebSocket推送数据完成，连接数：{}", sessions.size());
        } catch (Exception e) {
            logger.error("[语音播报] 广播消息失败", e);
        }
    }

    public static int getSessionCount() {
        return sessions.size();
    }
}
