package com.solar.ops.device.websocket;

import cn.hutool.json.JSONUtil;
import com.solar.ops.device.dto.InverterDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/ws/device/{stationId}")
public class DeviceDataWebSocket {

    private static final Logger log = LoggerFactory.getLogger(DeviceDataWebSocket.class);

    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    @OnOpen
    public void onOpen(Session session, @PathParam("stationId") String stationId) {
        String sessionId = session.getId();
        sessionMap.put(sessionId, session);
        onlineCount.incrementAndGet();
        log.info("WebSocket连接建立: sessionId={}, stationId={}, 当前连接数={}",
                sessionId, stationId, onlineCount.get());
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        sessionMap.remove(sessionId);
        onlineCount.decrementAndGet();
        log.info("WebSocket连接关闭: sessionId={}, 当前连接数={}", sessionId, onlineCount.get());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("收到WebSocket消息: sessionId={}, message={}", session.getId(), message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误: sessionId={}", session.getId(), error);
    }

    public void broadcast(InverterDataDTO data) {
        if (data == null) {
            return;
        }

        String message = JSONUtil.toJsonStr(data);
        for (Session session : sessionMap.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.warn("WebSocket发送消息失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }

    public void sendToSession(String sessionId, String message) {
        Session session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.warn("WebSocket发送消息失败: sessionId={}", sessionId, e);
            }
        }
    }

    public int getOnlineCount() {
        return onlineCount.get();
    }
}
