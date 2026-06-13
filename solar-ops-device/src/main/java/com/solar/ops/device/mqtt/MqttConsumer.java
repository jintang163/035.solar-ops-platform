package com.solar.ops.device.mqtt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.solar.ops.device.config.MqttProperties;
import com.solar.ops.device.dto.InverterDataDTO;
import com.solar.ops.device.service.DeviceDataService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class MqttConsumer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MqttConsumer.class);

    @Autowired
    private MqttProperties mqttProperties;

    @Autowired
    private DeviceDataService deviceDataService;

    private MqttClient client;

    private volatile boolean connected = false;

    public void connect() {
        try {
            client = new MqttClient(
                    mqttProperties.getBroker(),
                    mqttProperties.getClientId() + "-" + System.currentTimeMillis(),
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);

            if (mqttProperties.getUsername() != null && !mqttProperties.getUsername().isEmpty()) {
                options.setUserName(mqttProperties.getUsername());
                options.setPassword(mqttProperties.getPassword().toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接丢失", cause);
                    connected = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(options);
            connected = true;
            log.info("MQTT连接成功: broker={}", mqttProperties.getBroker());

            subscribeTopics();

        } catch (Exception e) {
            log.error("MQTT连接失败", e);
        }
    }

    private void subscribeTopics() {
        try {
            if (mqttProperties.getTopic() != null) {
                if (mqttProperties.getTopic().getDeviceData() != null) {
                    client.subscribe(mqttProperties.getTopic().getDeviceData(), 1);
                    log.info("订阅主题: {}", mqttProperties.getTopic().getDeviceData());
                }
                if (mqttProperties.getTopic().getDeviceStatus() != null) {
                    client.subscribe(mqttProperties.getTopic().getDeviceStatus(), 1);
                    log.info("订阅主题: {}", mqttProperties.getTopic().getDeviceStatus());
                }
            }
        } catch (Exception e) {
            log.error("MQTT订阅主题失败", e);
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.debug("收到MQTT消息: topic={}, payload={}", topic, payload);

        try {
            if (topic.contains("data")) {
                handleDeviceData(payload);
            } else if (topic.contains("status")) {
                handleDeviceStatus(payload);
            }
        } catch (Exception e) {
            log.error("处理MQTT消息异常: topic={}", topic, e);
        }
    }

    private void handleDeviceData(String payload) {
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            InverterDataDTO data = new InverterDataDTO();
            data.setDeviceId(json.getStr("deviceId"));
            data.setStationId(json.getStr("stationId"));
            data.setVoltage(json.getDouble("voltage"));
            data.setCurrent(json.getDouble("current"));
            data.setPower(json.getDouble("power"));
            data.setEnergy(json.getDouble("energy"));
            data.setTemperature(json.getDouble("temperature"));
            data.setFaultCode(json.getInt("faultCode"));
            data.setTimestamp(json.getLong("timestamp", System.currentTimeMillis()));

            if (data.getDeviceId() != null) {
                deviceDataService.processDeviceData(data);
            }
        } catch (Exception e) {
            log.error("解析设备数据失败: payload={}", payload, e);
        }
    }

    private void handleDeviceStatus(String payload) {
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            String deviceId = json.getStr("deviceId");
            String status = json.getStr("status");

            if (deviceId != null) {
                if ("online".equals(status)) {
                    deviceDataService.handleDeviceOnline(deviceId, "mqtt");
                } else if ("offline".equals(status)) {
                    deviceDataService.handleDeviceOffline(deviceId);
                }
            }
        } catch (Exception e) {
            log.error("解析设备状态失败: payload={}", payload, e);
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                connected = false;
                log.info("MQTT连接已断开");
            }
        } catch (Exception e) {
            log.error("MQTT断开连接失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        disconnect();
    }

    @Override
    public void run(String... args) throws Exception {
        connect();
    }

    public boolean isConnected() {
        return connected;
    }
}
