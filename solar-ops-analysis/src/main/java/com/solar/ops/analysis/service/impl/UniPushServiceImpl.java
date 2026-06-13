package com.solar.ops.analysis.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.analysis.config.AppPushProperties;
import com.solar.ops.analysis.service.AppPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UniPushServiceImpl implements AppPushService {

    private static final Logger log = LoggerFactory.getLogger(UniPushServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private AppPushProperties pushProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private long tokenExpireTime;

    @Override
    public boolean pushToUser(Long userId, String title, String content, Map<String, String> extras) {
        if (!pushProperties.isEnabled()) {
            log.debug("APP推送未启用，跳过推送");
            return false;
        }
        if (!checkConfig()) {
            return false;
        }
        try {
            Map<String, Object> payload = buildPushPayload(Collections.singletonList(String.valueOf(userId)),
                    "alias", title, content, extras);
            return doPush(payload, "alias");
        } catch (Exception e) {
            log.error("推送消息给用户[{}]失败", userId, e);
            return false;
        }
    }

    @Override
    public boolean pushToUsers(List<SysUser> users, String title, String content, Map<String, String> extras) {
        if (!pushProperties.isEnabled()) {
            log.debug("APP推送未启用，跳过推送");
            return false;
        }
        if (!checkConfig()) {
            return false;
        }
        if (CollectionUtils.isEmpty(users)) {
            log.warn("推送用户列表为空");
            return false;
        }
        try {
            List<String> aliases = users.stream()
                    .map(u -> u.getId() != null ? String.valueOf(u.getId()) : u.getPhone())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (aliases.isEmpty()) {
                log.warn("解析用户别名失败，尝试按手机号推送");
                List<String> phones = users.stream()
                        .map(SysUser::getPhone)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
                if (!phones.isEmpty()) {
                    return pushToPhones(phones, title, content, extras);
                }
                return false;
            }
            Map<String, Object> payload = buildPushPayload(aliases, "alias", title, content, extras);
            return doPush(payload, "alias");
        } catch (Exception e) {
            log.error("批量推送消息失败", e);
            return false;
        }
    }

    @Override
    public boolean pushToPhone(String phone, String title, String content, Map<String, String> extras) {
        return pushToPhones(Collections.singletonList(phone), title, content, extras);
    }

    @Override
    public boolean pushToPhones(List<String> phones, String title, String content, Map<String, String> extras) {
        if (!pushProperties.isEnabled()) {
            log.debug("APP推送未启用，跳过推送");
            return false;
        }
        if (!checkConfig()) {
            return false;
        }
        if (CollectionUtils.isEmpty(phones)) {
            log.warn("推送手机号列表为空");
            return false;
        }
        try {
            Map<String, Object> payload = buildPushPayload(phones, "phone_type", title, content, extras);
            return doPush(payload, "phone_type");
        } catch (Exception e) {
            log.error("按手机号批量推送消息失败", e);
            return false;
        }
    }

    private boolean checkConfig() {
        if (!StringUtils.hasText(pushProperties.getAppId())
                || !StringUtils.hasText(pushProperties.getAppKey())
                || !StringUtils.hasText(pushProperties.getMasterSecret())) {
            log.warn("APP推送配置不完整，缺少appId/appKey/masterSecret，跳过推送。当前配置仅作占位，请在application.yml中补充个推/uni-push凭证");
            return false;
        }
        return true;
    }

    private Map<String, Object> buildPushPayload(List<String> targets, String targetType,
                                                  String title, String content, Map<String, String> extras) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", UUID.randomUUID().toString().replace("-", ""));
        payload.put("settings", buildSettings());
        payload.put("audience", buildAudience(targets, targetType));
        payload.put("push_message", buildPushMessage(title, content, extras));
        payload.put("push_channel", buildPushChannel());
        return payload;
    }

    private Map<String, Object> buildSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("ttl", 7200000);
        Map<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("default", 1);
        strategy.put("ios", 4);
        strategy.put("st", 4);
        settings.put("strategy", strategy);
        return settings;
    }

    private Map<String, Object> buildAudience(List<String> targets, String targetType) {
        Map<String, Object> audience = new LinkedHashMap<>();
        audience.put(targetType, targets);
        return audience;
    }

    private Map<String, Object> buildPushMessage(String title, String content, Map<String, String> extras) {
        Map<String, Object> pushMessage = new LinkedHashMap<>();
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", content);
        if (!CollectionUtils.isEmpty(extras)) {
            try {
                notification.put("payload", OBJECT_MAPPER.writeValueAsString(extras));
            } catch (Exception e) {
                notification.put("payload", extras.toString());
            }
        }
        Map<String, Object> clickType = new LinkedHashMap<>();
        clickType.put("type", "intent");
        clickType.put("intent", "intent:#Intent;launchFlags=0x04000000;component=com.solar.ops/.MainActivity;end");
        notification.put("click_type", clickType);
        pushMessage.put("notification", notification);
        return pushMessage;
    }

    private Map<String, Object> buildPushChannel() {
        Map<String, Object> pushChannel = new LinkedHashMap<>();

        Map<String, Object> ups = new LinkedHashMap<>();
        Map<String, Object> upsNotification = new LinkedHashMap<>();
        Map<String, Object> upsOptions = new LinkedHashMap<>();
        Map<String, Object> HW = new LinkedHashMap<>();
        HW.put("/message/android/category", "IM");
        Map<String, Object> VIVO = new LinkedHashMap<>();
        VIVO.put("classification", 1);
        upsOptions.put("HW", HW);
        upsOptions.put("VIVO", VIVO);
        upsNotification.put("options", upsOptions);
        ups.put("notification", upsNotification);
        pushChannel.put("ups", ups);

        return pushChannel;
    }

    private boolean doPush(Map<String, Object> payload, String audienceType) {
        try {
            String token = getToken();
            if (!StringUtils.hasText(token)) {
                log.error("获取推送token失败");
                return false;
            }
            String url;
            if ("phone_type".equals(audienceType)) {
                url = pushProperties.getApiUrl() + pushProperties.getAppId() + "/push/single/batch/cid";
            } else {
                url = pushProperties.getApiUrl() + pushProperties.getAppId() + "/push/single/batch/alias";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", token);

            HttpEntity<String> entity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && StringUtils.hasText(response.getBody())) {
                JsonNode result = OBJECT_MAPPER.readTree(response.getBody());
                int code = result.path("code").asInt(-1);
                if (code == 0) {
                    log.info("APP推送成功: {}", response.getBody());
                    return true;
                } else {
                    log.error("APP推送失败: {}", response.getBody());
                    if (code == 10003 || code == 10001) {
                        cachedToken = null;
                        tokenExpireTime = 0;
                    }
                    return false;
                }
            }
            log.error("APP推送HTTP错误: status={}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("执行APP推送请求异常", e);
            return false;
        }
    }

    private synchronized String getToken() {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedToken) && now < tokenExpireTime) {
            return cachedToken;
        }
        if (!checkConfig()) {
            return null;
        }
        try {
            String url = pushProperties.getApiUrl() + pushProperties.getAppId() + "/auth";
            String sign = generateSign();
            long timestamp = System.currentTimeMillis();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sign", sign);
            body.put("timestamp", String.valueOf(timestamp));
            body.put("appkey", pushProperties.getAppKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

            HttpEntity<String> entity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && StringUtils.hasText(response.getBody())) {
                JsonNode result = OBJECT_MAPPER.readTree(response.getBody());
                int code = result.path("code").asInt(-1);
                if (code == 0) {
                    JsonNode data = result.path("data");
                    cachedToken = data.path("token").asText(null);
                    long expireTime = data.path("expire_time").asLong(0L);
                    tokenExpireTime = expireTime - 60000;
                    log.info("获取推送token成功");
                    return cachedToken;
                } else {
                    log.error("获取推送token失败: {}", response.getBody());
                }
            }
        } catch (Exception e) {
            log.error("获取推送token异常", e);
        }
        return null;
    }

    private String generateSign() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String original = pushProperties.getAppKey() + timestamp + pushProperties.getMasterSecret();
        return sha256Hex(original);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
