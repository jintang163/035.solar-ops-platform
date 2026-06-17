package com.solar.ops.admin.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.admin.config.VoiceSpeakerProperties;
import com.solar.ops.admin.vo.VoiceSpeakerDeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "voice.speaker", name = "enabled", havingValue = "true")
public class VoiceSpeakerService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceSpeakerService.class);
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int MAX_RETRY = 1;

    @Autowired(required = false)
    private VoiceSpeakerProperties voiceSpeakerProperties;

    public boolean pushToSpeaker(String audioUrl, String text, String deviceId) {
        if (!isEnabled()) {
            logger.warn("[Speaker] 音箱服务未启用，跳过推送");
            return false;
        }

        if (!StringUtils.hasText(deviceId)) {
            logger.warn("[Speaker] 设备ID为空，跳过推送");
            return false;
        }

        try {
            if ("mqtt".equalsIgnoreCase(voiceSpeakerProperties.getPushMethod())) {
                return pushViaMqtt(audioUrl, text, deviceId);
            } else {
                return pushViaHttp(audioUrl, text, deviceId);
            }
        } catch (Exception e) {
            logger.error("[Speaker] 推送音箱失败，deviceId={}", deviceId, e);
            return false;
        }
    }

    public boolean broadcastToAll(String audioUrl, String text) {
        if (!isEnabled()) {
            logger.warn("[Speaker] 音箱服务未启用，跳过广播");
            return false;
        }

        List<String> deviceIds = voiceSpeakerProperties.getDeviceIds();
        if (deviceIds == null || deviceIds.isEmpty()) {
            logger.warn("[Speaker] 未配置音箱设备列表，跳过广播");
            return false;
        }

        boolean allSuccess = true;
        for (String deviceId : deviceIds) {
            boolean success = pushToSpeaker(audioUrl, text, deviceId);
            if (!success) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    public List<VoiceSpeakerDeviceVO> getDeviceList() {
        List<VoiceSpeakerDeviceVO> deviceList = new ArrayList<>();
        if (!isEnabled() || voiceSpeakerProperties.getDeviceIds() == null) {
            return deviceList;
        }

        for (String deviceId : voiceSpeakerProperties.getDeviceIds()) {
            VoiceSpeakerDeviceVO device = getDevice(deviceId);
            if (device != null) {
                deviceList.add(device);
            }
        }
        return deviceList;
    }

    public VoiceSpeakerDeviceVO getDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }

        VoiceSpeakerDeviceVO device = new VoiceSpeakerDeviceVO();
        device.setDeviceId(deviceId);
        device.setDeviceName("语音播报音箱-" + deviceId);
        device.setLocation("运维中心");
        device.setDeviceType("smart-speaker");
        device.setVolume(80);
        device.setLastHeartbeatTime(LocalDateTime.now());

        boolean online = testSpeaker(deviceId);
        device.setOnline(online);

        return device;
    }

    public boolean testSpeaker(String deviceId) {
        if (!isEnabled()) {
            return false;
        }

        try {
            if ("mqtt".equalsIgnoreCase(voiceSpeakerProperties.getPushMethod())) {
                return testMqttConnection(deviceId);
            } else {
                return testHttpConnection(deviceId);
            }
        } catch (Exception e) {
            logger.warn("[Speaker] 测试音箱连接失败，deviceId={}", deviceId, e);
            return false;
        }
    }

    private boolean isEnabled() {
        return voiceSpeakerProperties != null && Boolean.TRUE.equals(voiceSpeakerProperties.getEnabled());
    }

    private boolean pushViaHttp(String audioUrl, String text, String deviceId) {
        if (!StringUtils.hasText(voiceSpeakerProperties.getApiUrl())) {
            logger.warn("[Speaker] HTTP API地址未配置");
            return false;
        }

        String url = voiceSpeakerProperties.getApiUrl();
        JSONObject body = new JSONObject();
        body.put("deviceId", deviceId);
        body.put("text", text);
        body.put("audioUrl", audioUrl);
        body.put("apiKey", voiceSpeakerProperties.getApiKey());

        return sendHttpRequest(url, body);
    }

    private boolean pushViaMqtt(String audioUrl, String text, String deviceId) {
        if (!StringUtils.hasText(voiceSpeakerProperties.getMqttBroker())) {
            logger.warn("[Speaker] MQTT Broker地址未配置");
            return false;
        }

        JSONObject message = new JSONObject();
        message.put("deviceId", deviceId);
        message.put("text", text);
        message.put("audioUrl", audioUrl);
        message.put("timestamp", System.currentTimeMillis());

        String topic = voiceSpeakerProperties.getMqttTopic();
        if (StringUtils.hasText(deviceId)) {
            topic = topic + "/" + deviceId;
        }

        logger.info("[Speaker] MQTT推送，topic={}, message={}", topic, message.toJSONString());
        return true;
    }

    private boolean testHttpConnection(String deviceId) {
        if (!StringUtils.hasText(voiceSpeakerProperties.getApiUrl())) {
            return false;
        }

        try {
            String testUrl = voiceSpeakerProperties.getApiUrl();
            if (testUrl.endsWith("/")) {
                testUrl = testUrl.substring(0, testUrl.length() - 1);
            }
            testUrl = testUrl + "/ping?deviceId=" + deviceId;

            URL url = URI.create(testUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(CONNECT_TIMEOUT);

            if (StringUtils.hasText(voiceSpeakerProperties.getApiKey())) {
                conn.setRequestProperty("Authorization", "Bearer " + voiceSpeakerProperties.getApiKey());
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            logger.debug("[Speaker] HTTP连接测试失败，deviceId={}, error={}", deviceId, e.getMessage());
            return false;
        }
    }

    private boolean testMqttConnection(String deviceId) {
        return StringUtils.hasText(voiceSpeakerProperties.getMqttBroker());
    }

    private boolean sendHttpRequest(String urlStr, JSONObject body) {
        int retryCount = 0;
        int readTimeout = (voiceSpeakerProperties.getTimeout() != null ? voiceSpeakerProperties.getTimeout() : 10) * 1000;

        while (retryCount <= MAX_RETRY) {
            HttpURLConnection conn = null;
            try {
                URL url = URI.create(urlStr).toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(readTimeout);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                if (StringUtils.hasText(voiceSpeakerProperties.getApiKey())) {
                    conn.setRequestProperty("Authorization", "Bearer " + voiceSpeakerProperties.getApiKey());
                }

                byte[] bodyBytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = conn.getInputStream();
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, len);
                        }
                        String response = baos.toString(StandardCharsets.UTF_8);
                        logger.debug("[Speaker] HTTP响应：{}", response);
                    }
                    logger.info("[Speaker] HTTP推送成功，url={}", urlStr);
                    return true;
                } else {
                    logger.warn("[Speaker] HTTP推送返回非200状态码: {}, url={}", responseCode, urlStr);
                }
            } catch (Exception e) {
                logger.warn("[Speaker] HTTP推送失败(重试次数: {}), url={}", retryCount, urlStr, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            retryCount++;
        }

        logger.error("[Speaker] HTTP推送最终失败，url={}", urlStr);
        return false;
    }

    public Map<String, Object> pushWithResult(String audioUrl, String text, List<String> deviceIds) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        Map<String, Boolean> details = new HashMap<>();

        if (deviceIds == null || deviceIds.isEmpty()) {
            deviceIds = voiceSpeakerProperties.getDeviceIds();
        }

        if (deviceIds == null || deviceIds.isEmpty()) {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("details", JSON.toJSONString(details));
            return result;
        }

        for (String deviceId : deviceIds) {
            boolean success = pushToSpeaker(audioUrl, text, deviceId);
            details.put(deviceId, success);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", JSON.toJSONString(details));
        return result;
    }
}
