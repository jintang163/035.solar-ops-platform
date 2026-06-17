package com.solar.ops.admin.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.admin.entity.AlarmMessageLog;
import com.solar.ops.admin.service.AlarmMessageLogService;
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
    private final AlarmMessageLogService alarmMessageLogService;

    @Override
    public void onMessage(String message) {
        log.info("[语音播报] 收到告警消息: {}", message);
        AlarmMessageLog consumeLog = alarmMessageLogService.beginConsume("fault-alarm-topic", message);
        try {
            JSONObject jsonObject = JSON.parseObject(message);
            Long stationId = jsonObject.getLong("stationId");
            Long inverterId = jsonObject.getLong("inverterId");
            String faultCode = jsonObject.getString("faultCode");
            String description = jsonObject.getString("description");
            String alarmType = jsonObject.getString("alarmType");

            JSONArray reasons = jsonObject.getJSONArray("reasons");
            if (reasons != null && !reasons.isEmpty() && !StringUtils.hasText(description)) {
                StringBuilder descBuilder = new StringBuilder();
                for (int i = 0; i < reasons.size(); i++) {
                    if (i > 0) {
                        descBuilder.append("；");
                    }
                    descBuilder.append(reasons.getString(i));
                }
                description = descBuilder.toString();
            }

            Double temperature = jsonObject.getDouble("temperature");

            if (stationId == null) {
                log.warn("[语音播报] stationId为空，跳过播报");
                alarmMessageLogService.markSuccess(consumeLog.getId());
                return;
            }

            Integer broadcastType = resolveBroadcastType(faultCode, alarmType, description, temperature);
            Integer alarmLevel = resolveAlarmLevel(faultCode, alarmType, description, temperature);

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
            alarmMessageLogService.markSuccess(consumeLog.getId());
        } catch (Exception e) {
            log.error("[语音播报] 处理告警消息失败: {}", message, e);
            alarmMessageLogService.markFailed(consumeLog.getId(), e.getMessage());
            throw e;
        }
    }

    private Integer resolveBroadcastType(String faultCode, String alarmType, String description, Double temperature) {
        if ("INV_NO_COMM".equals(faultCode) || "4".equals(faultCode)
                || (description != null && (description.contains("通讯中断") || description.contains("离线")))) {
            return 1;
        }
        if ("INV_OVER_TEMP".equals(faultCode) || "3".equals(faultCode) || "PANEL_HOT_SPOT".equals(faultCode) || "8".equals(faultCode)
                || (description != null && (description.contains("温度过高") || description.contains("热斑") || description.contains("火灾")))
                || (temperature != null && temperature > 85)) {
            return 2;
        }
        if ("INV_SHORT_CIRCUIT".equals(faultCode) || "2".equals(faultCode)
                || "DC_INSULATION_FAULT".equals(faultCode) || "10".equals(faultCode)) {
            return 4;
        }
        if ("INV_OVER_VOLT".equals(faultCode) || "1".equals(faultCode)) {
            return 3;
        }
        if (description != null && description.contains("功率超限")) {
            return 5;
        }
        if ("fault_code".equals(alarmType) && StringUtils.hasText(faultCode)) {
            return 3;
        }
        if ("data_abnormal".equals(alarmType)) {
            return 5;
        }
        return null;
    }

    private Integer resolveAlarmLevel(String faultCode, String alarmType, String description, Double temperature) {
        if ("INV_SHORT_CIRCUIT".equals(faultCode) || "2".equals(faultCode)
                || "DC_INSULATION_FAULT".equals(faultCode) || "10".equals(faultCode)
                || "INV_OVER_TEMP".equals(faultCode) || "3".equals(faultCode)
                || "PANEL_HOT_SPOT".equals(faultCode) || "8".equals(faultCode)) {
            return 4;
        }
        if (temperature != null && temperature > 85) {
            return 4;
        }
        if (description != null && description.contains("温度过高")) {
            return 4;
        }
        if ("INV_NO_COMM".equals(faultCode) || "4".equals(faultCode)
                || "INV_GRID_OFF".equals(faultCode) || "5".equals(faultCode)
                || "STRING_NO_OUTPUT".equals(faultCode) || "7".equals(faultCode)
                || "FAN_FAILURE".equals(faultCode) || "9".equals(faultCode)
                || "INV_OVER_VOLT".equals(faultCode) || "1".equals(faultCode)
                || "fault_code".equals(alarmType)) {
            return 3;
        }
        if (description != null && description.contains("功率超限")) {
            return 3;
        }
        if ("data_abnormal".equals(alarmType)) {
            return 2;
        }
        return 3;
    }
}
