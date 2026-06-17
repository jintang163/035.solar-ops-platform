package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.config.VoiceBroadcastWebSocketHandler;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.entity.VoiceBroadcastConfig;
import com.solar.ops.admin.entity.VoiceBroadcastRecord;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.mapper.VoiceBroadcastConfigMapper;
import com.solar.ops.admin.mapper.VoiceBroadcastRecordMapper;
import com.solar.ops.admin.vo.VoiceBroadcastConfigVO;
import com.solar.ops.admin.vo.VoiceBroadcastRecordVO;
import com.solar.ops.admin.vo.VoiceSpeakerDeviceVO;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VoiceBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceBroadcastService.class);

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
    private VoiceBroadcastConfigMapper voiceBroadcastConfigMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private InverterMapper inverterMapper;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private VoiceBroadcastWebSocketHandler voiceBroadcastWebSocketHandler;

    @Autowired(required = false)
    private TtsService ttsService;

    @Autowired(required = false)
    private VoiceSpeakerService voiceSpeakerService;

    public VoiceBroadcastRecordVO triggerBroadcast(Integer broadcastType, Integer alarmLevel,
                                                    Long stationId, Long inverterId,
                                                    String faultCode, String description,
                                                    Long workOrderId) {
        return triggerBroadcastToDevices(broadcastType, alarmLevel, stationId, inverterId,
                faultCode, description, workOrderId, null);
    }

    public VoiceBroadcastRecordVO triggerBroadcastToDevices(Integer broadcastType, Integer alarmLevel,
                                                             Long stationId, Long inverterId,
                                                             String faultCode, String description,
                                                             Long workOrderId, List<String> deviceIds) {
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

        String targetSpeakerIds = null;
        if (deviceIds != null && !deviceIds.isEmpty()) {
            targetSpeakerIds = String.join(",", deviceIds);
        }

        VoiceBroadcastRecord record = new VoiceBroadcastRecord();
        record.setBroadcastType(broadcastType);
        record.setAlarmLevel(alarmLevel);
        record.setStationId(stationId);
        record.setStationName(stationName);
        record.setInverterId(inverterId);
        record.setInverterName(inverterName);
        record.setFaultCode(faultCode);
        record.setDescription(description);
        record.setBroadcastContent(broadcastContent);
        record.setTargetSpeakerIds(targetSpeakerIds);
        record.setStatus(0);
        record.setWorkOrderId(workOrderId);
        voiceBroadcastRecordMapper.insert(record);

        String audioUrl = null;
        if (ttsService != null) {
            try {
                audioUrl = ttsService.synthesize(broadcastContent, config.getVoiceName(), config.getSpeed(), config.getVolume());
            } catch (Exception e) {
                logger.warn("TTS语音合成失败，将使用前端兜底播放", e);
            }
        }
        if (audioUrl != null) {
            record.setAudioUrl(audioUrl);
        }

        int successCount = 0;
        int failCount = 0;
        String pushResultJson = null;
        if (voiceSpeakerService != null && audioUrl != null) {
            try {
                Map<String, Object> pushResult = voiceSpeakerService.pushWithResult(audioUrl, broadcastContent, deviceIds);
                successCount = (Integer) pushResult.get("successCount");
                failCount = (Integer) pushResult.get("failCount");
                pushResultJson = (String) pushResult.get("details");
                record.setSuccessSpeakerCount(successCount);
                record.setFailSpeakerCount(failCount);
                record.setPushResult(pushResultJson);
                logger.info("音箱推送完成，成功：{}，失败：{}", successCount, failCount);
            } catch (Exception e) {
                logger.warn("推送音箱终端失败", e);
                record.setSuccessSpeakerCount(0);
                record.setFailSpeakerCount(deviceIds != null ? deviceIds.size() : 0);
            }
        }

        VoiceBroadcastRecordVO vo = convertToVO(record);

        try {
            voiceBroadcastWebSocketHandler.pushBroadcastMessage(vo);
            record.setStatus(1);
            record.setBroadcastTime(LocalDateTime.now());
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(1);
            vo.setBroadcastTime(record.getBroadcastTime());
            vo.setSuccessSpeakerCount(record.getSuccessSpeakerCount());
            vo.setFailSpeakerCount(record.getFailSpeakerCount());
            vo.setPushResult(record.getPushResult());
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
        record.setDescription(content != null ? content : "这是一条测试语音播报消息");
        record.setBroadcastContent(content != null ? content : "这是一条测试语音播报消息");
        record.setStatus(0);
        voiceBroadcastRecordMapper.insert(record);

        String audioUrl = null;
        if (ttsService != null) {
            try {
                audioUrl = ttsService.synthesize(record.getBroadcastContent(), "xiaoyan", 50, 80);
            } catch (Exception e) {
                logger.warn("测试播报TTS语音合成失败", e);
            }
        }
        if (audioUrl != null) {
            record.setAudioUrl(audioUrl);
        }

        int successCount = 0;
        int failCount = 0;
        String pushResultJson = null;
        if (voiceSpeakerService != null && audioUrl != null) {
            try {
                Map<String, Object> pushResult = voiceSpeakerService.pushWithResult(audioUrl, record.getBroadcastContent(), null);
                successCount = (Integer) pushResult.get("successCount");
                failCount = (Integer) pushResult.get("failCount");
                pushResultJson = (String) pushResult.get("details");
                record.setSuccessSpeakerCount(successCount);
                record.setFailSpeakerCount(failCount);
                record.setPushResult(pushResultJson);
            } catch (Exception e) {
                logger.warn("测试播报推送音箱终端失败", e);
            }
        }

        VoiceBroadcastRecordVO vo = convertToVO(record);

        try {
            voiceBroadcastWebSocketHandler.pushBroadcastMessage(vo);
            record.setStatus(1);
            record.setBroadcastTime(LocalDateTime.now());
            voiceBroadcastRecordMapper.updateById(record);
            vo.setStatus(1);
            vo.setBroadcastTime(record.getBroadcastTime());
            vo.setSuccessSpeakerCount(record.getSuccessSpeakerCount());
            vo.setFailSpeakerCount(record.getFailSpeakerCount());
            vo.setPushResult(record.getPushResult());
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

        if (voiceSpeakerService != null && record.getAudioUrl() != null) {
            try {
                List<String> deviceIds = null;
                if (StringUtils.hasText(record.getTargetSpeakerIds())) {
                    deviceIds = Arrays.asList(record.getTargetSpeakerIds().split(","));
                }
                Map<String, Object> pushResult = voiceSpeakerService.pushWithResult(
                        record.getAudioUrl(), record.getBroadcastContent(), deviceIds);
                record.setSuccessSpeakerCount((Integer) pushResult.get("successCount"));
                record.setFailSpeakerCount((Integer) pushResult.get("failCount"));
                record.setPushResult((String) pushResult.get("details"));
            } catch (Exception e) {
                logger.warn("重新播报推送音箱终端失败", e);
            }
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

    public List<VoiceSpeakerDeviceVO> getSpeakerDevices() {
        if (voiceSpeakerService != null) {
            return voiceSpeakerService.getDeviceList();
        }
        return java.util.Collections.emptyList();
    }

    public boolean testSpeakerDevice(String deviceId) {
        if (voiceSpeakerService != null) {
            return voiceSpeakerService.testSpeaker(deviceId);
        }
        return false;
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
        LambdaQueryWrapper<VoiceBroadcastConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceBroadcastConfig::getConfigKey, DEFAULT_CONFIG_KEY);
        wrapper.last("LIMIT 1");
        VoiceBroadcastConfig config = voiceBroadcastConfigMapper.selectOne(wrapper);

        if (config != null) {
            return convertConfigToVO(config);
        }
        return getDefaultConfigVO();
    }

    public void updateBroadcastConfig(VoiceBroadcastConfigVO vo) {
        if (vo == null) {
            vo = getDefaultConfigVO();
        }
        if (vo.getEnabled() == null) {
            vo.setEnabled(true);
        }
        if (vo.getMinAlarmLevel() == null) {
            vo.setMinAlarmLevel(3);
        }
        if (vo.getVolume() == null) {
            vo.setVolume(80);
        }
        if (vo.getSpeed() == null) {
            vo.setSpeed(50);
        }
        if (vo.getVoiceName() == null) {
            vo.setVoiceName("xiaoyan");
        }

        LambdaQueryWrapper<VoiceBroadcastConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceBroadcastConfig::getConfigKey, DEFAULT_CONFIG_KEY);
        wrapper.last("LIMIT 1");
        VoiceBroadcastConfig existing = voiceBroadcastConfigMapper.selectOne(wrapper);

        VoiceBroadcastConfig config = convertVOToConfig(vo);
        config.setConfigKey(DEFAULT_CONFIG_KEY);

        if (existing != null) {
            config.setId(existing.getId());
            voiceBroadcastConfigMapper.updateById(config);
        } else {
            voiceBroadcastConfigMapper.insert(config);
        }
        logger.info("语音播报配置已更新：{}", vo);
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
        vo.setDescription(record.getDescription());
        vo.setBroadcastContent(record.getBroadcastContent());
        vo.setAudioUrl(record.getAudioUrl());
        vo.setTargetSpeakerIds(record.getTargetSpeakerIds());
        vo.setSuccessSpeakerCount(record.getSuccessSpeakerCount());
        vo.setFailSpeakerCount(record.getFailSpeakerCount());
        vo.setPushResult(record.getPushResult());
        vo.setStatus(record.getStatus());
        vo.setBroadcastTime(record.getBroadcastTime());
        vo.setWorkOrderId(record.getWorkOrderId());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private VoiceBroadcastConfigVO getDefaultConfigVO() {
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

    private VoiceBroadcastConfigVO convertConfigToVO(VoiceBroadcastConfig config) {
        VoiceBroadcastConfigVO vo = new VoiceBroadcastConfigVO();
        vo.setEnabled(config.getEnabled());
        vo.setMinAlarmLevel(config.getMinAlarmLevel());
        if (StringUtils.hasText(config.getEnabledTypes())) {
            vo.setEnabledTypes(Arrays.stream(config.getEnabledTypes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        } else {
            vo.setEnabledTypes(Arrays.asList(1, 2, 3, 4, 5));
        }
        vo.setVolume(config.getVolume());
        vo.setSpeed(config.getSpeed());
        vo.setVoiceName(config.getVoiceName());
        vo.setBroadcastStartTime(config.getBroadcastStartTime());
        vo.setBroadcastEndTime(config.getBroadcastEndTime());
        vo.setNightBroadcast(config.getNightBroadcast());
        return vo;
    }

    private VoiceBroadcastConfig convertVOToConfig(VoiceBroadcastConfigVO vo) {
        VoiceBroadcastConfig config = new VoiceBroadcastConfig();
        config.setEnabled(vo.getEnabled());
        config.setMinAlarmLevel(vo.getMinAlarmLevel());
        if (vo.getEnabledTypes() != null) {
            config.setEnabledTypes(vo.getEnabledTypes().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
        config.setVolume(vo.getVolume());
        config.setSpeed(vo.getSpeed());
        config.setVoiceName(vo.getVoiceName());
        config.setBroadcastStartTime(vo.getBroadcastStartTime());
        config.setBroadcastEndTime(vo.getBroadcastEndTime());
        config.setNightBroadcast(vo.getNightBroadcast());
        return config;
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
