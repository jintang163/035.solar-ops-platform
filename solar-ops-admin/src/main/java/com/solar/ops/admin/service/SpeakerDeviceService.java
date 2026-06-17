package com.solar.ops.admin.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "voice.speaker", name = "enabled", havingValue = "true")
public class SpeakerDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakerDeviceService.class);
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private static final int MAX_RETRY = 1;

    @Value("${voice.speaker.api-url:}")
    private String speakerApiUrl;

    @Value("${voice.speaker.api-key:}")
    private String speakerApiKey;

    public void pushToAllSpeakers(String audioUrl, String text) {
        if (speakerApiUrl == null || speakerApiUrl.isEmpty()) {
            logger.warn("[Speaker] 音箱API地址未配置，跳过推送");
            return;
        }
        doPush(speakerApiUrl, audioUrl, text);
    }

    public void pushToSpeaker(String speakerId, String audioUrl, String text) {
        if (speakerApiUrl == null || speakerApiUrl.isEmpty()) {
            logger.warn("[Speaker] 音箱API地址未配置，跳过推送");
            return;
        }
        String url = speakerApiUrl;
        if (speakerId != null && !speakerId.isEmpty()) {
            url = speakerApiUrl + "/" + speakerId;
        }
        doPush(url, audioUrl, text);
    }

    private void doPush(String url, String audioUrl, String text) {
        JSONObject body = new JSONObject();
        body.put("audioUrl", audioUrl);
        body.put("text", text);
        body.put("volume", 80);
        body.put("speed", 50);

        int retryCount = 0;
        while (retryCount <= MAX_RETRY) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                if (speakerApiKey != null && !speakerApiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + speakerApiKey);
                }

                byte[] bodyBytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    logger.info("[Speaker] 推送音箱成功，url={}", url);
                    return;
                } else {
                    logger.warn("[Speaker] 推送音箱返回非200状态码: {}, url={}", responseCode, url);
                }
            } catch (Exception e) {
                logger.warn("[Speaker] 推送音箱失败(重试次数: {}), url={}", retryCount, url, e);
            }
            retryCount++;
        }
        logger.error("[Speaker] 推送音箱最终失败，url={}", url);
    }
}
