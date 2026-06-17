package com.solar.ops.device.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AlarmDeduplicationHelper {

    private static final Logger log = LoggerFactory.getLogger(AlarmDeduplicationHelper.class);

    private static final String ALARM_DEDUP_KEY = "alarm:dedup:";
    private static final long DEDUP_WINDOW_MS = 5 * 60 * 1000L;
    private static final int MAX_CACHE_SIZE = 10000;

    private final Map<String, Long> localCache = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupScheduler;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alarm-dedup-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpired, 60, 60, TimeUnit.SECONDS);
    }

    public boolean allowSend(String deviceId, String faultCode) {
        String key = deviceId + ":" + faultCode;
        long now = System.currentTimeMillis();

        Long lastTime = localCache.get(key);
        if (lastTime != null && now - lastTime < DEDUP_WINDOW_MS) {
            log.debug("告警去重[本地缓存]命中, key={}", key);
            return false;
        }

        try {
            Boolean exists = redisTemplate.hasKey(ALARM_DEDUP_KEY + key);
            if (Boolean.TRUE.equals(exists)) {
                log.debug("告警去重[Redis]命中, key={}", key);
                localCache.put(key, now);
                return false;
            }
        } catch (Exception e) {
            log.warn("告警去重查询Redis异常, key={}, error={}", key, e.getMessage());
        }

        localCache.put(key, now);
        if (localCache.size() > MAX_CACHE_SIZE) {
            cleanupExpired();
        }

        try {
            redisTemplate.opsForValue().set(ALARM_DEDUP_KEY + key, String.valueOf(now),
                    DEDUP_WINDOW_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("告警去重写入Redis异常, key={}, error={}", key, e.getMessage());
        }

        return true;
    }

    private void cleanupExpired() {
        try {
            long now = System.currentTimeMillis();
            localCache.entrySet().removeIf(entry -> now - entry.getValue() >= DEDUP_WINDOW_MS);
            log.debug("告警去重本地缓存清理完成, 当前大小={}", localCache.size());
        } catch (Exception e) {
            log.warn("告警去重本地缓存清理异常", e);
        }
    }
}
