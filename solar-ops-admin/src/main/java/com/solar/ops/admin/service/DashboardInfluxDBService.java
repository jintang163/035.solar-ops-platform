package com.solar.ops.admin.service;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@ConditionalOnBean(InfluxDB.class)
public class DashboardInfluxDBService {

    private static final Logger log = LoggerFactory.getLogger(DashboardInfluxDBService.class);

    private static final String MEASUREMENT = "inverter_data";
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:00");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private InfluxDB influxDB;

    @Value("${influxdb.database}")
    private String database;

    public Map<String, BigDecimal> queryAllRealtimePower(List<String> deviceSnList) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (deviceSnList == null || deviceSnList.isEmpty()) {
            return result;
        }

        try {
            StringBuilder condition = new StringBuilder();
            for (int i = 0; i < deviceSnList.size(); i++) {
                if (i > 0) condition.append(" OR ");
                condition.append("deviceId='").append(deviceSnList.get(i)).append("'");
            }

            String sql = String.format(
                    "SELECT last(power) AS power FROM %s WHERE %s GROUP BY deviceId",
                    MEASUREMENT, condition
            );

            Query query = new Query(sql, database);
            QueryResult queryResult = influxDB.query(query);
            parsePowerResults(queryResult, result);
        } catch (Exception e) {
            log.error("查询所有逆变器实时功率失败", e);
        }

        return result;
    }

    public Map<String, Map<String, BigDecimal>> queryStationRealtimeData(Long stationId, List<String> deviceSnList) {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        if (deviceSnList == null || deviceSnList.isEmpty()) {
            return result;
        }

        try {
            StringBuilder condition = new StringBuilder();
            for (int i = 0; i < deviceSnList.size(); i++) {
                if (i > 0) condition.append(" OR ");
                condition.append("deviceId='").append(deviceSnList.get(i)).append("'");
            }

            String sql = String.format(
                    "SELECT last(power) AS power, last(voltage) AS voltage, last(current) AS current, " +
                            "last(temperature) AS temperature, last(energy) AS energy " +
                            "FROM %s WHERE %s GROUP BY deviceId",
                    MEASUREMENT, condition
            );

            Query query = new Query(sql, database);
            QueryResult queryResult = influxDB.query(query);
            parseRealtimeResults(queryResult, result);
        } catch (Exception e) {
            log.error("查询电站逆变器实时数据失败, stationId={}", stationId, e);
        }

        return result;
    }

    public List<Map<String, Object>> query24hPowerTrend() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            long now = System.currentTimeMillis();
            long start = now - 24 * 60 * 60 * 1000L;

            String sql = String.format(
                    "SELECT mean(power) AS power FROM %s WHERE time >= %dms AND time <= %dms GROUP BY time(1h) ORDER BY time ASC",
                    MEASUREMENT, start, now
            );

            Query query = new Query(sql, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            List<List<Object>> values = series.getValues();
                            if (values != null) {
                                int timeIdx = columns.indexOf("time");
                                int powerIdx = columns.indexOf("power");
                                for (List<Object> value : values) {
                                    Map<String, Object> point = new HashMap<>();
                                    if (timeIdx >= 0 && value.get(timeIdx) != null) {
                                        String timeStr = value.get(timeIdx).toString();
                                        try {
                                            LocalDateTime ldt = LocalDateTime.parse(timeStr,
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                                            point.put("time", ldt.plusHours(8).format(HOUR_FORMATTER));
                                        } catch (Exception ex) {
                                            point.put("time", timeStr);
                                        }
                                    }
                                    if (powerIdx >= 0 && value.get(powerIdx) != null) {
                                        point.put("power", BigDecimal.valueOf(((Number) value.get(powerIdx)).doubleValue())
                                                .setScale(2, BigDecimal.ROUND_HALF_UP));
                                    } else {
                                        point.put("power", BigDecimal.ZERO);
                                    }
                                    result.add(point);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询24小时功率趋势失败", e);
        }

        return result;
    }

    public List<Map<String, Object>> query7dGenerationTrend() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            long now = System.currentTimeMillis();
            long start = now - 7 * 24 * 60 * 60 * 1000L;

            String sql = String.format(
                    "SELECT spread(energy) AS energy FROM %s WHERE time >= %dms AND time <= %dms GROUP BY time(1d) ORDER BY time ASC",
                    MEASUREMENT, start, now
            );

            Query query = new Query(sql, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            List<List<Object>> values = series.getValues();
                            if (values != null) {
                                int timeIdx = columns.indexOf("time");
                                int energyIdx = columns.indexOf("energy");
                                for (List<Object> value : values) {
                                    if (energyIdx >= 0 && value.get(energyIdx) != null) {
                                        Map<String, Object> point = new HashMap<>();
                                        if (timeIdx >= 0 && value.get(timeIdx) != null) {
                                            String timeStr = value.get(timeIdx).toString();
                                            try {
                                                LocalDate date = LocalDate.parse(timeStr.substring(0, 10));
                                                point.put("date", date.format(DATE_FORMATTER));
                                            } catch (Exception ex) {
                                                point.put("date", timeStr);
                                            }
                                        }
                                        point.put("generation", BigDecimal.valueOf(((Number) value.get(energyIdx)).doubleValue())
                                                .setScale(2, BigDecimal.ROUND_HALF_UP));
                                        result.add(point);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询7天发电量趋势失败", e);
        }

        return result;
    }

    public Map<String, BigDecimal> queryRealtimeData(String deviceSn) {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            String sql = String.format(
                    "SELECT last(power) AS power, last(voltage) AS voltage, last(current) AS current, " +
                            "last(temperature) AS temperature, last(energy) AS energy " +
                            "FROM %s WHERE deviceId='%s'",
                    MEASUREMENT, deviceSn
            );

            Query query = new Query(sql, database);
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
                                        result.put(col, BigDecimal.valueOf(((Number) val).doubleValue())
                                                .setScale(2, BigDecimal.ROUND_HALF_UP));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询单台逆变器实时数据失败, deviceSn={}", deviceSn, e);
        }

        return result;
    }

    private void parsePowerResults(QueryResult queryResult, Map<String, BigDecimal> result) {
        if (queryResult.getResults() != null) {
            for (QueryResult.Result qResult : queryResult.getResults()) {
                if (qResult.getSeries() != null) {
                    for (QueryResult.Series series : qResult.getSeries()) {
                        Map<String, String> tags = series.getTags();
                        String deviceId = tags != null ? tags.get("deviceId") : null;
                        List<String> columns = series.getColumns();
                        List<List<Object>> values = series.getValues();

                        if (deviceId != null && values != null && !values.isEmpty()) {
                            int powerIdx = columns.indexOf("power");
                            if (powerIdx >= 0 && values.get(0).get(powerIdx) != null) {
                                result.put(deviceId, BigDecimal.valueOf(((Number) values.get(0).get(powerIdx)).doubleValue())
                                        .setScale(2, BigDecimal.ROUND_HALF_UP));
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseRealtimeResults(QueryResult queryResult, Map<String, Map<String, BigDecimal>> result) {
        if (queryResult.getResults() != null) {
            for (QueryResult.Result qResult : queryResult.getResults()) {
                if (qResult.getSeries() != null) {
                    for (QueryResult.Series series : qResult.getSeries()) {
                        Map<String, String> tags = series.getTags();
                        String deviceId = tags != null ? tags.get("deviceId") : null;
                        List<String> columns = series.getColumns();
                        List<List<Object>> values = series.getValues();

                        if (deviceId != null && values != null && !values.isEmpty()) {
                            Map<String, BigDecimal> data = new HashMap<>();
                            List<Object> value = values.get(0);
                            for (int i = 0; i < columns.size(); i++) {
                                String col = columns.get(i);
                                Object val = value.get(i);
                                if (val != null && !"time".equals(col)) {
                                    data.put(col, BigDecimal.valueOf(((Number) val).doubleValue())
                                            .setScale(2, BigDecimal.ROUND_HALF_UP));
                                }
                            }
                            result.put(deviceId, data);
                        }
                    }
                }
            }
        }
    }
}
