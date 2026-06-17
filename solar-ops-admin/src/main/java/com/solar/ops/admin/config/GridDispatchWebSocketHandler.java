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
public class GridDispatchWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GridDispatchWebSocketHandler.class);

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("[电网调度] WebSocket连接建立，当前连接数：{}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("[电网调度] 收到客户端消息：{}", payload);
        if ("heartbeat".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("[电网调度] WebSocket连接关闭，当前连接数：{}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("[电网调度] WebSocket传输错误", exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }

    public void broadcastMessage(String type, Object data) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("data", data);
            String jsonData = JSON.toJSONString(message);
            TextMessage textMessage = new TextMessage(jsonData);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("[电网调度] 发送数据失败，sessionId：{}", session.getId(), e);
                    }
                }
            }
            logger.debug("[电网调度] WebSocket推送数据完成，类型：{}，连接数：{}", type, sessions.size());
        } catch (Exception e) {
            logger.error("[电网调度] 广播消息失败，类型：{}", type, e);
        }
    }

    public void pushCommandUpdate(Object commandVO) {
        broadcastMessage("command-update", commandVO);
    }

    public void pushUploadStatus(boolean success, Object record) {
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        data.put("record", record);
        broadcastMessage("upload-status", data);
    }

    public static int getSessionCount() {
        return sessions.size();
    }
}
