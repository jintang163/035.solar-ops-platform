package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AlarmScanService {

    private static final Logger logger = LoggerFactory.getLogger(AlarmScanService.class);

    private static final String REDIS_KEY_OFFLINE_BROADCAST = "voice:broadcast:offline";
    private static final String REDIS_KEY_TEMP_BROADCAST = "voice:broadcast:temp";
    private static final String REDIS_KEY_FAULT_BROADCAST = "voice:broadcast:fault";
    private static final long DEDUP_MINUTES = 5;

    private final InverterMapper inverterMapper;
    private final WorkOrderMapper workOrderMapper;
    private final VoiceBroadcastService voiceBroadcastService;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Autowired(required = false)
    private DashboardInfluxDBService dashboardInfluxDBService;

    public AlarmScanService(InverterMapper inverterMapper,
                            WorkOrderMapper workOrderMapper,
                            VoiceBroadcastService voiceBroadcastService) {
        this.inverterMapper = inverterMapper;
        this.workOrderMapper = workOrderMapper;
        this.voiceBroadcastService = voiceBroadcastService;
    }

    @Scheduled(fixedRate = 60000)
    public void scanAlarms() {
        try {
            scanInverterOffline();
            scanTemperatureAlarm();
            scanFaultCodeAlarm();
        } catch (Exception e) {
            logger.error("[告警扫描] 定时扫描异常", e);
        }
    }

    public void scanInverterOffline() {
        LambdaQueryWrapper<Inverter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inverter::getOnlineStatus, 0);
        List<Inverter> offlineInverters = inverterMapper.selectList(wrapper);

        if (offlineInverters.isEmpty()) {
            return;
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        for (Inverter inv : offlineInverters) {
            if (inv.getLastOnlineTime() != null && inv.getLastOnlineTime().isAfter(fiveMinutesAgo)) {
                if (isAlreadyBroadcast(REDIS_KEY_OFFLINE_BROADCAST, String.valueOf(inv.getId()))) {
                    continue;
                }
                voiceBroadcastService.triggerBroadcast(
                        1, 3,
                        inv.getStationId(), inv.getId(),
                        "INV_NO_COMM", "逆变器离线",
                        null
                );
                markBroadcast(REDIS_KEY_OFFLINE_BROADCAST, String.valueOf(inv.getId()));
                logger.info("[告警扫描] 逆变器离线播报: inverterId={}", inv.getId());
            }
        }
    }

    public void scanTemperatureAlarm() {
        List<Inverter> allInverters = inverterMapper.selectList(null);
        for (Inverter inv : allInverters) {
            Double temperature = null;
            if (dashboardInfluxDBService != null && inv.getDeviceSn() != null) {
                try {
                    java.util.Map<String, java.math.BigDecimal> data =
                            dashboardInfluxDBService.queryRealtimeData(inv.getDeviceSn());
                    if (data != null && data.containsKey("temperature")) {
                        temperature = data.get("temperature").doubleValue();
                    }
                } catch (Exception e) {
                    logger.debug("[告警扫描] InfluxDB查询温度失败, deviceSn={}", inv.getDeviceSn());
                }
            }

            if (temperature != null && temperature > 85) {
                if (isAlreadyBroadcast(REDIS_KEY_TEMP_BROADCAST, String.valueOf(inv.getId()))) {
                    continue;
                }
                voiceBroadcastService.triggerBroadcast(
                        2, 4,
                        inv.getStationId(), inv.getId(),
                        "INV_OVER_TEMP", "温度过高，当前温度" + temperature + "℃",
                        null
                );
                markBroadcast(REDIS_KEY_TEMP_BROADCAST, String.valueOf(inv.getId()));
                logger.info("[告警扫描] 温度告警播报: inverterId={}, temperature={}", inv.getId(), temperature);
            }
        }
    }

    public void scanFaultCodeAlarm() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(WorkOrder::getFaultLevel, 3);
        wrapper.ge(WorkOrder::getCreateTime, fiveMinutesAgo);
        wrapper.in(WorkOrder::getStatus, 0, 1, 2);
        List<WorkOrder> faultOrders = workOrderMapper.selectList(wrapper);

        for (WorkOrder order : faultOrders) {
            if (isAlreadyBroadcast(REDIS_KEY_FAULT_BROADCAST, String.valueOf(order.getId()))) {
                continue;
            }
            Integer alarmLevel = order.getFaultLevel() != null ? order.getFaultLevel() : 3;
            Integer broadcastType = resolveBroadcastTypeFromFaultCode(order.getFaultCode());

            voiceBroadcastService.triggerBroadcast(
                    broadcastType, alarmLevel,
                    order.getStationId(), order.getInverterId(),
                    order.getFaultCode(), order.getDescription(),
                    order.getId()
            );
            markBroadcast(REDIS_KEY_FAULT_BROADCAST, String.valueOf(order.getId()));
            logger.info("[告警扫描] 故障工单播报: orderId={}, faultCode={}", order.getId(), order.getFaultCode());
        }
    }

    private Integer resolveBroadcastTypeFromFaultCode(String faultCode) {
        if (faultCode == null) {
            return 3;
        }
        switch (faultCode) {
            case "INV_NO_COMM":
            case "4":
                return 1;
            case "INV_OVER_TEMP":
            case "3":
            case "PANEL_HOT_SPOT":
            case "8":
                return 2;
            case "INV_SHORT_CIRCUIT":
            case "2":
            case "DC_INSULATION_FAULT":
            case "10":
                return 4;
            case "INV_OVER_VOLT":
            case "1":
                return 3;
            default:
                return 3;
        }
    }

    private boolean isAlreadyBroadcast(String redisKey, String deviceId) {
        if (redissonClient == null) {
            return false;
        }
        try {
            RSet<String> set = redissonClient.getSet(redisKey);
            return set.contains(deviceId);
        } catch (Exception e) {
            logger.warn("[告警扫描] Redis查询失败", e);
            return false;
        }
    }

    private void markBroadcast(String redisKey, String deviceId) {
        if (redissonClient == null) {
            return;
        }
        try {
            RSet<String> set = redissonClient.getSet(redisKey);
            set.add(deviceId);
            set.expire(DEDUP_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("[告警扫描] Redis写入失败", e);
        }
    }
}
