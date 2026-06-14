package com.solar.ops.prediction.service;

import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.prediction.entity.PowerPrediction;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionInfluxDBService {

    private static final Logger log = LoggerFactory.getLogger(PredictionInfluxDBService.class);
    private static final String MEASUREMENT = "inverter_data";

    @Autowired
    private InfluxDB influxDB;

    @Autowired
    private InverterMapper inverterMapper;

    @Value("${influxdb.database}")
    private String database;

    public BigDecimal fetchActualPower(Long inverterId, LocalDateTime targetTime) {
        if (inverterId == null) {
            return null;
        }

        Inverter inverter = inverterMapper.selectById(inverterId);
        if (inverter == null || inverter.getDeviceSn() == null) {
            log.warn("逆变器不存在或无设备序列号: inverterId={}", inverterId);
            return null;
        }

        long startTime = targetTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = targetTime.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        String queryStr = String.format(
                "SELECT mean(power) as avg_power FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms",
                MEASUREMENT, inverter.getDeviceSn(), startTime, endTime
        );

        try {
            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<List<Object>> values = series.getValues();
                            if (values != null && !values.isEmpty()) {
                                Object value = values.get(0).get(1);
                                if (value != null) {
                                    double power = ((Number) value).doubleValue();
                                    return BigDecimal.valueOf(power).setScale(4, RoundingMode.HALF_UP);
                                }
                            }
                        }
                    }
                }
            }

            log.debug("时序库中未找到功率数据: inverterId={}, targetTime={}", inverterId, targetTime);
        } catch (Exception e) {
            log.error("从InfluxDB查询实际功率失败: inverterId={}", inverterId, e);
        }

        return null;
    }

    public BigDecimal fetchStationActualPower(Long stationId, LocalDateTime targetTime) {
        if (stationId == null) {
            return null;
        }

        List<Inverter> inverters = inverterMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inverter>()
                        .eq(Inverter::getStationId, stationId)
                        .eq(Inverter::getStatus, 1)
        );

        if (inverters == null || inverters.isEmpty()) {
            return null;
        }

        BigDecimal totalPower = BigDecimal.ZERO;
        int validCount = 0;

        for (Inverter inverter : inverters) {
            BigDecimal power = fetchActualPower(inverter.getId(), targetTime);
            if (power != null) {
                totalPower = totalPower.add(power);
                validCount++;
            }
        }

        if (validCount > 0) {
            return totalPower;
        }

        return null;
    }

    public List<Map<String, Object>> fetchHistoricalPowerData(Long stationId, Long inverterId, int days) {
        List<Map<String, Object>> result = new ArrayList<>();

        String deviceSn;
        if (inverterId != null) {
            Inverter inverter = inverterMapper.selectById(inverterId);
            if (inverter == null || inverter.getDeviceSn() == null) {
                return result;
            }
            deviceSn = inverter.getDeviceSn();
        } else {
            List<Inverter> inverters = inverterMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inverter>()
                            .eq(Inverter::getStationId, stationId)
                            .eq(Inverter::getStatus, 1)
            );
            if (inverters == null || inverters.isEmpty()) {
                return result;
            }
            deviceSn = inverters.get(0).getDeviceSn();
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - (long) days * 24 * 60 * 60 * 1000;

        String queryStr = String.format(
                "SELECT mean(power) as power, mean(temperature) as temperature " +
                        "FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms " +
                        "GROUP BY time(1h) fill(none) ORDER BY time ASC",
                MEASUREMENT, deviceSn, startTime, endTime
        );

        try {
            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            List<List<Object>> values = series.getValues();

                            if (values != null) {
                                for (List<Object> value : values) {
                                    Map<String, Object> dp = new HashMap<>();
                                    for (int i = 0; i < columns.size(); i++) {
                                        String col = columns.get(i);
                                        Object val = value.get(i);
                                        if ("time".equals(col)) {
                                            dp.put("timestamp", parseTime(val));
                                        } else if (val != null) {
                                            dp.put(col, ((Number) val).doubleValue());
                                        }
                                    }
                                    if (dp.get("power") != null) {
                                        result.add(dp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("从InfluxDB查询历史功率数据失败: stationId={}, inverterId={}", stationId, inverterId, e);
        }

        return result;
    }

    public BigDecimal getHistoricalPowerAtTime(Long stationId, Long inverterId, LocalDateTime targetTime) {
        LocalDateTime queryTime = targetTime.minusDays(7);
        if (inverterId != null) {
            return fetchActualPower(inverterId, queryTime);
        } else {
            return fetchStationActualPower(stationId, queryTime);
        }
    }

    private Long parseTime(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return java.time.Instant.parse((String) value).toEpochMilli();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public Map<String, Object> fetchDailyInverterData(Long inverterId, LocalDate date) {
        Map<String, Object> result = new HashMap<>();

        Inverter inverter = inverterMapper.selectById(inverterId);
        if (inverter == null || inverter.getDeviceSn() == null) {
            log.warn("逆变器不存在或无设备序列号: inverterId={}", inverterId);
            return result;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        long startTime = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = endOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        String queryStr = String.format(
                "SELECT mean(temperature) as avg_temp, max(temperature) as max_temp, " +
                        "max(energy) as max_energy, min(energy) as min_energy, " +
                        "count(faultCode) as fault_count, " +
                        "non_negative_derivative(max(energy), 1h) as hourly_energy " +
                        "FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms " +
                        "GROUP BY time(1d)",
                MEASUREMENT, inverter.getDeviceSn(), startTime, endTime
        );

        try {
            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            List<List<Object>> values = series.getValues();

                            if (values != null && !values.isEmpty()) {
                                List<Object> value = values.get(0);
                                for (int i = 0; i < columns.size(); i++) {
                                    String col = columns.get(i);
                                    Object val = value.get(i);
                                    if (val != null && !"time".equals(col)) {
                                        result.put(col, ((Number) val).doubleValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!result.containsKey("avg_temp")) {
                String detailQuery = String.format(
                        "SELECT mean(temperature) as avg_temp, max(temperature) as max_temp, " +
                                "max(power) as max_power, mean(power) as avg_power " +
                                "FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms",
                        MEASUREMENT, inverter.getDeviceSn(), startTime, endTime
                );

                Query detailQueryObj = new Query(detailQuery, database);
                QueryResult detailResult = influxDB.query(detailQueryObj);

                if (detailResult.getResults() != null) {
                    for (QueryResult.Result qResult : detailResult.getResults()) {
                        if (qResult.getSeries() != null) {
                            for (QueryResult.Series series : qResult.getSeries()) {
                                List<String> columns = series.getColumns();
                                List<List<Object>> values = series.getValues();

                                if (values != null && !values.isEmpty()) {
                                    List<Object> value = values.get(0);
                                    for (int i = 0; i < columns.size(); i++) {
                                        String col = columns.get(i);
                                        Object val = value.get(i);
                                        if (val != null && !"time".equals(col)) {
                                            result.put(col, ((Number) val).doubleValue());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            long operatingHours = calculateOperatingHours(inverter.getDeviceSn(), startTime, endTime);
            if (operatingHours > 0) {
                result.put("operating_hours", (double) operatingHours);
            }

        } catch (Exception e) {
            log.error("从InfluxDB查询逆变器日数据失败: inverterId={}, date={}", inverterId, date, e);
        }

        return result;
    }

    private long calculateOperatingHours(String deviceSn, long startTime, long endTime) {
        try {
            String queryStr = String.format(
                    "SELECT count(power) as point_count FROM %s " +
                            "WHERE deviceId='%s' AND time >= %dms AND time <= %dms " +
                            "AND power > 0 GROUP BY time(1h)",
                    MEASUREMENT, deviceSn, startTime, endTime
            );

            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            long hoursWithPower = 0;
            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        hoursWithPower = qResult.getSeries().size();
                    }
                }
            }

            return hoursWithPower;
        } catch (Exception e) {
            log.error("计算工作时长失败: deviceSn={}", deviceSn, e);
            return 0;
        }
    }

    public List<Map<String, Object>> fetchHourlyDataForDay(Long inverterId, LocalDate date) {
        List<Map<String, Object>> result = new ArrayList<>();

        Inverter inverter = inverterMapper.selectById(inverterId);
        if (inverter == null || inverter.getDeviceSn() == null) {
            return result;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        long startTime = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = endOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        String queryStr = String.format(
                "SELECT mean(temperature) as temperature, mean(power) as power, max(faultCode) as faultCode " +
                        "FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms " +
                        "GROUP BY time(1h) fill(none) ORDER BY time ASC",
                MEASUREMENT, inverter.getDeviceSn(), startTime, endTime
        );

        try {
            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            List<List<Object>> values = series.getValues();

                            if (values != null) {
                                for (List<Object> value : values) {
                                    Map<String, Object> dp = new HashMap<>();
                                    for (int i = 0; i < columns.size(); i++) {
                                        String col = columns.get(i);
                                        Object val = value.get(i);
                                        if ("time".equals(col)) {
                                            dp.put("timestamp", parseTime(val));
                                        } else if (val != null) {
                                            dp.put(col, ((Number) val).doubleValue());
                                        }
                                    }
                                    if (dp.get("temperature") != null || dp.get("power") != null) {
                                        result.add(dp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询小时级数据失败: inverterId={}", inverterId, e);
        }

        return result;
    }
}
