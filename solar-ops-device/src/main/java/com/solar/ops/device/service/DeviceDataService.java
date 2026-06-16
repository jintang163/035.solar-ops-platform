package com.solar.ops.device.service;

import com.solar.ops.device.config.DeviceProperties;
import com.solar.ops.device.dto.InverterDataDTO;
import com.solar.ops.device.dto.PlaybackQueryDTO;
import com.solar.ops.device.vo.DataPlaybackVO;
import com.solar.ops.device.vo.DataStatisticsVO;
import com.solar.ops.device.vo.FaultPointVO;
import com.solar.ops.device.vo.RootCauseVO;
import com.solar.ops.device.websocket.DeviceDataWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private final Map<String, InverterDataDTO> realtimeDataMap = new ConcurrentHashMap<>();
    private final Map<String, Long> deviceOnlineTimeMap = new ConcurrentHashMap<>();

    private static final long TWO_HOURS_MS = 2 * 60 * 60 * 1000L;

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

    /**
     * 查询历史数据回放
     *
     * @param dto 查询参数
     * @return 回放数据
     */
    public DataPlaybackVO queryPlaybackData(PlaybackQueryDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDeviceId())) {
            return new DataPlaybackVO();
        }

        long now = System.currentTimeMillis();
        long startTime = dto.getStartTime() != null ? dto.getStartTime() : now - TWO_HOURS_MS;
        long endTime = dto.getEndTime() != null ? dto.getEndTime() : now;

        if (startTime >= endTime) {
            return new DataPlaybackVO();
        }

        String aggregation = StringUtils.hasText(dto.getAggregation()) ? dto.getAggregation() : "mean";
        String interval = StringUtils.hasText(dto.getInterval()) ? dto.getInterval() : calculateInterval(startTime, endTime);

        List<InverterDataDTO> aggregatedData = influxDBService.queryAggregatedData(
                dto.getDeviceId(), startTime, endTime, aggregation, interval);

        List<InverterDataDTO> rawData = influxDBService.queryHistoryData(
                dto.getDeviceId(), startTime, endTime);

        DataPlaybackVO result = new DataPlaybackVO();
        result.setAggregatedData(aggregatedData);
        result.setFaultPoints(buildFaultPoints(rawData));
        result.setStatistics(buildStatistics(aggregatedData, rawData));
        result.setRootCauseAnalysis(analyzeRootCause(rawData));

        return result;
    }

    /**
     * 根据工单ID查询回放数据
     *
     * @param workOrderId 工单ID
     * @return 回放数据
     */
    public DataPlaybackVO queryPlaybackByWorkOrder(Long workOrderId) {
        if (workOrderId == null || jdbcTemplate == null) {
            return new DataPlaybackVO();
        }

        try {
            String sql = "SELECT inverter_id, create_time FROM work_order WHERE id = ?";
            Map<String, Object> workOrder = jdbcTemplate.queryForMap(sql, workOrderId);

            if (CollectionUtils.isEmpty(workOrder)) {
                return new DataPlaybackVO();
            }

            Object inverterIdObj = workOrder.get("inverter_id");
            Object createTimeObj = workOrder.get("create_time");

            if (inverterIdObj == null || createTimeObj == null) {
                return new DataPlaybackVO();
            }

            String deviceId = String.valueOf(inverterIdObj);
            long faultTime;
            if (createTimeObj instanceof LocalDateTime) {
                faultTime = ((LocalDateTime) createTimeObj)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } else if (createTimeObj instanceof java.util.Date) {
                faultTime = ((java.util.Date) createTimeObj).getTime();
            } else {
                faultTime = System.currentTimeMillis();
            }

            long startTime = faultTime - TWO_HOURS_MS;
            long endTime = faultTime + TWO_HOURS_MS;

            PlaybackQueryDTO dto = new PlaybackQueryDTO();
            dto.setDeviceId(deviceId);
            dto.setStartTime(startTime);
            dto.setEndTime(endTime);

            return queryPlaybackData(dto);
        } catch (Exception e) {
            log.warn("根据工单查询回放数据失败: workOrderId={}, error={}", workOrderId, e.getMessage());
            return new DataPlaybackVO();
        }
    }

    /**
     * 分析根因
     *
     * @param data 原始数据列表
     * @return 根因分析列表
     */
    public List<RootCauseVO> analyzeRootCause(List<InverterDataDTO> data) {
        List<RootCauseVO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(data) || data.size() < 2) {
            return result;
        }

        long totalDuration = data.get(data.size() - 1).getTimestamp() - data.get(0).getTimestamp();
        if (totalDuration <= 0) {
            return result;
        }

        analyzeTemperatureAbnormal(data, totalDuration, result);
        analyzeVoltageFluctuation(data, totalDuration, result);
        analyzePowerDrop(data, totalDuration, result);

        return result;
    }

    /**
     * 自动计算时间间隔
     * <1h=1m, <6h=5m, <24h=15m, 更长=1h
     */
    private String calculateInterval(long startTime, long endTime) {
        long durationMs = endTime - startTime;
        long oneHour = 60 * 60 * 1000L;
        long sixHours = 6 * oneHour;
        long twentyFourHours = 24 * oneHour;

        if (durationMs < oneHour) {
            return "1m";
        } else if (durationMs < sixHours) {
            return "5m";
        } else if (durationMs < twentyFourHours) {
            return "15m";
        } else {
            return "1h";
        }
    }

    /**
     * 构造故障点列表
     */
    private List<FaultPointVO> buildFaultPoints(List<InverterDataDTO> rawData) {
        List<FaultPointVO> faultPoints = new ArrayList<>();
        if (CollectionUtils.isEmpty(rawData)) {
            return faultPoints;
        }

        for (InverterDataDTO data : rawData) {
            if (data.getFaultCode() != null && data.getFaultCode() != 0) {
                FaultPointVO vo = new FaultPointVO();
                vo.setTimestamp(data.getTimestamp());
                vo.setFaultCode(data.getFaultCode());
                vo.setFaultDesc(getFaultDescription(data.getFaultCode()));
                vo.setDataPoint(data);
                faultPoints.add(vo);
            }
        }
        return faultPoints;
    }

    /**
     * 构造统计信息
     */
    private DataStatisticsVO buildStatistics(List<InverterDataDTO> aggregatedData,
                                              List<InverterDataDTO> rawData) {
        DataStatisticsVO statistics = new DataStatisticsVO();

        List<Double> powerList = new ArrayList<>();
        List<Double> tempList = new ArrayList<>();
        int faultCount = 0;

        List<InverterDataDTO> allData = !CollectionUtils.isEmpty(rawData) ? rawData : aggregatedData;
        if (CollectionUtils.isEmpty(allData)) {
            statistics.setAvgPower(0.0);
            statistics.setMaxPower(0.0);
            statistics.setAvgTemp(0.0);
            statistics.setMaxTemp(0.0);
            statistics.setFaultCount(0);
            statistics.setDataPointCount(0);
            return statistics;
        }

        for (InverterDataDTO data : allData) {
            if (data.getPower() != null) {
                powerList.add(data.getPower());
            }
            if (data.getTemperature() != null) {
                tempList.add(data.getTemperature());
            }
            if (data.getFaultCode() != null && data.getFaultCode() != 0) {
                faultCount++;
            }
        }

        statistics.setAvgPower(calculateAverage(powerList));
        statistics.setMaxPower(calculateMax(powerList));
        statistics.setAvgTemp(calculateAverage(tempList));
        statistics.setMaxTemp(calculateMax(tempList));
        statistics.setFaultCount(faultCount);
        statistics.setDataPointCount(allData.size());

        return statistics;
    }

    /**
     * 分析温度异常（连续>55℃）
     */
    private void analyzeTemperatureAbnormal(List<InverterDataDTO> data, long totalDuration,
                                            List<RootCauseVO> result) {
        long abnormalDuration = 0L;
        long segmentStart = -1L;
        int consecutiveCount = 0;
        int maxConsecutive = 0;

        for (int i = 0; i < data.size(); i++) {
            InverterDataDTO point = data.get(i);
            if (point.getTemperature() != null && point.getTemperature() > 55.0) {
                if (segmentStart < 0) {
                    segmentStart = point.getTimestamp();
                }
                consecutiveCount++;
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);

                if (i == data.size() - 1) {
                    abnormalDuration += point.getTimestamp() - segmentStart;
                }
            } else {
                if (segmentStart > 0 && consecutiveCount >= 3) {
                    abnormalDuration += data.get(i - 1).getTimestamp() - segmentStart;
                }
                segmentStart = -1L;
                consecutiveCount = 0;
            }
        }

        if (abnormalDuration > 0 && maxConsecutive >= 3) {
            RootCauseVO vo = new RootCauseVO();
            vo.setMetric("temperature");
            vo.setAbnormalType("温度过高");
            vo.setDescription("逆变器温度连续超过55℃，可能导致设备降额或故障停机");
            vo.setSuggestion("检查散热风扇是否正常运行，清理进风口灰尘，改善设备通风环境");
            vo.setConfidence(calculateConfidence(abnormalDuration, totalDuration));
            result.add(vo);
        }
    }

    /**
     * 分析电压波动（波动>20%）
     */
    private void analyzeVoltageFluctuation(List<InverterDataDTO> data, long totalDuration,
                                           List<RootCauseVO> result) {
        long abnormalDuration = 0L;
        int abnormalPointCount = 0;
        double baseVoltage = 0.0;

        List<Double> validVoltages = new ArrayList<>();
        for (InverterDataDTO point : data) {
            if (point.getVoltage() != null && point.getVoltage() > 0) {
                validVoltages.add(point.getVoltage());
            }
        }
        if (validVoltages.size() < 10) {
            return;
        }

        baseVoltage = calculateAverage(validVoltages);
        if (baseVoltage <= 0) {
            return;
        }

        long segmentStart = -1L;
        for (int i = 0; i < data.size(); i++) {
            InverterDataDTO point = data.get(i);
            if (point.getVoltage() != null && point.getVoltage() > 0) {
                double fluctuation = Math.abs(point.getVoltage() - baseVoltage) / baseVoltage;
                if (fluctuation > 0.20) {
                    abnormalPointCount++;
                    if (segmentStart < 0) {
                        segmentStart = point.getTimestamp();
                    }
                    if (i == data.size() - 1) {
                        abnormalDuration += point.getTimestamp() - segmentStart;
                    }
                } else {
                    if (segmentStart > 0) {
                        abnormalDuration += data.get(i - 1).getTimestamp() - segmentStart;
                    }
                    segmentStart = -1L;
                }
            } else {
                if (segmentStart > 0) {
                    abnormalDuration += data.get(i - 1).getTimestamp() - segmentStart;
                }
                segmentStart = -1L;
            }
        }

        if (abnormalPointCount >= 5) {
            RootCauseVO vo = new RootCauseVO();
            vo.setMetric("voltage");
            vo.setAbnormalType("电压不稳");
            vo.setDescription("电网电压波动超过20%，可能导致逆变器保护停机或输出异常");
            vo.setSuggestion("检查电网接入是否稳定，联系电力公司排查电网质量，考虑加装稳压设备");
            vo.setConfidence(calculateConfidence(abnormalDuration, totalDuration));
            result.add(vo);
        }
    }

    /**
     * 分析功率骤降（骤降>60%）
     */
    private void analyzePowerDrop(List<InverterDataDTO> data, long totalDuration,
                                  List<RootCauseVO> result) {
        long abnormalDuration = 0L;
        int dropEventCount = 0;
        long lastDropTime = -1L;

        for (int i = 1; i < data.size(); i++) {
            InverterDataDTO prev = data.get(i - 1);
            InverterDataDTO curr = data.get(i);

            if (prev.getPower() != null && curr.getPower() != null
                    && prev.getPower() > 0 && curr.getTimestamp() != null
                    && prev.getTimestamp() != null) {
                double dropRatio = (prev.getPower() - curr.getPower()) / prev.getPower();
                long timeDiff = curr.getTimestamp() - prev.getTimestamp();

                if (dropRatio > 0.60 && timeDiff < 10 * 60 * 1000L) {
                    dropEventCount++;
                    if (lastDropTime < 0
                            || curr.getTimestamp() - lastDropTime > 30 * 60 * 1000L) {
                        abnormalDuration += 5 * 60 * 1000L;
                        lastDropTime = curr.getTimestamp();
                    }
                }
            }
        }

        if (dropEventCount >= 3) {
            RootCauseVO vo = new RootCauseVO();
            vo.setMetric("power");
            vo.setAbnormalType("功率异常下跌");
            vo.setDescription("输出功率短时间内骤降超过60%，可能存在组件遮挡、接线松动或逆变器故障");
            vo.setSuggestion("检查光伏组件表面是否有遮挡，排查直流侧接线是否牢固，检测组件串输出是否正常");
            vo.setConfidence(calculateConfidence(abnormalDuration, totalDuration));
            result.add(vo);
        }
    }

    /**
     * 计算置信度（异常持续时长/总时长，上限0.95）
     */
    private Double calculateConfidence(long abnormalDuration, long totalDuration) {
        if (totalDuration <= 0 || abnormalDuration <= 0) {
            return 0.5;
        }
        double ratio = (double) abnormalDuration / totalDuration;
        double confidence = Math.min(ratio, 0.95);
        confidence = Math.max(confidence, 0.3);
        return BigDecimal.valueOf(confidence)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 计算平均值
     */
    private Double calculateAverage(List<Double> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double value : list) {
            sum += value;
        }
        return BigDecimal.valueOf(sum / list.size())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 计算最大值
     */
    private Double calculateMax(List<Double> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0.0;
        }
        double max = Double.MIN_VALUE;
        for (Double value : list) {
            if (value > max) {
                max = value;
            }
        }
        return BigDecimal.valueOf(max)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 获取故障码描述
     */
    private String getFaultDescription(Integer faultCode) {
        switch (faultCode) {
            case 1: return "直流侧过压保护";
            case 2: return "直流侧短路故障";
            case 3: return "逆变器过温保护";
            case 4: return "通讯中断故障";
            case 5: return "电网离网保护";
            case 6: return "逆变器效率异常";
            case 7: return "组串无输出";
            case 8: return "组件热斑故障";
            case 9: return "散热风扇故障";
            case 10: return "直流绝缘故障";
            default: return "未知故障码: " + faultCode;
        }
    }
}
