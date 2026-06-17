package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.config.VoiceBroadcastWebSocketHandler;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.entity.VoiceBroadcastRecord;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.VoiceBroadcastRecordMapper;
import com.solar.ops.admin.vo.VoiceBroadcastConfigVO;
import com.solar.ops.admin.vo.VoiceBroadcastRecordVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceBroadcastService.class);

    private static final Map<String, VoiceBroadcastConfigVO> CONFIG_CACHE = new ConcurrentHashMap<>();
    private static final String DEFAULT_CONFIG_KEY = "default";

    private static final Map<String, LocalDateTime> DEDUPLICATION_CACHE = new ConcurrentHashMap<>();
    private static final long DEDUPLICATION_MINUTES = 5;

    private static final Map<Integer, String> BROADCAST_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, String> ALARM_LEVEL_MAP = new HashMap<>();
    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();

    static {
        BROADCAST_TYPE_MAP.put(1, "逆变器离线");
        BROADCAST_TYPE_MAP.put(2, "火灾预警");
        BROADCAST_TYPE_MAP.put(3, "高级告警");
        BROADCAST_TYPE_MAP.put(4, "紧急告警");
        BROADCAST_TYPE_MAP.put(5, "设备异常");

        ALARM_LEVEL_MAP.put(1, "低级");
        ALARM_LEVEL_MAP.put(2, "中级");
        ALARM_LEVEL_MAP.put(3, "高级");
        ALARM_LEVEL_MAP.put(4, "紧急");

        STATUS_MAP.put(0, "待播报");
        STATUS_MAP.put(1, "已播报");
        STATUS_MAP.put(2, "播报失败");
    }

    @Resource
    private VoiceBroadcastRecordMapper voiceBroadcastRecordMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private InverterMapper inverterMapper;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private VoiceBroadcastWebSocketHandler voiceBroadcastWebSocketHandler;

    private VoiceBroadcastConfigVO getDefaultConfig() {
        VoiceBroadcastConfigVO config = new VoiceBroadcastConfigVO();
        config.setEnabled(true);
        config.setMinAlarmLevel(3);
        config.setEnabledTypes(Arrays.asList(1, 2, 3, 4, 5));
        config.setVolume(80);
        config.setSpeed(50);
        config.setVoiceName("xiaoyan");
        config.setBroadcastStartTime("08:00");
        config.setBroadcastEndTime("20:00");
        config.setNightBroadcast(false);
        return config;
    }

    public VoiceBroadcastRecordVO triggerBroadcast(Integer broadcastType, Integer alarmLevel,
                                                    Long stationId, Long inverterId,
                                                    String faultCode, String description,
                                                    Long workOrderId) {
        VoiceBroadcastConfigVO config = getBroadcastConfig();

        if (!config.getEnabled()) {
            logger.debug("语音播报未启用，跳过播报");
            return null;
        }

        if (alarmLevel == null || alarmLevel < config.getMinAlarmLevel()) {
            logger.debug("告警级别{}低于最小播报级别{}，跳过播报", alarmLevel, config.getMinAlarmLevel());
            return null;
        }

        if (config.getEnabledTypes() != null && !config.getEnabledTypes().contains(broadcastType)) {
            logger.debug("播报类型{}不在启用列表中，跳过播报", broadcastType);
            return null;
        }

        String dedupKey = buildDeduplicationKey(inverterId, faultCode);
        if (isDuplicate(dedupKey)) {
            logger.debug("5分钟内重复播报，跳过。inverterId={}, faultCode={}", inverterId, faultCode);
            return null;
        }

        Station station = stationId != null ? stationMapper.selectById(stationId) : null;
        Inverter inverter = inverterId != null ? inverterMapper.selectById(inverterId) : null;

        String stationName = station != null ? station.getStationName() : "";
        String inverterName = inverter != null ? inverter.getDeviceName() : "";

        String broadcastContent = generateBroadcastContent(broadcastType, alarmLevel, stationName, inverterName, faultCode, description);

        VoiceBroadcastRecord record = new VoiceBroadcastRecord();
        record.setBroadcastType(broadcastType);
        record.setAlarmLevel(alarmLevel);
        record.setStationId(stationId);
        record.setStationName(stationName);
        record.setInverterId(inverterId);
        record.setInverterName(inverterName);
        record.setFaultCode(faultCode);
        record.setBroadcastContent(broadcastContent);
        record.setStatus(0);
        record.setWorkOrderId(workOrderId);
        voiceBroadcastRecordMapper.insert(record);

        VoiceBroadcastRecordVO vo = convertToVO(record);

        try {
            voiceBroadcastWebSocketHandler.pushBroadcastMessage(vo);
            record.setStatus(1);
            record.setBroadcastTime(LocalDateTime.now());
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(1);
            vo.setBroadcastTime(record.getBroadcastTime());
            DEDUPLICATION_CACHE.put(dedupKey, LocalDateTime.now());
            logger.info("语音播报推送成功，recordId={}", record.getId());
        } catch (Exception e) {
            logger.error("语音播报推送失败，recordId={}", record.getId(), e);
            record.setStatus(2);
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(2);
        }

        return vo;
    }

    public VoiceBroadcastRecordVO triggerTestBroadcast(String content) {
        VoiceBroadcastRecord record = new VoiceBroadcastRecord();
        record.setBroadcastType(3);
        record.setAlarmLevel(3);
        record.setBroadcastContent(content != null ? content : "这是一条测试语音播报消息");
        record.setStatus(0);
        voiceBroadcastRecordMapper.insert(record);

        VoiceBroadcastRecordVO vo = convertToVO(record);

        try {
            voiceBroadcastWebSocketHandler.pushBroadcastMessage(vo);
            record.setStatus(1);
            record.setBroadcastTime(LocalDateTime.now());
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(1);
            vo.setBroadcastTime(record.getBroadcastTime());
        } catch (Exception e) {
            logger.error("测试语音播报推送失败，recordId={}", record.getId(), e);
            record.setStatus(2);
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(2);
        }

        return vo;
    }

    public void retryBroadcast(Long id) {
        VoiceBroadcastRecord record = voiceBroadcastRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("播报记录不存在");
        }

        VoiceBroadcastRecordVO vo = convertToVO(record);
        try {
            voiceBroadcastWebSocketHandler.pushBroadcastMessage(vo);
            record.setStatus(1);
            record.setBroadcastTime(LocalDateTime.now());
            voiceBroadcastRecordMapper.updateById(record);
            logger.info("语音播报重新推送成功，recordId={}", id);
        } catch (Exception e) {
            logger.error("语音播报重新推送失败，recordId={}", id, e);
            record.setStatus(2);
            voiceBroadcastRecordMapper.updateById(record);
        }
    }

    public PageResult<VoiceBroadcastRecordVO> getBroadcastHistory(Integer pageNum, Integer pageSize,
                                                                   Integer broadcastType, Integer alarmLevel,
                                                                   String keyword) {
        Page<VoiceBroadcastRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VoiceBroadcastRecord> wrapper = new LambdaQueryWrapper<>();

        if (broadcastType != null) {
            wrapper.eq(VoiceBroadcastRecord::getBroadcastType, broadcastType);
        }
        if (alarmLevel != null) {
            wrapper.eq(VoiceBroadcastRecord::getAlarmLevel, alarmLevel);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(VoiceBroadcastRecord::getStationName, keyword)
                    .or().like(VoiceBroadcastRecord::getInverterName, keyword)
                    .or().like(VoiceBroadcastRecord::getBroadcastContent, keyword)
                    .or().like(VoiceBroadcastRecord::getFaultCode, keyword);
        }
        wrapper.orderByDesc(VoiceBroadcastRecord::getCreateTime);

        voiceBroadcastRecordMapper.selectPage(page, wrapper);

        List<VoiceBroadcastRecordVO> voList = new java.util.ArrayList<>();
        for (VoiceBroadcastRecord record : page.getRecords()) {
            voList.add(convertToVO(record));
        }

        return PageResult.build(page.getTotal(), voList, (long) pageNum, (long) pageSize);
    }

    public VoiceBroadcastConfigVO getBroadcastConfig() {
        VoiceBroadcastConfigVO config = CONFIG_CACHE.get(DEFAULT_CONFIG_KEY);
        if (config == null) {
            config = getDefaultConfig();
            CONFIG_CACHE.put(DEFAULT_CONFIG_KEY, config);
        }
        return config;
    }

    public void updateBroadcastConfig(VoiceBroadcastConfigVO config) {
        if (config == null) {
            config = getDefaultConfig();
        }
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        if (config.getMinAlarmLevel() == null) {
            config.setMinAlarmLevel(3);
        }
        if (config.getVolume() == null) {
            config.setVolume(80);
        }
        if (config.getSpeed() == null) {
            config.setSpeed(50);
        }
        if (config.getVoiceName() == null) {
            config.setVoiceName("xiaoyan");
        }
        CONFIG_CACHE.put(DEFAULT_CONFIG_KEY, config);
        logger.info("语音播报配置已更新：{}", config);
    }

    public String generateBroadcastContent(Integer broadcastType, Integer alarmLevel,
                                            String stationName, String inverterName,
                                            String faultCode, String description) {
        StringBuilder sb = new StringBuilder();

        String levelDesc = ALARM_LEVEL_MAP.getOrDefault(alarmLevel, "告警");
        sb.append("【").append(levelDesc).append("】");

        if (StringUtils.hasText(stationName)) {
            sb.append(stationName);
        }
        if (StringUtils.hasText(inverterName)) {
            sb.append(inverterName);
        }

        if (StringUtils.hasText(description)) {
            sb.append(description);
        } else if (broadcastType != null) {
            String typeDesc = BROADCAST_TYPE_MAP.get(broadcastType);
            if (typeDesc != null) {
                sb.append(typeDesc);
            }
        }

        if (StringUtils.hasText(faultCode)) {
            sb.append("，故障码：").append(faultCode);
        }

        sb.append("，请及时处理");

        return sb.toString();
    }

    public VoiceBroadcastRecordVO convertToVO(VoiceBroadcastRecord record) {
        if (record == null) {
            return null;
        }
        VoiceBroadcastRecordVO vo = new VoiceBroadcastRecordVO();
        vo.setId(record.getId());
        vo.setBroadcastType(record.getBroadcastType());
        vo.setBroadcastTypeDesc(BROADCAST_TYPE_MAP.get(record.getBroadcastType()));
        vo.setAlarmLevel(record.getAlarmLevel());
        vo.setAlarmLevelDesc(ALARM_LEVEL_MAP.get(record.getAlarmLevel()));
        vo.setStationId(record.getStationId());
        vo.setStationName(record.getStationName());
        vo.setInverterId(record.getInverterId());
        vo.setInverterName(record.getInverterName());
        vo.setFaultCode(record.getFaultCode());
        vo.setBroadcastContent(record.getBroadcastContent());
        vo.setAudioUrl(record.getAudioUrl());
        vo.setStatus(record.getStatus());
        vo.setBroadcastTime(record.getBroadcastTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private String buildDeduplicationKey(Long inverterId, String faultCode) {
        return (inverterId != null ? inverterId : "null") + "_" + (faultCode != null ? faultCode : "null");
    }

    private boolean isDuplicate(String key) {
        LocalDateTime lastTime = DEDUPLICATION_CACHE.get(key);
        if (lastTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (lastTime.plusMinutes(DEDUPLICATION_MINUTES).isAfter(now)) {
            return true;
        }
        DEDUPLICATION_CACHE.remove(key);
        return false;
    }
}
