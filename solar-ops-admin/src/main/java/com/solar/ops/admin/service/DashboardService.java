package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.vo.*;
import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.entity.RevenueStatistics;
import com.solar.ops.analysis.entity.StationHealth;
import com.solar.ops.analysis.mapper.EfficiencyStatisticsMapper;
import com.solar.ops.analysis.mapper.RevenueStatisticsMapper;
import com.solar.ops.analysis.mapper.StationHealthMapper;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Resource
    private StationMapper stationMapper;

    @Resource
    private InverterMapper inverterMapper;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private StationHealthMapper stationHealthMapper;

    @Resource
    private EfficiencyStatisticsMapper efficiencyStatisticsMapper;

    @Resource
    private RevenueStatisticsMapper revenueStatisticsMapper;

    @Autowired(required = false)
    private DashboardInfluxDBService influxDBService;

    private static final BigDecimal EMISSION_FACTOR = new BigDecimal("0.98");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:00");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    public DashboardRealTimeVO getRealTimeDashboard() {
        DashboardRealTimeVO vo = new DashboardRealTimeVO();
        LocalDateTime now = LocalDateTime.now();

        List<Station> stations = stationMapper.selectList(new LambdaQueryWrapper<Station>()
                .eq(Station::getStatus, 1));
        List<Inverter> inverters = inverterMapper.selectList(new LambdaQueryWrapper<Inverter>()
                .eq(Inverter::getStatus, 1));

        int stationCount = stations.size();
        int inverterCount = inverters.size();
        int onlineCount = (int) inverters.stream().filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1).count();
        int offlineCount = inverterCount - onlineCount;
        BigDecimal onlineRate = inverterCount > 0
                ? BigDecimal.valueOf(onlineCount).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(inverterCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalPower = calculateTotalPower(inverters);
        BigDecimal todayGeneration = calculateTodayGeneration();
        BigDecimal totalGeneration = calculateTotalGeneration();
        BigDecimal totalEmissionReduction = totalGeneration.multiply(EMISSION_FACTOR)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        int alarmCount = calculateAlarmCount();
        int unhandledWorkOrderCount = calculateUnhandledWorkOrderCount();

        List<StationMapVO> stationMapList = buildStationMapList(stations, inverters);
        List<PowerTrendVO> powerTrend = generatePowerTrend();
        List<GenerationTrendVO> generationTrend = generateGenerationTrend();

        vo.setTotalPower(totalPower);
        vo.setTodayGeneration(todayGeneration);
        vo.setTotalGeneration(totalGeneration);
        vo.setTotalEmissionReduction(totalEmissionReduction);
        vo.setOnlineRate(onlineRate);
        vo.setOnlineCount(onlineCount);
        vo.setOfflineCount(offlineCount);
        vo.setAlarmCount(alarmCount);
        vo.setUnhandledWorkOrderCount(unhandledWorkOrderCount);
        vo.setStationCount(stationCount);
        vo.setInverterCount(inverterCount);
        vo.setUpdateTime(now);
        vo.setStationMapList(stationMapList);
        vo.setPowerTrend(powerTrend);
        vo.setGenerationTrend(generationTrend);

        return vo;
    }

    public List<InverterMonitorVO> getInverterMonitorByStation(Long stationId) {
        List<Inverter> inverters = inverterMapper.selectList(new LambdaQueryWrapper<Inverter>()
                .eq(Inverter::getStationId, stationId)
                .eq(Inverter::getStatus, 1));

        return inverters.stream().map(this::convertToInverterMonitorVO).collect(Collectors.toList());
    }

    public MobileDashboardVO getMobileDashboard() {
        MobileDashboardVO vo = new MobileDashboardVO();
        LocalDateTime now = LocalDateTime.now();

        List<Station> stations = stationMapper.selectList(new LambdaQueryWrapper<Station>()
                .eq(Station::getStatus, 1));
        List<Inverter> inverters = inverterMapper.selectList(new LambdaQueryWrapper<Inverter>()
                .eq(Inverter::getStatus, 1));

        int inverterCount = inverters.size();
        int onlineCount = (int) inverters.stream().filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1).count();
        BigDecimal onlineRate = inverterCount > 0
                ? BigDecimal.valueOf(onlineCount).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(inverterCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalPower = calculateTotalPower(inverters);
        BigDecimal todayGeneration = calculateTodayGeneration();
        BigDecimal totalEmissionReduction = calculateTotalGeneration()
                .multiply(EMISSION_FACTOR).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        int alarmCount = calculateAlarmCount();
        int unhandledWorkOrderCount = calculateUnhandledWorkOrderCount();

        List<StationHealthStatVO> healthStats = buildStationHealthStats(stations);
        List<StationAlarmVO> alarmStations = buildAlarmStations();

        vo.setTotalPower(totalPower);
        vo.setTodayGeneration(todayGeneration);
        vo.setTotalEmissionReduction(totalEmissionReduction);
        vo.setOnlineRate(onlineRate);
        vo.setAlarmCount(alarmCount);
        vo.setUnhandledWorkOrderCount(unhandledWorkOrderCount);
        vo.setPowerTrend(totalPower.compareTo(BigDecimal.ZERO) > 0 ? "up" : "neutral");
        vo.setPowerChangePercent(calculatePowerChangePercent());
        vo.setGenerationTrend(todayGeneration.compareTo(BigDecimal.ZERO) > 0 ? "up" : "neutral");
        vo.setGenerationChangePercent(calculateGenerationChangePercent());
        vo.setStationHealthStats(healthStats);
        vo.setAlarmStations(alarmStations);
        vo.setUpdateTime(now);

        return vo;
    }

    private BigDecimal calculateTotalPower(List<Inverter> inverters) {
        if (influxDBService != null) {
            List<String> onlineDeviceSnList = inverters.stream()
                    .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                    .map(Inverter::getDeviceSn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!onlineDeviceSnList.isEmpty()) {
                Map<String, BigDecimal> powerMap = influxDBService.queryAllRealtimePower(onlineDeviceSnList);
                if (!powerMap.isEmpty()) {
                    return powerMap.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        return inverters.stream()
                .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                .map(i -> {
                    BigDecimal base = i.getRatedPower() != null ? i.getRatedPower() : BigDecimal.ZERO;
                    return base.multiply(new BigDecimal("0.8"));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTodayGeneration() {
        LocalDate today = LocalDate.now();
        List<EfficiencyStatistics> stats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .eq(EfficiencyStatistics::getStatisticsDate, today)
                        .eq(EfficiencyStatistics::getStatisticsType, 1));

        return stats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalGeneration() {
        List<EfficiencyStatistics> stats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .eq(EfficiencyStatistics::getStatisticsType, 1));

        return stats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateAlarmCount() {
        Long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .ge(WorkOrder::getFaultLevel, 3)
                .lt(WorkOrder::getStatus, 4));
        return count != null ? count.intValue() : 0;
    }

    private int calculateUnhandledWorkOrderCount() {
        Long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, 0, 1, 2));
        return count != null ? count.intValue() : 0;
    }

    private List<StationMapVO> buildStationMapList(List<Station> stations, List<Inverter> inverters) {
        Map<Long, List<Inverter>> stationInverterMap = inverters.stream()
                .collect(Collectors.groupingBy(Inverter::getStationId));

        List<Long> stationIds = stations.stream().map(Station::getId).collect(Collectors.toList());

        Map<Long, StationHealth> healthMap = stationHealthMapper.selectList(
                        new LambdaQueryWrapper<StationHealth>()
                                .in(!stationIds.isEmpty(), StationHealth::getStationId, stationIds)
                                .orderByDesc(StationHealth::getAssessmentTime))
                .stream()
                .collect(Collectors.toMap(StationHealth::getStationId, h -> h, (existing, replacement) -> existing));

        Map<Long, Long> alarmCountMap = workOrderMapper.selectList(
                        new LambdaQueryWrapper<WorkOrder>()
                                .ge(WorkOrder::getFaultLevel, 3)
                                .lt(WorkOrder::getStatus, 4)
                                .in(!stationIds.isEmpty(), WorkOrder::getStationId, stationIds))
                .stream()
                .collect(Collectors.groupingBy(WorkOrder::getStationId, Collectors.counting()));

        Map<Long, Long> unhandledOrderMap = workOrderMapper.selectList(
                        new LambdaQueryWrapper<WorkOrder>()
                                .in(WorkOrder::getStatus, 0, 1, 2)
                                .in(!stationIds.isEmpty(), WorkOrder::getStationId, stationIds))
                .stream()
                .collect(Collectors.groupingBy(WorkOrder::getStationId, Collectors.counting()));

        Map<Long, BigDecimal> stationTodayGenerationMap = calculateStationTodayGenerationMap(stationIds);

        Map<String, BigDecimal> influxPowerMap = null;
        if (influxDBService != null) {
            List<String> allDeviceSnList = inverters.stream()
                    .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                    .map(Inverter::getDeviceSn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!allDeviceSnList.isEmpty()) {
                influxPowerMap = influxDBService.queryAllRealtimePower(allDeviceSnList);
            }
        }

        Map<String, BigDecimal> finalInfluxPowerMap = influxPowerMap;

        return stations.stream().map(station -> {
            StationMapVO vo = new StationMapVO();
            List<Inverter> stationInverters = stationInverterMap.getOrDefault(station.getId(), Collections.emptyList());
            int invCount = stationInverters.size();
            int onlineInvCount = (int) stationInverters.stream()
                    .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1).count();

            StationHealth health = healthMap.get(station.getId());
            int healthLevel = health != null && health.getHealthLevel() != null ? health.getHealthLevel() : 1;
            String healthColor = healthLevel == 1 ? "green" : healthLevel == 2 ? "yellow" : "red";
            BigDecimal healthScore = health != null && health.getEfficiencyScore() != null
                    ? health.getEfficiencyScore()
                    : BigDecimal.valueOf(80);

            BigDecimal currentPower;
            if (finalInfluxPowerMap != null) {
                currentPower = stationInverters.stream()
                        .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                        .map(i -> {
                            BigDecimal p = i.getDeviceSn() != null ? finalInfluxPowerMap.get(i.getDeviceSn()) : null;
                            return p != null ? p : BigDecimal.ZERO;
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                currentPower = stationInverters.stream()
                        .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                        .map(i -> {
                            BigDecimal base = i.getRatedPower() != null ? i.getRatedPower() : BigDecimal.ZERO;
                            return base.multiply(new BigDecimal("0.8"));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            vo.setStationId(station.getId());
            vo.setStationName(station.getStationName());
            vo.setStationCode(station.getStationCode());
            vo.setCapacity(station.getCapacity());
            vo.setCurrentPower(currentPower);
            vo.setTodayGeneration(stationTodayGenerationMap.getOrDefault(station.getId(), BigDecimal.ZERO));
            vo.setLongitude(station.getLongitude());
            vo.setLatitude(station.getLatitude());
            vo.setAddress(station.getAddress());
            vo.setHealthLevel(healthLevel);
            vo.setHealthColor(healthColor);
            vo.setHealthScore(healthScore);
            vo.setOnlineStatus(onlineInvCount > 0 ? 1 : 0);
            vo.setInverterCount(invCount);
            vo.setOnlineInverterCount(onlineInvCount);
            vo.setAlarmCount(alarmCountMap.getOrDefault(station.getId(), 0L).intValue());
            vo.setUnhandledOrderCount(unhandledOrderMap.getOrDefault(station.getId(), 0L).intValue());

            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> calculateStationTodayGenerationMap(List<Long> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDate today = LocalDate.now();
        List<EfficiencyStatistics> stats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .in(EfficiencyStatistics::getStationId, stationIds)
                        .eq(EfficiencyStatistics::getStatisticsDate, today)
                        .eq(EfficiencyStatistics::getStatisticsType, 1));

        return stats.stream()
                .collect(Collectors.groupingBy(
                        EfficiencyStatistics::getStationId,
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO,
                                BigDecimal::add)));
    }

    private InverterMonitorVO convertToInverterMonitorVO(Inverter inverter) {
        InverterMonitorVO vo = new InverterMonitorVO();
        boolean isOnline = inverter.getOnlineStatus() != null && inverter.getOnlineStatus() == 1;
        BigDecimal ratedPower = inverter.getRatedPower() != null ? inverter.getRatedPower() : BigDecimal.ZERO;

        Map<String, BigDecimal> influxData = null;
        if (influxDBService != null && isOnline && inverter.getDeviceSn() != null) {
            influxData = influxDBService.queryRealtimeData(inverter.getDeviceSn());
        }

        BigDecimal currentPower;
        BigDecimal voltage;
        BigDecimal currentVal;
        BigDecimal temperature;
        BigDecimal efficiency;
        BigDecimal dayGeneration;

        if (influxData != null && !influxData.isEmpty()) {
            currentPower = influxData.getOrDefault("power", BigDecimal.ZERO);
            voltage = influxData.getOrDefault("voltage", BigDecimal.ZERO);
            currentVal = influxData.getOrDefault("current", BigDecimal.ZERO);
            temperature = influxData.getOrDefault("temperature", BigDecimal.ZERO);
            dayGeneration = influxData.getOrDefault("energy", BigDecimal.ZERO);
            efficiency = ratedPower.compareTo(BigDecimal.ZERO) > 0
                    ? currentPower.multiply(BigDecimal.valueOf(100))
                        .divide(ratedPower, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } else {
            currentPower = isOnline ? ratedPower.multiply(new BigDecimal("0.8")) : BigDecimal.ZERO;
            voltage = BigDecimal.ZERO;
            currentVal = BigDecimal.ZERO;
            temperature = BigDecimal.ZERO;
            efficiency = BigDecimal.ZERO;
            dayGeneration = BigDecimal.ZERO;
        }

        int healthLevel = 1;
        if (inverter.getStationId() != null) {
            StationHealth health = stationHealthMapper.selectOne(
                    new LambdaQueryWrapper<StationHealth>()
                            .eq(StationHealth::getStationId, inverter.getStationId())
                            .orderByDesc(StationHealth::getAssessmentTime)
                            .last("LIMIT 1"));
            if (health != null && health.getHealthLevel() != null) {
                healthLevel = health.getHealthLevel();
            }
        }
        String healthColor = healthLevel == 1 ? "green" : healthLevel == 2 ? "yellow" : "red";

        vo.setId(inverter.getId());
        vo.setDeviceSn(inverter.getDeviceSn());
        vo.setDeviceName(inverter.getDeviceName());
        vo.setDeviceModel(inverter.getDeviceModel());
        vo.setRatedPower(ratedPower);
        vo.setCurrentPower(currentPower.setScale(2, RoundingMode.HALF_UP));
        vo.setDayGeneration(dayGeneration.setScale(2, RoundingMode.HALF_UP));
        vo.setVoltage(voltage.setScale(1, RoundingMode.HALF_UP));
        vo.setCurrent(currentVal.setScale(1, RoundingMode.HALF_UP));
        vo.setTemperature(temperature.setScale(1, RoundingMode.HALF_UP));
        vo.setEfficiency(efficiency.setScale(1, RoundingMode.HALF_UP));
        vo.setRunHours(BigDecimal.ZERO);
        vo.setLongitude(inverter.getLongitude());
        vo.setLatitude(inverter.getLatitude());
        vo.setInstallLocation(inverter.getInstallLocation());
        vo.setOnlineStatus(inverter.getOnlineStatus());
        vo.setHealthLevel(healthLevel);
        vo.setHealthColor(healthColor);
        vo.setLastOnlineTime(inverter.getLastOnlineTime());
        vo.setUpdateTime(LocalDateTime.now());

        return vo;
    }

    private List<PowerTrendVO> generatePowerTrend() {
        if (influxDBService != null) {
            List<Map<String, Object>> influxTrend = influxDBService.query24hPowerTrend();
            if (!influxTrend.isEmpty()) {
                return influxTrend.stream().map(point -> {
                    PowerTrendVO vo = new PowerTrendVO();
                    vo.setTime(point.get("time") != null ? point.get("time").toString() : "");
                    vo.setPower(point.get("power") != null ? (BigDecimal) point.get("power") : BigDecimal.ZERO);
                    return vo;
                }).collect(Collectors.toList());
            }
        }

        LocalDate today = LocalDate.now();
        List<EfficiencyStatistics> stats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .eq(EfficiencyStatistics::getStatisticsDate, today)
                        .eq(EfficiencyStatistics::getStatisticsType, 1));

        BigDecimal todayTotalEnergy = stats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PowerTrendVO> trend = new ArrayList<>();
        int currentHour = LocalDateTime.now().getHour();
        for (int i = 0; i <= currentHour; i++) {
            PowerTrendVO vo = new PowerTrendVO();
            vo.setTime(String.format("%02d:00", i));
            double factor = i >= 6 && i <= 18
                    ? Math.sin(Math.PI * (i - 6) / 12.0)
                    : 0.0;
            vo.setPower(todayTotalEnergy.compareTo(BigDecimal.ZERO) > 0
                    ? todayTotalEnergy.multiply(BigDecimal.valueOf(factor))
                        .divide(BigDecimal.valueOf(currentHour > 0 ? currentHour : 1), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            trend.add(vo);
        }
        return trend;
    }

    private List<GenerationTrendVO> generateGenerationTrend() {
        if (influxDBService != null) {
            List<Map<String, Object>> influxTrend = influxDBService.query7dGenerationTrend();
            if (!influxTrend.isEmpty()) {
                return influxTrend.stream().map(point -> {
                    GenerationTrendVO vo = new GenerationTrendVO();
                    vo.setDate(point.get("date") != null ? point.get("date").toString() : "");
                    vo.setGeneration(point.get("generation") != null ? (BigDecimal) point.get("generation") : BigDecimal.ZERO);
                    return vo;
                }).collect(Collectors.toList());
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        List<RevenueStatistics> stats = revenueStatisticsMapper.selectList(
                new LambdaQueryWrapper<RevenueStatistics>()
                        .between(RevenueStatistics::getStatisticsDate, sevenDaysAgo, today)
                        .eq(RevenueStatistics::getStatisticsType, 1));

        Map<LocalDate, BigDecimal> generationMap = stats.stream()
                .collect(Collectors.groupingBy(
                        RevenueStatistics::getStatisticsDate,
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO,
                                BigDecimal::add)));

        List<GenerationTrendVO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            GenerationTrendVO vo = new GenerationTrendVO();
            vo.setDate(date.format(DATE_FORMATTER));
            vo.setGeneration(generationMap.getOrDefault(date, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP));
            trend.add(vo);
        }
        return trend;
    }

    private List<StationHealthStatVO> buildStationHealthStats(List<Station> stations) {
        int total = stations.size();
        if (total == 0) return Collections.emptyList();

        List<Long> stationIds = stations.stream().map(Station::getId).collect(Collectors.toList());
        List<StationHealth> healthList = stationHealthMapper.selectList(
                new LambdaQueryWrapper<StationHealth>()
                        .in(StationHealth::getStationId, stationIds)
                        .orderByDesc(StationHealth::getAssessmentTime));

        Map<Long, StationHealth> latestHealthMap = healthList.stream()
                .collect(Collectors.toMap(StationHealth::getStationId, h -> h, (existing, replacement) -> existing));

        Map<Integer, Long> healthCountMap = latestHealthMap.values().stream()
                .filter(h -> h.getHealthLevel() != null)
                .collect(Collectors.groupingBy(StationHealth::getHealthLevel, Collectors.counting()));

        long countedCount = healthCountMap.values().stream().mapToLong(Long::longValue).sum();
        long defaultCount = total - countedCount;
        if (defaultCount > 0) {
            healthCountMap.merge(1, defaultCount, Long::sum);
        }

        List<StationHealthStatVO> stats = new ArrayList<>();
        for (int level = 1; level <= 3; level++) {
            StationHealthStatVO vo = new StationHealthStatVO();
            int count = healthCountMap.getOrDefault(level, 0L).intValue();
            vo.setHealthLevel(level);
            vo.setHealthColor(level == 1 ? "green" : level == 2 ? "yellow" : "red");
            vo.setHealthLevelDesc(level == 1 ? "优秀" : level == 2 ? "良好" : "异常");
            vo.setCount(count);
            vo.setPercentage(BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            stats.add(vo);
        }
        return stats;
    }

    private List<StationAlarmVO> buildAlarmStations() {
        List<WorkOrder> alarmOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>()
                        .ge(WorkOrder::getFaultLevel, 3)
                        .lt(WorkOrder::getStatus, 4));

        if (alarmOrders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> alarmCountMap = alarmOrders.stream()
                .filter(o -> o.getStationId() != null)
                .collect(Collectors.groupingBy(WorkOrder::getStationId, Collectors.counting()));

        Map<Long, Integer> maxAlarmLevelMap = alarmOrders.stream()
                .filter(o -> o.getStationId() != null)
                .collect(Collectors.groupingBy(
                        WorkOrder::getStationId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(WorkOrder::getFaultLevel)),
                                opt -> opt.map(WorkOrder::getFaultLevel).orElse(1))));

        List<Long> stationIds = new ArrayList<>(alarmCountMap.keySet());
        if (stationIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = stationMapper.selectList(
                new LambdaQueryWrapper<Station>().in(Station::getId, stationIds));
        Map<Long, Station> stationMap = stations.stream()
                .collect(Collectors.toMap(Station::getId, s -> s));

        Map<Long, StationHealth> healthMap = stationHealthMapper.selectList(
                        new LambdaQueryWrapper<StationHealth>()
                                .in(StationHealth::getStationId, stationIds)
                                .orderByDesc(StationHealth::getAssessmentTime))
                .stream()
                .collect(Collectors.toMap(StationHealth::getStationId, h -> h, (existing, replacement) -> existing));

        return alarmCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Long stationId = entry.getKey();
                    Station station = stationMap.get(stationId);
                    if (station == null) return null;

                    StationAlarmVO vo = new StationAlarmVO();
                    vo.setStationId(stationId);
                    vo.setStationName(station.getStationName());
                    vo.setAlarmCount(entry.getValue().intValue());
                    vo.setMaxAlarmLevel(maxAlarmLevelMap.getOrDefault(stationId, 1));

                    StationHealth health = healthMap.get(stationId);
                    int healthLevel = health != null && health.getHealthLevel() != null ? health.getHealthLevel() : 2;
                    vo.setHealthColor(healthLevel == 1 ? "green" : healthLevel == 2 ? "yellow" : "red");
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BigDecimal calculatePowerChangePercent() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<EfficiencyStatistics> todayStats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .eq(EfficiencyStatistics::getStatisticsDate, today)
                        .eq(EfficiencyStatistics::getStatisticsType, 1));
        BigDecimal todayEnergy = todayStats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<EfficiencyStatistics> yesterdayStats = efficiencyStatisticsMapper.selectList(
                new LambdaQueryWrapper<EfficiencyStatistics>()
                        .eq(EfficiencyStatistics::getStatisticsDate, yesterday)
                        .eq(EfficiencyStatistics::getStatisticsType, 1));
        BigDecimal yesterdayEnergy = yesterdayStats.stream()
                .map(s -> s.getTotalEnergy() != null ? s.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (yesterdayEnergy.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return todayEnergy.subtract(yesterdayEnergy)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterdayEnergy, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGenerationChangePercent() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<RevenueStatistics> todayStats = revenueStatisticsMapper.selectList(
                new LambdaQueryWrapper<RevenueStatistics>()
                        .eq(RevenueStatistics::getStatisticsDate, today)
                        .eq(RevenueStatistics::getStatisticsType, 1));
        BigDecimal todayEnergy = todayStats.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueStatistics> yesterdayStats = revenueStatisticsMapper.selectList(
                new LambdaQueryWrapper<RevenueStatistics>()
                        .eq(RevenueStatistics::getStatisticsDate, yesterday)
                        .eq(RevenueStatistics::getStatisticsType, 1));
        BigDecimal yesterdayEnergy = yesterdayStats.stream()
                .map(s -> s.getGridEnergy() != null ? s.getGridEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (yesterdayEnergy.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return todayEnergy.subtract(yesterdayEnergy)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterdayEnergy, 2, RoundingMode.HALF_UP);
    }
}
