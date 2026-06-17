package com.solar.ops.admin.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.admin.config.VoiceTtsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "voice.tts", name = "enabled", havingValue = "true")
public class TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);

    private static final String XUNFEI_TTS_URL = "https://tts-api.xfyun.cn/v2/tts";

    @Autowired(required = false)
    private VoiceTtsProperties voiceTtsProperties;

    public String synthesize(String text, String voiceName, Integer speed, Integer volume) {
        if (voiceTtsProperties == null) {
            logger.warn("[TTS] VoiceTtsProperties 未注入，跳过TTS合成");
            return null;
        }

        if (!isConfigValid()) {
            logger.warn("[TTS] 讯飞TTS配置不完整，跳过合成。appId={}, apiKey={}, apiSecret={}",
                    StringUtils.hasText(voiceTtsProperties.getXunfeiAppId()),
                    StringUtils.hasText(voiceTtsProperties.getXunfeiApiKey()),
                    StringUtils.hasText(voiceTtsProperties.getXunfeiApiSecret()));
            return null;
        }

        try {
            File dir = new File(voiceTtsProperties.getAudioSavePath());
            if (!dir.exists()) {
                dir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = "broadcast_" + timestamp + "_" + uuid + ".mp3";
            String filePath = voiceTtsProperties.getAudioSavePath() + File.separator + fileName;

            String vcn = StringUtils.hasText(voiceName) ? voiceName : voiceTtsProperties.getXunfeiVoiceName();
            int spd = (speed != null && speed > 0) ? speed : voiceTtsProperties.getXunfeiSpeed();
            int vol = (volume != null && volume > 0) ? volume : voiceTtsProperties.getXunfeiVolume();

            byte[] audioData = doSynthesize(text, vcn, spd, vol);
            if (audioData == null || audioData.length == 0) {
                logger.warn("[TTS] 合成音频数据为空");
                return null;
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(audioData);
            }

            String audioUrl = voiceTtsProperties.getAudioBaseUrl() + "/" + fileName;
            logger.info("[TTS] 语音合成成功，audioUrl={}", audioUrl);
            return audioUrl;
        } catch (Exception e) {
            logger.error("[TTS] 语音合成失败", e);
            return null;
        }
    }

    private boolean isConfigValid() {
        return voiceTtsProperties != null
                && StringUtils.hasText(voiceTtsProperties.getXunfeiAppId())
                && StringUtils.hasText(voiceTtsProperties.getXunfeiApiKey())
                && StringUtils.hasText(voiceTtsProperties.getXunfeiApiSecret());
    }

    private byte[] doSynthesize(String text, String voiceName, int speed, int volume) throws Exception {
        JSONObject business = new JSONObject();
        business.put("aue", "lame");
        business.put("sfl", 1);
        business.put("vcn", voiceName);
        business.put("speed", speed);
        business.put("volume", volume);
        business.put("pitch", 50);
        business.put("bgs", 0);

        JSONObject common = new JSONObject();
        common.put("app_id", voiceTtsProperties.getXunfeiAppId());

        JSONObject data = new JSONObject();
        data.put("status", 2);
        data.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));

        JSONObject requestJson = new JSONObject();
        requestJson.put("common", common);
        requestJson.put("business", business);
        requestJson.put("data", data);

        String authUrl = buildAuthUrl();
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(authUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] bodyBytes = requestJson.toJSONString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(bodyBytes);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.error("[TTS] 讯飞TTS HTTP请求失败，responseCode={}", responseCode);
                return null;
            }

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String responseBody = baos.toString(StandardCharsets.UTF_8);
                JSONObject resp = JSON.parseObject(responseBody);
                int code = resp.getIntValue("code");
                if (code != 0) {
                    logger.error("[TTS] 讯飞TTS返回错误: code={}, message={}", code, resp.getString("message"));
                    return null;
                }
                JSONObject dataObj = resp.getJSONObject("data");
                if (dataObj != null) {
                    String audio = dataObj.getString("audio");
                    if (StringUtils.hasText(audio)) {
                        return Base64.getDecoder().decode(audio);
                    }
                }
                return null;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String buildAuthUrl() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        String host = "tts-api.xfyun.cn";
        String path = "/v2/tts";

        String signatureOrigin = "host: " + host + "\ndate: " + date + "\nPOST " + path + " HTTP/1.1";
        String signature = hmacSha256(signatureOrigin, voiceTtsProperties.getXunfeiApiSecret());
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                voiceTtsProperties.getXunfeiApiKey(), "hmac-sha256", "host date request-line", signature);
        String authorizationBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        return XUNFEI_TTS_URL + "?authorization=" + URLEncoder.encode(authorizationBase64, "UTF-8")
                + "&date=" + URLEncoder.encode(date, "UTF-8")
                + "&host=" + URLEncoder.encode(host, "UTF-8");
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
