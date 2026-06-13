package com.solar.ops.device.service;

import com.solar.ops.device.config.DeviceProperties;
import com.solar.ops.device.dto.InverterDataDTO;
import com.solar.ops.device.websocket.DeviceDataWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceDataService {

    private static final Logger log = LoggerFactory.getLogger(DeviceDataService.class);

    private static final String DEVICE_ONLINE_KEY = "device:online:";
    private static final String DEVICE_LAST_DATA_KEY = "device:last:data:";
    private static final String FAULT_ALARM_TOPIC = "fault-alarm-topic";

    @Autowired
    private InfluxDBService influxDBService;

    @Autowired
    private DeviceDataWebSocket deviceDataWebSocket;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DeviceProperties deviceProperties;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private final Map<String, InverterDataDTO> realtimeDataMap = new ConcurrentHashMap<>();
    private final Map<String, Long> deviceOnlineTimeMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkDeviceOffline, 60, 60, TimeUnit.SECONDS);
    }

    public void processDeviceData(InverterDataDTO data) {
        if (data == null || data.getDeviceId() == null) {
            return;
        }

        log.debug("处理设备数据: deviceId={}, power={}", data.getDeviceId(), data.getPower());

        influxDBService.writeInverterData(data);

        realtimeDataMap.put(data.getDeviceId(), data);
        deviceOnlineTimeMap.put(data.getDeviceId(), System.currentTimeMillis());

        updateDeviceOnlineStatus(data.getDeviceId());

        detectAbnormalData(data);

        deviceDataWebSocket.broadcast(data);
    }

    public void handleDeviceOnline(String deviceId, String remoteAddr) {
        log.info("设备上线: deviceId={}, addr={}", deviceId, remoteAddr);
        deviceOnlineTimeMap.put(deviceId, System.currentTimeMillis());
        updateDeviceOnlineStatus(deviceId);
    }

    public void handleDeviceOffline(String deviceId) {
        log.info("设备离线: deviceId={}", deviceId);
        deviceOnlineTimeMap.remove(deviceId);
        redisTemplate.delete(DEVICE_ONLINE_KEY + deviceId);
    }

    private void updateDeviceOnlineStatus(String deviceId) {
        try {
            redisTemplate.opsForValue().set(DEVICE_ONLINE_KEY + deviceId,
                    String.valueOf(System.currentTimeMillis()),
                    deviceProperties.getOfflineTimeout(),
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("更新设备在线状态到Redis失败: {}", deviceId, e);
        }
    }

    private void checkDeviceOffline() {
        long now = System.currentTimeMillis();
        long timeout = deviceProperties.getOfflineTimeout() * 1000L;

        List<String> offlineDevices = new ArrayList<>();
        for (Map.Entry<String, Long> entry : deviceOnlineTimeMap.entrySet()) {
            if (now - entry.getValue() > timeout) {
                offlineDevices.add(entry.getKey());
            }
        }

        for (String deviceId : offlineDevices) {
            deviceOnlineTimeMap.remove(deviceId);
            realtimeDataMap.remove(deviceId);
            log.info("设备超时离线: deviceId={}", deviceId);
        }
    }

    private void detectAbnormalData(InverterDataDTO data) {
        DeviceProperties.AbnormalProperties abnormal = deviceProperties.getAbnormal();
        List<String> abnormalReasons = new ArrayList<>();

        if (data.getVoltage() != null) {
            if (data.getVoltage() > abnormal.getVoltageMax()) {
                abnormalReasons.add("电压过高: " + data.getVoltage());
            } else if (data.getVoltage() < abnormal.getVoltageMin()) {
                abnormalReasons.add("电压过低: " + data.getVoltage());
            }
        }

        if (data.getCurrent() != null) {
            if (data.getCurrent() > abnormal.getCurrentMax()) {
                abnormalReasons.add("电流过大: " + data.getCurrent());
            } else if (data.getCurrent() < abnormal.getCurrentMin()) {
                abnormalReasons.add("电流过小: " + data.getCurrent());
            }
        }

        if (data.getTemperature() != null && data.getTemperature() > abnormal.getTemperatureMax()) {
            abnormalReasons.add("温度过高: " + data.getTemperature());
        }

        if (data.getPower() != null && data.getPower() > abnormal.getPowerMax()) {
            abnormalReasons.add("功率超限: " + data.getPower());
        }

        if (data.getFaultCode() != null && data.getFaultCode() != 0) {
            abnormalReasons.add("故障码: " + data.getFaultCode());
        }

        if (!abnormalReasons.isEmpty()) {
            log.warn("设备数据异常: deviceId={}, reasons={}", data.getDeviceId(), abnormalReasons);
            triggerAlarm(data, abnormalReasons);
        }
    }

    private void triggerAlarm(InverterDataDTO data, List<String> reasons) {
        log.info("触发告警: deviceId={}, stationId={}, reasons={}",
                data.getDeviceId(), data.getStationId(), reasons);

        boolean hasFaultCode = data.getFaultCode() != null && data.getFaultCode() != 0;

        Map<String, Object> alarmMessage = new HashMap<>();
        alarmMessage.put("deviceId", data.getDeviceId());
        alarmMessage.put("stationId", parseLong(data.getStationId()));
        alarmMessage.put("inverterId", parseLong(data.getDeviceId()));
        alarmMessage.put("reasons", reasons);
        alarmMessage.put("timestamp", System.currentTimeMillis());

        if (hasFaultCode) {
            alarmMessage.put("faultCode", mapFaultCode(data.getFaultCode()));
            alarmMessage.put("description", String.join("; ", reasons));
            alarmMessage.put("alarmType", "fault_code");
        } else {
            alarmMessage.put("description", String.join("; ", reasons));
            alarmMessage.put("alarmType", "data_abnormal");
        }

        try {
            Message<String> message = MessageBuilder
                    .withPayload(com.alibaba.fastjson.JSON.toJSONString(alarmMessage))
                    .build();
            rocketMQTemplate.syncSend(FAULT_ALARM_TOPIC, message);
            log.info("告警消息已发送至RocketMQ: deviceId={}, topic={}", data.getDeviceId(), FAULT_ALARM_TOPIC);
        } catch (Exception e) {
            log.error("告警消息发送失败: deviceId={}, error={}", data.getDeviceId(), e.getMessage(), e);
        }
    }

    private String mapFaultCode(Integer faultCode) {
        switch (faultCode) {
            case 1: return "INV_OVER_VOLT";
            case 2: return "INV_SHORT_CIRCUIT";
            case 3: return "INV_OVER_TEMP";
            case 4: return "INV_NO_COMM";
            case 5: return "INV_GRID_OFF";
            case 6: return "INV_LOW_EFF";
            case 7: return "STRING_NO_OUTPUT";
            case 8: return "PANEL_HOT_SPOT";
            case 9: return "FAN_FAILURE";
            case 10: return "DC_INSULATION_FAULT";
            default: return "UNKNOWN_FAULT_" + faultCode;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public InverterDataDTO getRealtimeData(String deviceId) {
        return realtimeDataMap.get(deviceId);
    }

    public Map<String, InverterDataDTO> getAllRealtimeData() {
        return realtimeDataMap;
    }

    public boolean isDeviceOnline(String deviceId) {
        return deviceOnlineTimeMap.containsKey(deviceId);
    }

    public int getOnlineDeviceCount() {
        return deviceOnlineTimeMap.size();
    }

    public List<InverterDataDTO> getHistoryData(String deviceId, long startTime, long endTime) {
        return influxDBService.queryHistoryData(deviceId, startTime, endTime);
    }

    public List<InverterDataDTO> getHistoryData(String deviceId, long startTime, long endTime, int limit) {
        return influxDBService.queryHistoryData(deviceId, startTime, endTime, limit);
    }
}
