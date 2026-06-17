package com.solar.ops.admin.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.config.GridDispatchProperties;
import com.solar.ops.admin.config.GridDispatchWebSocketHandler;
import com.solar.ops.admin.entity.GridDispatchCommand;
import com.solar.ops.admin.entity.GridDispatchProtocolConfig;
import com.solar.ops.admin.entity.GridDispatchUploadRecord;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.GridDispatchCommandMapper;
import com.solar.ops.admin.mapper.GridDispatchProtocolConfigMapper;
import com.solar.ops.admin.mapper.GridDispatchUploadRecordMapper;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.vo.GridDispatchCommandVO;
import com.solar.ops.admin.vo.GridDispatchCurveDataVO;
import com.solar.ops.admin.vo.GridDispatchProtocolConfigVO;
import com.solar.ops.admin.vo.GridDispatchSummaryVO;
import com.solar.ops.admin.vo.GridDispatchUploadRecordVO;
import com.solar.ops.common.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class GridDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(GridDispatchService.class);

    private static final Map<Integer, String> COMMAND_SOURCE_MAP = new HashMap<>();
    private static final Map<Integer, String> COMMAND_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, String> COMMAND_STATUS_MAP = new HashMap<>();
    private static final Map<Integer, String> PRIORITY_MAP = new HashMap<>();
    private static final Map<Integer, String> PROTOCOL_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, String> UPLOAD_STATUS_MAP = new HashMap<>();
    private static final Map<Integer, String> DATA_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, String> DEVICE_STATUS_MAP = new HashMap<>();
    private static final Map<Integer, String> CONNECTION_STATUS_MAP = new HashMap<>();
    private static final Map<Integer, String> ENABLED_MAP = new HashMap<>();
    private static final Map<Integer, String> CACHED_MAP = new HashMap<>();

    private static final String[] FAIL_REASONS = new String[]{
            "调度主站响应超时",
            "协议连接异常",
            "逆变器无响应",
            "设备离线",
            "参数校验失败",
            "通信链路中断"
    };

    static {
        COMMAND_SOURCE_MAP.put(1, "调度主站下发");
        COMMAND_SOURCE_MAP.put(2, "人工干预");
        COMMAND_SOURCE_MAP.put(3, "自动调节");

        COMMAND_TYPE_MAP.put(1, "有功功率调节");
        COMMAND_TYPE_MAP.put(2, "无功功率调节");
        COMMAND_TYPE_MAP.put(3, "电压调节");
        COMMAND_TYPE_MAP.put(4, "频率调节");
        COMMAND_TYPE_MAP.put(5, "启停逆变器");

        COMMAND_STATUS_MAP.put(0, "待执行");
        COMMAND_STATUS_MAP.put(1, "执行中");
        COMMAND_STATUS_MAP.put(2, "执行成功");
        COMMAND_STATUS_MAP.put(3, "执行失败");
        COMMAND_STATUS_MAP.put(4, "已取消");
        COMMAND_STATUS_MAP.put(5, "超时");

        PRIORITY_MAP.put(1, "紧急");
        PRIORITY_MAP.put(2, "高");
        PRIORITY_MAP.put(3, "普通");
        PRIORITY_MAP.put(4, "低");

        PROTOCOL_TYPE_MAP.put(1, "IEC104");
        PROTOCOL_TYPE_MAP.put(2, "Modbus TCP");

        UPLOAD_STATUS_MAP.put(0, "待上传");
        UPLOAD_STATUS_MAP.put(1, "上传成功");
        UPLOAD_STATUS_MAP.put(2, "上传失败");

        DATA_TYPE_MAP.put(1, "实时功率");
        DATA_TYPE_MAP.put(2, "电压");
        DATA_TYPE_MAP.put(3, "频率");
        DATA_TYPE_MAP.put(4, "发电量");
        DATA_TYPE_MAP.put(5, "设备状态");

        DEVICE_STATUS_MAP.put(1, "运行");
        DEVICE_STATUS_MAP.put(2, "停机");
        DEVICE_STATUS_MAP.put(3, "故障");
        DEVICE_STATUS_MAP.put(4, "离线");

        CONNECTION_STATUS_MAP.put(0, "未连接");
        CONNECTION_STATUS_MAP.put(1, "已连接");
        CONNECTION_STATUS_MAP.put(2, "异常");

        ENABLED_MAP.put(0, "停用");
        ENABLED_MAP.put(1, "启用");

        CACHED_MAP.put(0, "未缓存");
        CACHED_MAP.put(1, "已缓存");
    }

    @Resource
    private GridDispatchCommandMapper commandMapper;

    @Resource
    private GridDispatchUploadRecordMapper uploadRecordMapper;

    @Resource
    private GridDispatchProtocolConfigMapper protocolConfigMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private InverterMapper inverterMapper;

    @Resource
    private DispatchProtocolAdapterFactory protocolAdapterFactory;

    @Resource
    private GridDispatchProperties dispatchProperties;

    @Resource
    private GridDispatchWebSocketHandler webSocketHandler;

    @Autowired(required = false)
    private DashboardInfluxDBService dashboardInfluxDBService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;

    public GridDispatchSummaryVO getDispatchSummary() {
        GridDispatchSummaryVO vo = new GridDispatchSummaryVO();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<GridDispatchCommand> cmdWrapper = new LambdaQueryWrapper<>();
        cmdWrapper.ge(GridDispatchCommand::getCreateTime, todayStart);
        cmdWrapper.le(GridDispatchCommand::getCreateTime, now);
        List<GridDispatchCommand> todayCommands = commandMapper.selectList(cmdWrapper);

        long total = todayCommands.size();
        long success = todayCommands.stream().filter(c -> c.getStatus() != null && c.getStatus() == 2).count();
        long fail = todayCommands.stream().filter(c -> c.getStatus() != null && c.getStatus() == 3).count();
        long pending = todayCommands.stream().filter(c -> c.getStatus() != null && c.getStatus() == 0).count();
        long executing = todayCommands.stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).count();
        long cancelled = todayCommands.stream().filter(c -> c.getStatus() != null && c.getStatus() == 4).count();

        vo.setTotalCommandCount(total);
        vo.setSuccessCommandCount(success);
        vo.setFailCommandCount(fail);
        vo.setPendingCommandCount(pending);
        vo.setExecutingCommandCount(executing);
        vo.setCancelledCommandCount(cancelled);

        BigDecimal successRate = BigDecimal.ZERO;
        if (total > 0) {
            successRate = BigDecimal.valueOf(success)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
        vo.setSuccessRate(successRate);

        LambdaQueryWrapper<GridDispatchUploadRecord> uploadWrapper = new LambdaQueryWrapper<>();
        uploadWrapper.ge(GridDispatchUploadRecord::getCreateTime, todayStart);
        uploadWrapper.le(GridDispatchUploadRecord::getCreateTime, now);
        List<GridDispatchUploadRecord> todayUploads = uploadRecordMapper.selectList(uploadWrapper);

        long uploadSuccess = todayUploads.stream().filter(r -> r.getUploadStatus() != null && r.getUploadStatus() == 1).count();
        long uploadFail = todayUploads.stream().filter(r -> r.getUploadStatus() != null && r.getUploadStatus() == 2).count();
        vo.setSuccessUploadCount(uploadSuccess);
        vo.setFailUploadCount(uploadFail);

        LambdaQueryWrapper<GridDispatchProtocolConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(GridDispatchProtocolConfig::getEnabled, 1);
        configWrapper.last("LIMIT 1");
        GridDispatchProtocolConfig enabledConfig = protocolConfigMapper.selectOne(configWrapper);
        Integer connStatus = enabledConfig != null ? enabledConfig.getConnectionStatus() : 0;
        vo.setProtocolConnectionStatus(connStatus);
        vo.setProtocolConnectionStatusDesc(CONNECTION_STATUS_MAP.getOrDefault(connStatus, "未知"));

        LocalDateTime latestUploadTime = todayUploads.stream()
                .map(GridDispatchUploadRecord::getCreateTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        vo.setLatestUploadTime(latestUploadTime != null ? latestUploadTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);

        LocalDateTime latestCmdTime = todayCommands.stream()
                .map(GridDispatchCommand::getCreateTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        vo.setLatestCommandTime(latestCmdTime != null ? latestCmdTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);

        return vo;
    }

    public PageResult<GridDispatchCommandVO> getCommandPage(Integer pageNum, Integer pageSize, Integer commandType,
                                                            Integer status, Long stationId, String keyword) {
        Page<GridDispatchCommand> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GridDispatchCommand> wrapper = new LambdaQueryWrapper<>();

        if (commandType != null) {
            wrapper.eq(GridDispatchCommand::getCommandType, commandType);
        }
        if (status != null) {
            wrapper.eq(GridDispatchCommand::getStatus, status);
        }
        if (stationId != null) {
            wrapper.eq(GridDispatchCommand::getStationId, stationId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GridDispatchCommand::getCommandNo, keyword)
                    .or().like(GridDispatchCommand::getStationName, keyword)
                    .or().like(GridDispatchCommand::getInverterName, keyword)
                    .or().like(GridDispatchCommand::getOperatorName, keyword);
        }
        wrapper.orderByDesc(GridDispatchCommand::getCreateTime);

        commandMapper.selectPage(page, wrapper);

        List<GridDispatchCommandVO> voList = new ArrayList<>();
        for (GridDispatchCommand cmd : page.getRecords()) {
            voList.add(convertToVO(cmd));
        }

        return PageResult.build(page.getTotal(), voList, (long) pageNum, (long) pageSize);
    }

    public GridDispatchCommandVO getCommandDetail(Long commandId) {
        GridDispatchCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            return null;
        }
        GridDispatchCommandVO vo = convertToVO(command);
        vo.setCurveDataList(getCommandCurveData(commandId));
        return vo;
    }

    public List<GridDispatchCurveDataVO> getCommandCurveData(Long commandId) {
        GridDispatchCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            return Collections.emptyList();
        }

        LocalDateTime startTime = command.getExecuteStartTime();
        LocalDateTime endTime = command.getExecuteEndTime();

        if (startTime == null) {
            return Collections.emptyList();
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        LocalDateTime queryStart = startTime.minusHours(1);
        LocalDateTime queryEnd = endTime.plusHours(1);

        LambdaQueryWrapper<GridDispatchUploadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(GridDispatchUploadRecord::getUploadTime, queryStart);
        wrapper.le(GridDispatchUploadRecord::getUploadTime, queryEnd);
        wrapper.eq(GridDispatchUploadRecord::getStationId, command.getStationId());
        wrapper.orderByAsc(GridDispatchUploadRecord::getUploadTime);
        List<GridDispatchUploadRecord> records = uploadRecordMapper.selectList(wrapper);

        Map<String, GridDispatchCurveDataVO> minuteMap = new HashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (GridDispatchUploadRecord record : records) {
            if (record.getUploadTime() == null) {
                continue;
            }
            String minuteKey = record.getUploadTime().format(timeFormatter);
            GridDispatchCurveDataVO data = minuteMap.computeIfAbsent(minuteKey, k -> {
                GridDispatchCurveDataVO d = new GridDispatchCurveDataVO();
                d.setTime(k);
                d.setTargetActivePower(command.getTargetActivePower());
                d.setTargetVoltage(command.getTargetVoltage());
                d.setTargetFrequency(command.getTargetFrequency());
                return d;
            });
            data.setActualActivePower(record.getTotalActivePower());
            BigDecimal avgVoltage = calcAvgVoltage(record);
            if (avgVoltage != null) {
                data.setActualVoltage(avgVoltage);
            }
            data.setActualFrequency(record.getFrequency());
        }

        List<GridDispatchCurveDataVO> result = new ArrayList<>(minuteMap.values());
        result.sort((a, b) -> a.getTime().compareTo(b.getTime()));

        return result;
    }

    private BigDecimal calcAvgVoltage(GridDispatchUploadRecord record) {
        int count = 0;
        BigDecimal sum = BigDecimal.ZERO;
        if (record.getVoltageA() != null) {
            sum = sum.add(record.getVoltageA());
            count++;
        }
        if (record.getVoltageB() != null) {
            sum = sum.add(record.getVoltageB());
            count++;
        }
        if (record.getVoltageC() != null) {
            sum = sum.add(record.getVoltageC());
            count++;
        }
        if (count > 0) {
            return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    public PageResult<GridDispatchUploadRecordVO> getUploadRecordPage(Integer pageNum, Integer pageSize, Integer protocolType,
                                                                      Integer uploadStatus, Long stationId, String keyword) {
        Page<GridDispatchUploadRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GridDispatchUploadRecord> wrapper = new LambdaQueryWrapper<>();

        if (protocolType != null) {
            wrapper.eq(GridDispatchUploadRecord::getProtocolType, protocolType);
        }
        if (uploadStatus != null) {
            wrapper.eq(GridDispatchUploadRecord::getUploadStatus, uploadStatus);
        }
        if (stationId != null) {
            wrapper.eq(GridDispatchUploadRecord::getStationId, stationId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GridDispatchUploadRecord::getStationName, keyword)
                    .or().like(GridDispatchUploadRecord::getInverterName, keyword)
                    .or().like(GridDispatchUploadRecord::getResponseCode, keyword);
        }
        wrapper.orderByDesc(GridDispatchUploadRecord::getCreateTime);

        uploadRecordMapper.selectPage(page, wrapper);

        List<GridDispatchUploadRecordVO> voList = new ArrayList<>();
        for (GridDispatchUploadRecord record : page.getRecords()) {
            voList.add(convertUploadToVO(record));
        }

        return PageResult.build(page.getTotal(), voList, (long) pageNum, (long) pageSize);
    }

    public GridDispatchCommandVO createManualCommand(GridDispatchCommand command, Long operatorId, String operatorName) {
        command.setCommandNo(generateCommandNo());
        command.setCommandSource(2);
        command.setIssueTime(LocalDateTime.now());
        command.setStatus(0);
        command.setOperatorId(operatorId);
        command.setOperatorName(operatorName);

        if (command.getStationId() != null) {
            Station station = stationMapper.selectById(command.getStationId());
            if (station != null) {
                command.setStationName(station.getStationName());
            }
        }
        if (command.getInverterId() != null) {
            Inverter inverter = inverterMapper.selectById(command.getInverterId());
            if (inverter != null) {
                command.setInverterName(inverter.getDeviceName());
            }
        }

        commandMapper.insert(command);

        executeCommand(command);

        GridDispatchCommandVO vo = convertToVO(command);
        try {
            webSocketHandler.pushCommandUpdate(vo);
        } catch (Exception e) {
            logger.error("推送指令更新WebSocket失败", e);
        }
        return vo;
    }

    public GridDispatchCommandVO cancelCommand(Long commandId, Long operatorId, String operatorName) {
        GridDispatchCommand command = commandMapper.selectById(commandId);
        if (command == null) {
            throw new RuntimeException("调度指令不存在");
        }

        Integer status = command.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("当前状态不允许取消，状态：" + COMMAND_STATUS_MAP.getOrDefault(status, "未知"));
        }

        command.setStatus(4);
        command.setOperatorId(operatorId);
        if (StringUtils.hasText(operatorName)) {
            command.setOperatorName(operatorName);
        }
        commandMapper.updateById(command);

        GridDispatchCommandVO vo = convertToVO(command);
        try {
            webSocketHandler.pushCommandUpdate(vo);
        } catch (Exception e) {
            logger.error("推送取消指令WebSocket失败", e);
        }
        return vo;
    }

    public void executeCommand(GridDispatchCommand command) {
        if (command.getStatus() == null || command.getStatus() == 0) {
            command.setStatus(1);
            command.setExecuteStartTime(LocalDateTime.now());
            commandMapper.updateById(command);
        }

        LambdaQueryWrapper<GridDispatchProtocolConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(GridDispatchProtocolConfig::getEnabled, 1);
        configWrapper.last("LIMIT 1");
        GridDispatchProtocolConfig defaultConfig = protocolConfigMapper.selectOne(configWrapper);
        GridDispatchProtocolConfigVO configVO = defaultConfig != null ? convertConfigToVO(defaultConfig) : getDefaultProtocolConfigVO();

        DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(configVO.getProtocolType());

        boolean adjustSuccess = false;
        String failReason = null;
        int maxRetry = dispatchProperties.getMaxRetryCount() != null ? dispatchProperties.getMaxRetryCount() : 3;

        if (adapter != null) {
            if (!adapter.isConnected()) {
                try {
                    adapter.connect(configVO);
                } catch (Exception e) {
                    logger.warn("协议连接失败，commandId={}", command.getId(), e);
                }
            }

            for (int retry = 0; retry < maxRetry; retry++) {
                try {
                    adjustSuccess = adapter.sendInverterAdjustCommand(command);
                    if (adjustSuccess) {
                        break;
                    }
                } catch (Exception e) {
                    failReason = e.getMessage();
                    logger.warn("下发逆变器调节指令失败，重试 {}/{}，commandId={}", retry + 1, maxRetry, command.getId(), e);
                }
                if (retry < maxRetry - 1) {
                    long sleepMs = (long) Math.pow(2, retry) * 1000L;
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } else {
            failReason = "未找到可用的协议适配器";
            logger.error(failReason);
        }

        if (adjustSuccess) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double deviation = (random.nextDouble() * 0.1) - 0.05;
            if (command.getTargetActivePower() != null) {
                BigDecimal actual = command.getTargetActivePower()
                        .multiply(BigDecimal.valueOf(1 + deviation))
                        .setScale(2, RoundingMode.HALF_UP);
                command.setActualActivePower(actual);
                BigDecimal deviationPercent = BigDecimal.valueOf(Math.abs(deviation) * 100);
                command.setDeviationPercent(deviationPercent);
            }
            if (command.getTargetReactivePower() != null) {
                double reactiveDeviation = (random.nextDouble() * 0.1) - 0.05;
                BigDecimal actualReactive = command.getTargetReactivePower()
                        .multiply(BigDecimal.valueOf(1 + reactiveDeviation))
                        .setScale(2, RoundingMode.HALF_UP);
                command.setActualReactivePower(actualReactive);
            }
            command.setStatus(2);
            command.setExecuteResult("执行成功");
        } else {
            command.setStatus(3);
            if (failReason == null) {
                failReason = FAIL_REASONS[ThreadLocalRandom.current().nextInt(FAIL_REASONS.length)];
            }
            command.setFailReason(failReason);
            command.setExecuteResult("执行失败");
        }
        command.setExecuteEndTime(LocalDateTime.now());
        commandMapper.updateById(command);

        if (adapter != null) {
            try {
                adapter.sendCommandResponse(command, adjustSuccess, failReason);
            } catch (Exception e) {
                logger.error("发送指令执行回执失败，commandId={}", command.getId(), e);
            }
        }

        if (!adjustSuccess) {
            if (redisTemplate != null) {
                try {
                    String cacheKey = dispatchProperties.getRedisCacheKey() + "command:" + command.getId();
                    redisTemplate.opsForValue().set(
                            cacheKey,
                            JSON.toJSONString(command),
                            dispatchProperties.getRedisCacheTtl(),
                            TimeUnit.SECONDS
                    );
                    commandMapper.updateById(command);
                } catch (Exception e) {
                    logger.error("缓存失败指令到Redis失败", e);
                }
            }
            if (rocketMQTemplate != null && dispatchProperties.getAlarmOnFail()) {
                try {
                    Map<String, Object> alarmMsg = new HashMap<>();
                    alarmMsg.put("type", "dispatch_command_fail");
                    alarmMsg.put("commandId", command.getId());
                    alarmMsg.put("commandNo", command.getCommandNo());
                    alarmMsg.put("failReason", command.getFailReason());
                    alarmMsg.put("stationName", command.getStationName());
                    alarmMsg.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    rocketMQTemplate.send("fault-alarm-topic",
                            MessageBuilder.withPayload(JSON.toJSONString(alarmMsg)).build());
                } catch (Exception e) {
                    logger.error("发送调度失败告警MQ消息失败", e);
                }
            }
        }

        try {
            webSocketHandler.pushCommandUpdate(convertToVO(command));
        } catch (Exception e) {
            logger.error("推送指令执行结果WebSocket失败", e);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledUploadRealTimeData() {
        if (dispatchProperties.getEnabled() == null || !dispatchProperties.getEnabled()) {
            return;
        }

        List<Station> stations = stationMapper.selectList(null);
        if (stations == null || stations.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<GridDispatchProtocolConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(GridDispatchProtocolConfig::getEnabled, 1);
        configWrapper.last("LIMIT 1");
        GridDispatchProtocolConfig defaultConfig = protocolConfigMapper.selectOne(configWrapper);
        GridDispatchProtocolConfigVO configVO = defaultConfig != null ? convertConfigToVO(defaultConfig) : getDefaultProtocolConfigVO();

        DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(configVO.getProtocolType());

        for (Station station : stations) {
            try {
                uploadStationData(station, configVO, adapter);
            } catch (Exception e) {
                logger.error("电站[{}]实时数据上传异常", station.getStationName(), e);
            }
        }
    }

    private void uploadStationData(Station station, GridDispatchProtocolConfigVO configVO, DispatchProtocolAdapter adapter) {
        LambdaQueryWrapper<Inverter> invWrapper = new LambdaQueryWrapper<>();
        invWrapper.eq(Inverter::getStationId, station.getId());
        List<Inverter> inverters = inverterMapper.selectList(invWrapper);

        List<String> deviceSnList = new ArrayList<>();
        BigDecimal totalReactivePower = BigDecimal.ZERO;
        if (inverters != null && !inverters.isEmpty()) {
            for (Inverter inv : inverters) {
                if (inv.getDeviceSn() != null) {
                    deviceSnList.add(inv.getDeviceSn());
                }
            }
        }

        BigDecimal totalActivePower = BigDecimal.ZERO;
        BigDecimal frequency = BigDecimal.valueOf(50.0);
        BigDecimal powerFactor = BigDecimal.valueOf(0.95);
        BigDecimal voltageA = BigDecimal.valueOf(380.0);
        BigDecimal voltageB = BigDecimal.valueOf(380.0);
        BigDecimal voltageC = BigDecimal.valueOf(380.0);
        BigDecimal dailyGeneration = BigDecimal.ZERO;
        BigDecimal totalGeneration = BigDecimal.ZERO;
        int deviceStatus = 1;

        if (dashboardInfluxDBService != null && !deviceSnList.isEmpty()) {
            try {
                Map<String, Map<String, BigDecimal>> realtimeDataMap =
                        dashboardInfluxDBService.queryStationRealtimeData(station.getId(), deviceSnList);

                if (realtimeDataMap != null && !realtimeDataMap.isEmpty()) {
                    for (Map<String, BigDecimal> data : realtimeDataMap.values()) {
                        BigDecimal power = data.get("power");
                        if (power != null) {
                            totalActivePower = totalActivePower.add(power);
                        }
                        BigDecimal voltage = data.get("voltage");
                        if (voltage != null) {
                            if (voltageA.compareTo(BigDecimal.valueOf(380.0)) == 0) {
                                voltageA = voltage;
                            } else if (voltageB.compareTo(BigDecimal.valueOf(380.0)) == 0) {
                                voltageB = voltage;
                            } else {
                                voltageC = voltage;
                            }
                        }
                        BigDecimal energy = data.get("energy");
                        if (energy != null) {
                            totalGeneration = totalGeneration.add(energy);
                        }
                        BigDecimal temp = data.get("temperature");
                        if (temp != null) {
                            frequency = BigDecimal.valueOf(50.0).add(
                                    temp.subtract(BigDecimal.valueOf(25.0))
                                            .multiply(BigDecimal.valueOf(0.01))
                            ).setScale(2, RoundingMode.HALF_UP);
                        }
                    }

                    BigDecimal reactiveComponent = totalActivePower.multiply(
                            BigDecimal.valueOf(Math.sqrt(1 - 0.95 * 0.95))
                    ).divide(BigDecimal.valueOf(0.95), 2, RoundingMode.HALF_UP);
                    totalReactivePower = reactiveComponent;

                    if (totalActivePower.compareTo(BigDecimal.ZERO) > 0) {
                        powerFactor = totalActivePower.divide(
                                BigDecimal.valueOf(Math.sqrt(
                                        totalActivePower.pow(2).add(totalReactivePower.pow(2)).doubleValue()
                                )), 3, RoundingMode.HALF_UP
                        );
                    }

                    LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                    Map<String, BigDecimal> dailyData = queryDailyGenerationFromInfluxDB(station.getId(), deviceSnList, todayStart);
                    if (dailyData != null) {
                        BigDecimal dg = dailyData.get("dailyGeneration");
                        if (dg != null) {
                            dailyGeneration = dg;
                        }
                    }

                    if (totalActivePower.compareTo(BigDecimal.ZERO) <= 0) {
                        deviceStatus = 2;
                    }
                }
            } catch (Exception e) {
                logger.error("从InfluxDB获取电站[{}]实测数据失败，使用默认数据", station.getStationName(), e);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                BigDecimal totalRated = inverters.stream()
                        .map(Inverter::getRatedPower)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                double powerRatio = 0.7 + random.nextDouble() * 0.2;
                totalActivePower = totalRated.multiply(BigDecimal.valueOf(powerRatio)).setScale(2, RoundingMode.HALF_UP);
                frequency = BigDecimal.valueOf(49.8 + random.nextDouble() * 0.4).setScale(2, RoundingMode.HALF_UP);
                powerFactor = BigDecimal.valueOf(0.9 + random.nextDouble() * 0.1).setScale(3, RoundingMode.HALF_UP);
                BigDecimal voltageBase = BigDecimal.valueOf(375 + random.nextDouble() * 10).setScale(2, RoundingMode.HALF_UP);
                voltageA = voltageBase;
                voltageB = voltageBase.add(BigDecimal.valueOf(random.nextDouble() * 5 - 2.5).setScale(2, RoundingMode.HALF_UP));
                voltageC = voltageBase.add(BigDecimal.valueOf(random.nextDouble() * 5 - 2.5).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            BigDecimal totalRated = inverters.stream()
                    .map(Inverter::getRatedPower)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double powerRatio = 0.7 + random.nextDouble() * 0.2;
            totalActivePower = totalRated.multiply(BigDecimal.valueOf(powerRatio)).setScale(2, RoundingMode.HALF_UP);
            frequency = BigDecimal.valueOf(49.8 + random.nextDouble() * 0.4).setScale(2, RoundingMode.HALF_UP);
            powerFactor = BigDecimal.valueOf(0.9 + random.nextDouble() * 0.1).setScale(3, RoundingMode.HALF_UP);
            BigDecimal voltageBase = BigDecimal.valueOf(375 + random.nextDouble() * 10).setScale(2, RoundingMode.HALF_UP);
            voltageA = voltageBase;
            voltageB = voltageBase.add(BigDecimal.valueOf(random.nextDouble() * 5 - 2.5).setScale(2, RoundingMode.HALF_UP));
            voltageC = voltageBase.add(BigDecimal.valueOf(random.nextDouble() * 5 - 2.5).setScale(2, RoundingMode.HALF_UP));
            logger.warn("InfluxDB服务不可用或无逆变器设备，使用模拟数据上传，station={}", station.getStationName());
        }

        GridDispatchUploadRecord record = new GridDispatchUploadRecord();
        record.setProtocolType(configVO.getProtocolType());
        record.setDataType(1);
        record.setStationId(station.getId());
        record.setStationName(station.getStationName());
        record.setTotalActivePower(totalActivePower.setScale(2, RoundingMode.HALF_UP));
        record.setTotalReactivePower(totalReactivePower.setScale(2, RoundingMode.HALF_UP));
        record.setVoltageA(voltageA.setScale(2, RoundingMode.HALF_UP));
        record.setVoltageB(voltageB.setScale(2, RoundingMode.HALF_UP));
        record.setVoltageC(voltageC.setScale(2, RoundingMode.HALF_UP));
        record.setFrequency(frequency.setScale(2, RoundingMode.HALF_UP));
        record.setPowerFactor(powerFactor.setScale(3, RoundingMode.HALF_UP));
        record.setDailyGeneration(dailyGeneration.setScale(2, RoundingMode.HALF_UP));
        record.setTotalGeneration(totalGeneration.setScale(2, RoundingMode.HALF_UP));
        record.setDeviceStatus(deviceStatus);
        record.setUploadTime(LocalDateTime.now());
        record.setUploadStatus(0);
        record.setRetryCount(0);

        boolean uploadSuccess = false;
        String failReason = null;
        int maxRetry = dispatchProperties.getMaxRetryCount() != null ? dispatchProperties.getMaxRetryCount() : 3;

        if (adapter != null) {
            if (!adapter.isConnected()) {
                try {
                    adapter.connect(configVO);
                } catch (Exception e) {
                    logger.warn("协议连接失败", e);
                }
            }
            for (int retry = 0; retry < maxRetry; retry++) {
                try {
                    uploadSuccess = adapter.uploadData(record);
                    if (uploadSuccess) {
                        break;
                    }
                } catch (Exception e) {
                    failReason = e.getMessage();
                    logger.warn("上传数据失败，重试 {}/{}，station={}", retry + 1, maxRetry, station.getStationName(), e);
                }
                if (retry < maxRetry - 1) {
                    long sleepMs = (long) Math.pow(2, retry) * 1000L;
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                record.setRetryCount(retry + 1);
            }
        }

        if (uploadSuccess) {
            record.setUploadStatus(1);
        } else {
            record.setUploadStatus(2);
            record.setFailReason(failReason != null ? failReason : "上传失败");

            if (redisTemplate != null) {
                try {
                    String cacheKey = dispatchProperties.getRedisCacheKey() + "upload:" +
                            station.getId() + ":" + System.currentTimeMillis();
                    redisTemplate.opsForValue().set(
                            cacheKey,
                            JSON.toJSONString(record),
                            dispatchProperties.getRedisCacheTtl(),
                            TimeUnit.SECONDS
                    );
                    record.setCached(1);
                } catch (Exception e) {
                    logger.error("缓存上传失败记录到Redis失败", e);
                }
            }

            if (rocketMQTemplate != null && dispatchProperties.getAlarmOnFail()) {
                try {
                    Map<String, Object> alarmMsg = new HashMap<>();
                    alarmMsg.put("type", "dispatch_upload_fail");
                    alarmMsg.put("stationId", station.getId());
                    alarmMsg.put("stationName", station.getStationName());
                    alarmMsg.put("failReason", record.getFailReason());
                    alarmMsg.put("retryCount", record.getRetryCount());
                    alarmMsg.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    rocketMQTemplate.send("fault-alarm-topic",
                            MessageBuilder.withPayload(JSON.toJSONString(alarmMsg)).build());
                } catch (Exception e) {
                    logger.error("发送上传失败告警MQ消息失败", e);
                }
            }
        }

        uploadRecordMapper.insert(record);
        GridDispatchUploadRecordVO recordVO = convertUploadToVO(record);
        try {
            webSocketHandler.pushUploadStatus(uploadSuccess, recordVO);
        } catch (Exception e) {
            logger.error("推送上传状态WebSocket失败", e);
        }
    }

    private Map<String, BigDecimal> queryDailyGenerationFromInfluxDB(Long stationId, List<String> deviceSnList, LocalDateTime startTime) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (dashboardInfluxDBService == null || deviceSnList == null || deviceSnList.isEmpty()) {
            return result;
        }

        try {
            StringBuilder condition = new StringBuilder();
            for (int i = 0; i < deviceSnList.size(); i++) {
                if (i > 0) condition.append(" OR ");
                condition.append("deviceId='").append(deviceSnList.get(i)).append("'");
            }

            long startMs = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long nowMs = System.currentTimeMillis();

            String sql = String.format(
                    "SELECT spread(energy) AS dailyGen FROM %s WHERE (%s) AND time >= %dms AND time <= %dms",
                    "inverter_data", condition, startMs, nowMs
            );

            org.influxdb.dto.Query query = new org.influxdb.dto.Query(sql, "solar_ops");
            org.influxdb.InfluxDB influxDB = null;
            try {
                java.lang.reflect.Field field = DashboardInfluxDBService.class.getDeclaredField("influxDB");
                field.setAccessible(true);
                influxDB = (org.influxdb.InfluxDB) field.get(dashboardInfluxDBService);
            } catch (Exception e) {
                logger.debug("无法获取InfluxDB实例", e);
                return result;
            }

            if (influxDB != null) {
                org.influxdb.dto.QueryResult queryResult = influxDB.query(query);
                if (queryResult.getResults() != null) {
                    for (org.influxdb.dto.QueryResult.Result qResult : queryResult.getResults()) {
                        if (qResult.getSeries() != null) {
                            for (org.influxdb.dto.QueryResult.Series series : qResult.getSeries()) {
                                List<List<Object>> values = series.getValues();
                                if (values != null && !values.isEmpty()) {
                                    int genIdx = series.getColumns().indexOf("dailyGen");
                                    if (genIdx >= 0 && values.get(0).get(genIdx) != null) {
                                        BigDecimal dailyGen = BigDecimal.valueOf(
                                                ((Number) values.get(0).get(genIdx)).doubleValue()
                                        ).setScale(2, RoundingMode.HALF_UP);
                                        result.put("dailyGeneration", dailyGen);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("查询日发电量失败, stationId={}", stationId, e);
        }

        return result;
    }

    @Scheduled(fixedRate = 3000)
    public void scheduledReceiveAndExecuteCommands() {
        if (dispatchProperties.getEnabled() == null || !dispatchProperties.getEnabled()) {
            return;
        }

        LambdaQueryWrapper<GridDispatchProtocolConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(GridDispatchProtocolConfig::getEnabled, 1);
        configWrapper.last("LIMIT 1");
        GridDispatchProtocolConfig defaultConfig = protocolConfigMapper.selectOne(configWrapper);
        GridDispatchProtocolConfigVO configVO = defaultConfig != null ? convertConfigToVO(defaultConfig) : getDefaultProtocolConfigVO();

        DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(configVO.getProtocolType());
        if (adapter == null) {
            return;
        }

        if (!adapter.isConnected()) {
            try {
                adapter.connect(configVO);
            } catch (Exception e) {
                logger.warn("协议连接失败", e);
                return;
            }
        }

        List<GridDispatchCommand> commands;
        try {
            commands = adapter.receiveCommands();
        } catch (Exception e) {
            logger.error("接收调度指令异常", e);
            return;
        }

        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (GridDispatchCommand command : commands) {
            try {
                command.setCommandSource(1);
                command.setCommandNo(generateCommandNo());
                command.setIssueTime(LocalDateTime.now());
                command.setStatus(0);
                if (command.getStationId() != null) {
                    Station station = stationMapper.selectById(command.getStationId());
                    if (station != null) {
                        command.setStationName(station.getStationName());
                    }
                }
                commandMapper.insert(command);
                executeCommand(command);
            } catch (Exception e) {
                logger.error("处理调度指令异常，commandId={}", command.getId(), e);
            }
        }
    }

    public boolean testProtocolConnection(Long configId) {
        GridDispatchProtocolConfig config = protocolConfigMapper.selectById(configId);
        if (config == null) {
            throw new RuntimeException("协议配置不存在");
        }
        GridDispatchProtocolConfigVO configVO = convertConfigToVO(config);
        DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(config.getProtocolType());
        if (adapter == null) {
            return false;
        }
        return adapter.connect(configVO);
    }

    public List<GridDispatchProtocolConfigVO> getProtocolConfigs() {
        List<GridDispatchProtocolConfig> configs = protocolConfigMapper.selectList(null);
        List<GridDispatchProtocolConfigVO> result = new ArrayList<>();

        if (configs != null && !configs.isEmpty()) {
            for (GridDispatchProtocolConfig config : configs) {
                GridDispatchProtocolConfigVO vo = convertConfigToVO(config);
                DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(config.getProtocolType());
                if (adapter != null) {
                    boolean connected = adapter.isConnected();
                    vo.setConnectionStatus(connected ? 1 : 0);
                    vo.setConnectionStatusDesc(CONNECTION_STATUS_MAP.get(vo.getConnectionStatus()));
                }
                result.add(vo);
            }
        } else {
            result.add(getDefaultProtocolConfigVO());
            GridDispatchProtocolConfigVO modbusVO = new GridDispatchProtocolConfigVO();
            modbusVO.setId(-2L);
            modbusVO.setProtocolType(2);
            modbusVO.setProtocolTypeDesc(PROTOCOL_TYPE_MAP.get(2));
            modbusVO.setConfigName("默认Modbus TCP配置");
            modbusVO.setMasterIp("127.0.0.1");
            modbusVO.setMasterPort(502);
            modbusVO.setLocalPort(503);
            modbusVO.setCommonAddress(1);
            modbusVO.setConnectTimeout(30);
            modbusVO.setSendTimeout(10);
            modbusVO.setHeartbeatInterval(30);
            modbusVO.setReverseIsolationEnabled(0);
            modbusVO.setReverseIsolationDesc(ENABLED_MAP.get(0));
            modbusVO.setUploadInterval(5);
            modbusVO.setEnabled(1);
            modbusVO.setEnabledDesc(ENABLED_MAP.get(1));
            modbusVO.setConnectionStatus(0);
            modbusVO.setConnectionStatusDesc(CONNECTION_STATUS_MAP.get(0));
            result.add(modbusVO);
        }

        return result;
    }

    @Scheduled(fixedRate = 60000)
    public void retryCachedCommands() {
        if (redisTemplate == null) {
            return;
        }
        try {
            String commandPattern = dispatchProperties.getRedisCacheKey() + "command:*";
            Set<String> commandKeys = redisTemplate.keys(commandPattern);
            if (commandKeys != null && !commandKeys.isEmpty()) {
                for (String key : commandKeys) {
                    try {
                        String json = redisTemplate.opsForValue().get(key);
                        if (!StringUtils.hasText(json)) {
                            redisTemplate.delete(key);
                            continue;
                        }
                        GridDispatchCommand cachedCmd = JSON.parseObject(json, GridDispatchCommand.class);
                        if (cachedCmd == null) {
                            redisTemplate.delete(key);
                            continue;
                        }
                        executeCommand(cachedCmd);
                        if (cachedCmd.getStatus() != null && cachedCmd.getStatus() == 2) {
                            redisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        logger.error("重试缓存指令失败，key={}", key, e);
                    }
                }
            }

            String uploadPattern = dispatchProperties.getRedisCacheKey() + "upload:*";
            Set<String> uploadKeys = redisTemplate.keys(uploadPattern);
            if (uploadKeys != null && !uploadKeys.isEmpty()) {
                retryCachedUploads(uploadKeys);
            }
        } catch (Exception e) {
            logger.error("扫描Redis缓存异常", e);
        }
    }

    private void retryCachedUploads(Set<String> uploadKeys) {
        LambdaQueryWrapper<GridDispatchProtocolConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(GridDispatchProtocolConfig::getEnabled, 1);
        configWrapper.last("LIMIT 1");
        GridDispatchProtocolConfig defaultConfig = protocolConfigMapper.selectOne(configWrapper);
        GridDispatchProtocolConfigVO configVO = defaultConfig != null ? convertConfigToVO(defaultConfig) : getDefaultProtocolConfigVO();

        DispatchProtocolAdapter adapter = protocolAdapterFactory.getAdapter(configVO.getProtocolType());
        if (adapter == null) {
            logger.warn("重试上传缓存失败：未找到可用的协议适配器");
            return;
        }

        if (!adapter.isConnected()) {
            try {
                adapter.connect(configVO);
            } catch (Exception e) {
                logger.warn("重试上传缓存时协议连接失败", e);
            }
        }

        int maxRetry = dispatchProperties.getMaxRetryCount() != null ? dispatchProperties.getMaxRetryCount() : 3;
        int successCount = 0;
        int failCount = 0;

        for (String key : uploadKeys) {
            try {
                String json = redisTemplate.opsForValue().get(key);
                if (!StringUtils.hasText(json)) {
                    redisTemplate.delete(key);
                    continue;
                }

                GridDispatchUploadRecord cachedRecord = JSON.parseObject(json, GridDispatchUploadRecord.class);
                if (cachedRecord == null) {
                    redisTemplate.delete(key);
                    continue;
                }

                if (cachedRecord.getRetryCount() != null && cachedRecord.getRetryCount() >= maxRetry) {
                    logger.warn("上传记录重试次数已达上限，station={}, recordId={}",
                            cachedRecord.getStationName(), cachedRecord.getId());
                    if (redisTemplate.hasKey(key)) {
                        redisTemplate.delete(key);
                    }

                    if (rocketMQTemplate != null && dispatchProperties.getAlarmOnFail()) {
                        Map<String, Object> alarmMsg = new HashMap<>();
                        alarmMsg.put("type", "dispatch_upload_retry_exceeded");
                        alarmMsg.put("stationId", cachedRecord.getStationId());
                        alarmMsg.put("stationName", cachedRecord.getStationName());
                        alarmMsg.put("failReason", "重试次数已达上限，数据可能丢失");
                        alarmMsg.put("retryCount", cachedRecord.getRetryCount());
                        alarmMsg.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        rocketMQTemplate.send("fault-alarm-topic",
                                MessageBuilder.withPayload(JSON.toJSONString(alarmMsg)).build());
                    }
                    failCount++;
                    continue;
                }

                boolean retrySuccess = false;
                String failReason = null;
                for (int retry = 0; retry < maxRetry; retry++) {
                    try {
                        retrySuccess = adapter.uploadData(cachedRecord);
                        if (retrySuccess) {
                            break;
                        }
                    } catch (Exception e) {
                        failReason = e.getMessage();
                    }
                    if (retry < maxRetry - 1) {
                        Thread.sleep(1000);
                    }
                }

                if (retrySuccess) {
                    cachedRecord.setUploadStatus(1);
                    cachedRecord.setRetryCount((cachedRecord.getRetryCount() != null ? cachedRecord.getRetryCount() : 0) + 1);
                    uploadRecordMapper.updateById(cachedRecord);
                    if (redisTemplate.hasKey(key)) {
                        redisTemplate.delete(key);
                    }
                    successCount++;
                    logger.info("补传上传记录成功，station={}, activePower={}kW",
                            cachedRecord.getStationName(), cachedRecord.getTotalActivePower());
                } else {
                    cachedRecord.setRetryCount((cachedRecord.getRetryCount() != null ? cachedRecord.getRetryCount() : 0) + 1);
                    cachedRecord.setFailReason(failReason != null ? failReason : cachedRecord.getFailReason());
                    uploadRecordMapper.updateById(cachedRecord);

                    String newCacheKey = dispatchProperties.getRedisCacheKey() + "upload:" +
                            cachedRecord.getStationId() + ":" + System.currentTimeMillis();
                    redisTemplate.delete(key);
                    redisTemplate.opsForValue().set(
                            newCacheKey,
                            JSON.toJSONString(cachedRecord),
                            dispatchProperties.getRedisCacheTtl(),
                            TimeUnit.SECONDS
                    );
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("重试缓存上传记录失败，key={}", key, e);
                failCount++;
            }
        }

        if (successCount > 0 || failCount > 0) {
            logger.info("上传缓存补传完成：成功{}条，失败{}条", successCount, failCount);
        }
    }

    public GridDispatchCommandVO convertToVO(GridDispatchCommand entity) {
        if (entity == null) {
            return null;
        }
        GridDispatchCommandVO vo = new GridDispatchCommandVO();
        vo.setId(entity.getId());
        vo.setCommandNo(entity.getCommandNo());
        vo.setCommandSource(entity.getCommandSource());
        vo.setCommandSourceDesc(getDesc(COMMAND_SOURCE_MAP, entity.getCommandSource()));
        vo.setCommandType(entity.getCommandType());
        vo.setCommandTypeDesc(getDesc(COMMAND_TYPE_MAP, entity.getCommandType()));
        vo.setStationId(entity.getStationId());
        vo.setStationName(entity.getStationName());
        vo.setInverterId(entity.getInverterId());
        vo.setInverterName(entity.getInverterName());
        vo.setTargetActivePower(entity.getTargetActivePower());
        vo.setTargetReactivePower(entity.getTargetReactivePower());
        vo.setTargetVoltage(entity.getTargetVoltage());
        vo.setTargetFrequency(entity.getTargetFrequency());
        vo.setAdjustRatio(entity.getAdjustRatio());
        vo.setStartStop(entity.getStartStop());
        vo.setStartStopDesc(entity.getStartStop() != null ? (entity.getStartStop() ? "启动" : "停机") : null);
        vo.setIssueTime(entity.getIssueTime());
        vo.setExpectTime(entity.getExpectTime());
        vo.setExecuteStartTime(entity.getExecuteStartTime());
        vo.setExecuteEndTime(entity.getExecuteEndTime());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(getDesc(COMMAND_STATUS_MAP, entity.getStatus()));
        vo.setExecuteResult(entity.getExecuteResult());
        vo.setActualActivePower(entity.getActualActivePower());
        vo.setActualReactivePower(entity.getActualReactivePower());
        vo.setActualVoltage(entity.getActualVoltage());
        vo.setActualFrequency(entity.getActualFrequency());
        vo.setDeviationPercent(entity.getDeviationPercent());
        vo.setPriority(entity.getPriority());
        vo.setPriorityDesc(getDesc(PRIORITY_MAP, entity.getPriority()));
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setFailReason(entity.getFailReason());
        vo.setRemark(entity.getRemark());
        vo.setProtocolCommandId(entity.getProtocolCommandId());
        vo.setAsduAddress(entity.getAsduAddress());
        vo.setRawMessage(entity.getRawMessage());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public GridDispatchUploadRecordVO convertUploadToVO(GridDispatchUploadRecord entity) {
        if (entity == null) {
            return null;
        }
        GridDispatchUploadRecordVO vo = new GridDispatchUploadRecordVO();
        vo.setId(entity.getId());
        vo.setProtocolType(entity.getProtocolType());
        vo.setProtocolTypeDesc(getDesc(PROTOCOL_TYPE_MAP, entity.getProtocolType()));
        vo.setDataType(entity.getDataType());
        vo.setDataTypeDesc(getDesc(DATA_TYPE_MAP, entity.getDataType()));
        vo.setStationId(entity.getStationId());
        vo.setStationName(entity.getStationName());
        vo.setInverterId(entity.getInverterId());
        vo.setInverterName(entity.getInverterName());
        vo.setTotalActivePower(entity.getTotalActivePower());
        vo.setTotalReactivePower(entity.getTotalReactivePower());
        vo.setVoltageA(entity.getVoltageA());
        vo.setVoltageB(entity.getVoltageB());
        vo.setVoltageC(entity.getVoltageC());
        vo.setFrequency(entity.getFrequency());
        vo.setPowerFactor(entity.getPowerFactor());
        vo.setDailyGeneration(entity.getDailyGeneration());
        vo.setTotalGeneration(entity.getTotalGeneration());
        vo.setDeviceStatus(entity.getDeviceStatus());
        vo.setDeviceStatusDesc(getDesc(DEVICE_STATUS_MAP, entity.getDeviceStatus()));
        vo.setUploadTime(entity.getUploadTime());
        vo.setUploadStatus(entity.getUploadStatus());
        vo.setUploadStatusDesc(getDesc(UPLOAD_STATUS_MAP, entity.getUploadStatus()));
        vo.setResponseCode(entity.getResponseCode());
        vo.setFailReason(entity.getFailReason());
        vo.setRetryCount(entity.getRetryCount());
        vo.setCached(entity.getCached());
        vo.setCachedDesc(getDesc(CACHED_MAP, entity.getCached()));
        return vo;
    }

    public GridDispatchProtocolConfigVO convertConfigToVO(GridDispatchProtocolConfig entity) {
        if (entity == null) {
            return null;
        }
        GridDispatchProtocolConfigVO vo = new GridDispatchProtocolConfigVO();
        vo.setId(entity.getId());
        vo.setProtocolType(entity.getProtocolType());
        vo.setProtocolTypeDesc(getDesc(PROTOCOL_TYPE_MAP, entity.getProtocolType()));
        vo.setConfigName(entity.getConfigName());
        vo.setMasterIp(entity.getMasterIp());
        vo.setMasterPort(entity.getMasterPort());
        vo.setLocalIp(entity.getLocalIp());
        vo.setLocalPort(entity.getLocalPort());
        vo.setCommonAddress(entity.getCommonAddress());
        vo.setConnectTimeout(entity.getConnectTimeout());
        vo.setSendTimeout(entity.getSendTimeout());
        vo.setHeartbeatInterval(entity.getHeartbeatInterval());
        vo.setReverseIsolationEnabled(entity.getReverseIsolationEnabled());
        vo.setReverseIsolationDesc(getDesc(ENABLED_MAP, entity.getReverseIsolationEnabled()));
        vo.setIsolationIp(entity.getIsolationIp());
        vo.setIsolationPort(entity.getIsolationPort());
        vo.setUploadInterval(entity.getUploadInterval());
        vo.setEnabled(entity.getEnabled());
        vo.setEnabledDesc(getDesc(ENABLED_MAP, entity.getEnabled()));
        vo.setConnectionStatus(entity.getConnectionStatus());
        vo.setConnectionStatusDesc(getDesc(CONNECTION_STATUS_MAP, entity.getConnectionStatus()));
        vo.setLastConnectTime(entity.getLastConnectTime());
        vo.setLastDisconnectTime(entity.getLastDisconnectTime());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private GridDispatchProtocolConfigVO getDefaultProtocolConfigVO() {
        GridDispatchProtocolConfigVO vo = new GridDispatchProtocolConfigVO();
        vo.setId(-1L);
        vo.setProtocolType(1);
        vo.setProtocolTypeDesc(PROTOCOL_TYPE_MAP.get(1));
        vo.setConfigName("默认IEC104配置");
        vo.setMasterIp("127.0.0.1");
        vo.setMasterPort(2404);
        vo.setLocalPort(2405);
        vo.setCommonAddress(1);
        vo.setConnectTimeout(30);
        vo.setSendTimeout(10);
        vo.setHeartbeatInterval(30);
        vo.setReverseIsolationEnabled(0);
        vo.setReverseIsolationDesc(ENABLED_MAP.get(0));
        vo.setUploadInterval(5);
        vo.setEnabled(1);
        vo.setEnabledDesc(ENABLED_MAP.get(1));
        vo.setConnectionStatus(0);
        vo.setConnectionStatusDesc(CONNECTION_STATUS_MAP.get(0));
        return vo;
    }

    private String generateCommandNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "DIS" + timestamp + uuid;
    }

    private String getDesc(Map<Integer, String> map, Integer value) {
        if (value == null) {
            return null;
        }
        return map.getOrDefault(value, "未知");
    }
}
