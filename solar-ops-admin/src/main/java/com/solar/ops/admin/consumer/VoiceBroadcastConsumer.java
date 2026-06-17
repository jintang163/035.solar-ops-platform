package com.solar.ops.admin.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.admin.service.VoiceBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "fault-alarm-topic", consumerGroup = "voice-broadcast-consumer-group")
public class VoiceBroadcastConsumer implements RocketMQListener<String> {

    private final VoiceBroadcastService voiceBroadcastService;

    @Override
    public void onMessage(String message) {
        log.info("[语音播报] 收到告警消息: {}", message);
        try {
            JSONObject jsonObject = JSON.parseObject(message);
            Long stationId = jsonObject.getLong("stationId");
            Long inverterId = jsonObject.getLong("inverterId");
            String faultCode = jsonObject.getString("faultCode");
            String description = jsonObject.getString("description");
            String alarmType = jsonObject.getString("alarmType");

            if (stationId == null) {
                log.warn("[语音播报] stationId为空，跳过播报");
                return;
            }

            Integer broadcastType = resolveBroadcastType(faultCode, alarmType, description);
            Integer alarmLevel = resolveAlarmLevel(faultCode, alarmType);

            if (broadcastType != null && alarmLevel >= 3) {
                voiceBroadcastService.triggerBroadcast(
                        broadcastType,
                        alarmLevel,
                        stationId,
                        inverterId,
                        faultCode,
                        description,
                        null
                );
                log.info("[语音播报] 已触发播报, stationId={}, inverterId={}, type={}, level={}",
                        stationId, inverterId, broadcastType, alarmLevel);
            } else {
                log.debug("[语音播报] 告警级别或类型不满足播报条件，跳过, alarmLevel={}, type={}", alarmLevel, broadcastType);
            }
        } catch (Exception e) {
            log.error("[语音播报] 处理告警消息失败: {}", message, e);
        }
    }

    private Integer resolveBroadcastType(String faultCode, String alarmType, String description) {
        if ("INV_NO_COMM".equals(faultCode) || "4".equals(faultCode)
                || (description != null && (description.contains("通讯中断") || description.contains("离线")))) {
            return 1;
        }
        if ("INV_OVER_TEMP".equals(faultCode) || "3".equals(faultCode) || "PANEL_HOT_SPOT".equals(faultCode) || "8".equals(faultCode)
                || (description != null && (description.contains("温度过高") || description.contains("热斑") || description.contains("火灾")))) {
            return 2;
        }
        if ("INV_SHORT_CIRCUIT".equals(faultCode) || "2".equals(faultCode)
                || "DC_INSULATION_FAULT".equals(faultCode) || "10".equals(faultCode)) {
            return 4;
        }
        if ("fault_code".equals(alarmType) && StringUtils.hasText(faultCode)) {
            return 3;
        }
        if ("data_abnormal".equals(alarmType)) {
            return 5;
        }
        return null;
    }

    private Integer resolveAlarmLevel(String faultCode, String alarmType) {
        if ("INV_SHORT_CIRCUIT".equals(faultCode) || "2".equals(faultCode)
                || "DC_INSULATION_FAULT".equals(faultCode) || "10".equals(faultCode)
                || "INV_OVER_TEMP".equals(faultCode) || "3".equals(faultCode)
                || "PANEL_HOT_SPOT".equals(faultCode) || "8".equals(faultCode)) {
            return 4;
        }
        if ("INV_NO_COMM".equals(faultCode) || "4".equals(faultCode)
                || "INV_GRID_OFF".equals(faultCode) || "5".equals(faultCode)
                || "STRING_NO_OUTPUT".equals(faultCode) || "7".equals(faultCode)
                || "FAN_FAILURE".equals(faultCode) || "9".equals(faultCode)
                || "fault_code".equals(alarmType)) {
            return 3;
        }
        if ("data_abnormal".equals(alarmType)) {
            return 2;
        }
        return 3;
    }
}
