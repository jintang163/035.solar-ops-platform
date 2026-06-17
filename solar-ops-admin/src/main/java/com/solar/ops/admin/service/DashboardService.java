package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.admin.vo.*;
import com.solar.ops.analysis.entity.StationHealth;
import com.solar.ops.analysis.mapper.StationHealthMapper;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
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
        BigDecimal todayGeneration = calculateTodayGeneration(stations);
        BigDecimal totalGeneration = calculateTotalGeneration(stations);
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
        BigDecimal todayGeneration = calculateTodayGeneration(stations);
        BigDecimal totalEmissionReduction = calculateTotalGeneration(stations)
                .multiply(EMISSION_FACTOR).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        int alarmCount = calculateAlarmCount();
        int unhandledWorkOrderCount = calculateUnhandledWorkOrderCount();

        List<StationHealthStatVO> healthStats = buildStationHealthStats(stations);
        List<StationAlarmVO> alarmStations = buildAlarmStations(stations);

        vo.setTotalPower(totalPower);
        vo.setTodayGeneration(todayGeneration);
        vo.setTotalEmissionReduction(totalEmissionReduction);
        vo.setOnlineRate(onlineRate);
        vo.setAlarmCount(alarmCount);
        vo.setUnhandledWorkOrderCount(unhandledWorkOrderCount);
        vo.setPowerTrend(totalPower.compareTo(BigDecimal.ZERO) > 0 ? "up" : "neutral");
        vo.setPowerChangePercent(new BigDecimal("5.2"));
        vo.setGenerationTrend("up");
        vo.setGenerationChangePercent(new BigDecimal("8.3"));
        vo.setStationHealthStats(healthStats);
        vo.setAlarmStations(alarmStations);
        vo.setUpdateTime(now);

        return vo;
    }

    private BigDecimal calculateTotalPower(List<Inverter> inverters) {
        Random random = new Random();
        return inverters.stream()
                .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                .map(i -> {
                    BigDecimal base = i.getRatedPower() != null ? i.getRatedPower() : BigDecimal.ZERO;
                    double factor = 0.7 + random.nextDouble() * 0.25;
                    return base.multiply(BigDecimal.valueOf(factor));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTodayGeneration(List<Station> stations) {
        Random random = new Random();
        return stations.stream()
                .map(s -> {
                    BigDecimal base = s.getCapacity() != null ? s.getCapacity() : BigDecimal.ZERO;
                    double factor = 3.5 + random.nextDouble() * 2;
                    return base.multiply(BigDecimal.valueOf(factor));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalGeneration(List<Station> stations) {
        return stations.stream()
                .map(s -> {
                    BigDecimal capacity = s.getCapacity() != null ? s.getCapacity() : BigDecimal.ZERO;
                    return capacity.multiply(BigDecimal.valueOf(1200));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateAlarmCount() {
        Random random = new Random();
        return 5 + random.nextInt(15);
    }

    private int calculateUnhandledWorkOrderCount() {
        Long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, 0, 1, 2));
        return count != null ? count.intValue() : 0;
    }

    private List<StationMapVO> buildStationMapList(List<Station> stations, List<Inverter> inverters) {
        Map<Long, List<Inverter>> stationInverterMap = inverters.stream()
                .collect(Collectors.groupingBy(Inverter::getStationId));

        Map<Long, StationHealth> healthMap = stationHealthMapper.selectList(
                        new LambdaQueryWrapper<StationHealth>()
                                .in(StationHealth::getStationId, stations.stream().map(Station::getId).collect(Collectors.toList()))
                                .orderByDesc(StationHealth::getAssessmentTime))
                .stream()
                .collect(Collectors.toMap(StationHealth::getStationId, h -> h, (existing, replacement) -> existing));

        Random random = new Random();

        return stations.stream().map(station -> {
            StationMapVO vo = new StationMapVO();
            List<Inverter> stationInverters = stationInverterMap.getOrDefault(station.getId(), Collections.emptyList());
            int invCount = stationInverters.size();
            int onlineInvCount = (int) stationInverters.stream()
                    .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1).count();

            StationHealth health = healthMap.get(station.getId());
            int healthLevel = health != null && health.getHealthLevel() != null ? health.getHealthLevel() : 1;
            String healthColor = healthLevel == 1 ? "green" : healthLevel == 2 ? "yellow" : "red";
            BigDecimal healthScore = health != null && health.getHealthScore() != null
                    ? health.getHealthScore()
                    : BigDecimal.valueOf(80 + random.nextInt(20));

            vo.setStationId(station.getId());
            vo.setStationName(station.getStationName());
            vo.setStationCode(station.getStationCode());
            vo.setCapacity(station.getCapacity());
            vo.setCurrentPower(calculateStationPower(stationInverters));
            vo.setTodayGeneration(calculateStationTodayGeneration(station));
            vo.setLongitude(station.getLongitude());
            vo.setLatitude(station.getLatitude());
            vo.setAddress(station.getAddress());
            vo.setHealthLevel(healthLevel);
            vo.setHealthColor(healthColor);
            vo.setHealthScore(healthScore);
            vo.setOnlineStatus(onlineInvCount > 0 ? 1 : 0);
            vo.setInverterCount(invCount);
            vo.setOnlineInverterCount(onlineInvCount);
            vo.setAlarmCount(random.nextInt(5));
            vo.setUnhandledOrderCount(random.nextInt(3));

            return vo;
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateStationPower(List<Inverter> inverters) {
        Random random = new Random();
        return inverters.stream()
                .filter(i -> i.getOnlineStatus() != null && i.getOnlineStatus() == 1)
                .map(i -> {
                    BigDecimal base = i.getRatedPower() != null ? i.getRatedPower() : BigDecimal.ZERO;
                    double factor = 0.7 + random.nextDouble() * 0.25;
                    return base.multiply(BigDecimal.valueOf(factor));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStationTodayGeneration(Station station) {
        Random random = new Random();
        BigDecimal capacity = station.getCapacity() != null ? station.getCapacity() : BigDecimal.ZERO;
        double factor = 3.5 + random.nextDouble() * 2;
        return capacity.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    private InverterMonitorVO convertToInverterMonitorVO(Inverter inverter) {
        InverterMonitorVO vo = new InverterMonitorVO();
        Random random = new Random();

        boolean isOnline = inverter.getOnlineStatus() != null && inverter.getOnlineStatus() == 1;
        BigDecimal ratedPower = inverter.getRatedPower() != null ? inverter.getRatedPower() : BigDecimal.ZERO;
        BigDecimal currentPower = isOnline
                ? ratedPower.multiply(BigDecimal.valueOf(0.7 + random.nextDouble() * 0.25))
                : BigDecimal.ZERO;

        int healthLevel = 1 + random.nextInt(3);
        String healthColor = healthLevel == 1 ? "green" : healthLevel == 2 ? "yellow" : "red";

        vo.setId(inverter.getId());
        vo.setDeviceSn(inverter.getDeviceSn());
        vo.setDeviceName(inverter.getDeviceName());
        vo.setDeviceModel(inverter.getDeviceModel());
        vo.setRatedPower(ratedPower);
        vo.setCurrentPower(currentPower.setScale(2, RoundingMode.HALF_UP));
        vo.setDayGeneration(currentPower.multiply(BigDecimal.valueOf(4 + random.nextDouble())).setScale(2, RoundingMode.HALF_UP));
        vo.setVoltage(isOnline ? BigDecimal.valueOf(380 + random.nextDouble() * 20).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setCurrent(isOnline ? BigDecimal.valueOf(10 + random.nextDouble() * 50).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setTemperature(isOnline ? BigDecimal.valueOf(35 + random.nextDouble() * 20).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setEfficiency(isOnline ? BigDecimal.valueOf(92 + random.nextDouble() * 6).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setRunHours(BigDecimal.valueOf(2000 + random.nextInt(5000)));
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
        List<PowerTrendVO> trend = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 23; i >= 0; i--) {
            LocalDateTime time = now.minusHours(i);
            int hour = time.getHour();
            double factor = hour >= 6 && hour <= 18
                    ? Math.sin(Math.PI * (hour - 6) / 12) * 0.9 + 0.1
                    : 0.02 + random.nextDouble() * 0.03;

            PowerTrendVO vo = new PowerTrendVO();
            vo.setTime(time.format(HOUR_FORMATTER));
            vo.setPower(BigDecimal.valueOf(8000 * factor + random.nextDouble() * 500).setScale(2, RoundingMode.HALF_UP));
            trend.add(vo);
        }
        return trend;
    }

    private List<GenerationTrendVO> generateGenerationTrend() {
        List<GenerationTrendVO> trend = new ArrayList<>();
        Random random = new Random();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            GenerationTrendVO vo = new GenerationTrendVO();
            vo.setDate(date.format(DATE_FORMATTER));
            vo.setGeneration(BigDecimal.valueOf(25000 + random.nextDouble() * 15000).setScale(2, RoundingMode.HALF_UP));
            trend.add(vo);
        }
        return trend;
    }

    private List<StationHealthStatVO> buildStationHealthStats(List<Station> stations) {
        int total = stations.size();
        if (total == 0) return Collections.emptyList();

        Map<Integer, Long> healthCountMap = new HashMap<>();
        healthCountMap.put(1, (long) (total * 0.6));
        healthCountMap.put(2, (long) (total * 0.3));
        healthCountMap.put(3, (long) (total * 0.1));

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

    private List<StationAlarmVO> buildAlarmStations(List<Station> stations) {
        Random random = new Random();
        List<StationAlarmVO> result = new ArrayList<>();

        for (int i = 0; i < Math.min(5, stations.size()); i++) {
            Station station = stations.get(i);
            StationAlarmVO vo = new StationAlarmVO();
            vo.setStationId(station.getId());
            vo.setStationName(station.getStationName());
            vo.setAlarmCount(1 + random.nextInt(4));
            vo.setMaxAlarmLevel(1 + random.nextInt(4));
            vo.setHealthColor(random.nextBoolean() ? "yellow" : "red");
            result.add(vo);
        }
        return result;
    }
}
