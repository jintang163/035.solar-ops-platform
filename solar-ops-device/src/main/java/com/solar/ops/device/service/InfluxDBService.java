package com.solar.ops.device.service;

import com.solar.ops.device.dto.InverterDataDTO;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBService.class);

    private static final String MEASUREMENT = "inverter_data";

    @Autowired
    private InfluxDB influxDB;

    @Value("${influxdb.database}")
    private String database;

    @Value("${influxdb.retention-policy}")
    private String retentionPolicy;

    public void writeInverterData(InverterDataDTO data) {
        try {
            Point point = Point.measurement(MEASUREMENT)
                    .time(data.getTimestamp() != null ? data.getTimestamp() : System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .tag("deviceId", data.getDeviceId())
                    .tag("stationId", data.getStationId() != null ? data.getStationId() : "")
                    .addField("voltage", data.getVoltage())
                    .addField("current", data.getCurrent())
                    .addField("power", data.getPower())
                    .addField("energy", data.getEnergy())
                    .addField("temperature", data.getTemperature())
                    .addField("faultCode", data.getFaultCode())
                    .build();

            influxDB.write(database, retentionPolicy, point);
            log.debug("写入InfluxDB成功: deviceId={}", data.getDeviceId());
        } catch (Exception e) {
            log.error("写入InfluxDB失败: deviceId={}", data.getDeviceId(), e);
        }
    }

    public void writeInverterDataBatch(List<InverterDataDTO> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        try {
            List<Point> points = new ArrayList<>();
            for (InverterDataDTO data : dataList) {
                Point point = Point.measurement(MEASUREMENT)
                        .time(data.getTimestamp() != null ? data.getTimestamp() : System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag("deviceId", data.getDeviceId())
                        .tag("stationId", data.getStationId() != null ? data.getStationId() : "")
                        .addField("voltage", data.getVoltage())
                        .addField("current", data.getCurrent())
                        .addField("power", data.getPower())
                        .addField("energy", data.getEnergy())
                        .addField("temperature", data.getTemperature())
                        .addField("faultCode", data.getFaultCode())
                        .build();
                points.add(point);
            }

            influxDB.write(database, retentionPolicy, points);
            log.debug("批量写入InfluxDB成功，数量: {}", dataList.size());
        } catch (Exception e) {
            log.error("批量写入InfluxDB失败", e);
        }
    }

    public List<InverterDataDTO> queryRealtimeData(String deviceId) {
        String queryStr = String.format("SELECT * FROM %s WHERE deviceId='%s' ORDER BY time DESC LIMIT 1", MEASUREMENT, deviceId);
        return executeQuery(queryStr);
    }

    public List<InverterDataDTO> queryHistoryData(String deviceId, long startTime, long endTime) {
        String queryStr = String.format(
                "SELECT * FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms ORDER BY time ASC",
                MEASUREMENT, deviceId, startTime, endTime
        );
        return executeQuery(queryStr);
    }

    public List<InverterDataDTO> queryHistoryData(String deviceId, long startTime, long endTime, int limit) {
        String queryStr = String.format(
                "SELECT * FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms ORDER BY time DESC LIMIT %d",
                MEASUREMENT, deviceId, startTime, endTime, limit
        );
        return executeQuery(queryStr);
    }

    public List<InverterDataDTO> queryStationRealtimeData(String stationId) {
        String queryStr = String.format(
                "SELECT * FROM %s WHERE stationId='%s' GROUP BY deviceId ORDER BY time DESC LIMIT 1",
                MEASUREMENT, stationId
        );
        return executeQuery(queryStr);
    }

    public List<InverterDataDTO> queryAggregatedData(String deviceId, long startTime, long endTime, String aggregation, String interval) {
        String queryStr = String.format(
                "SELECT %s(voltage) as voltage, %s(current) as current, %s(power) as power, max(energy) as energy, %s(temperature) as temperature " +
                        "FROM %s WHERE deviceId='%s' AND time >= %dms AND time <= %dms " +
                        "GROUP BY time(%s) ORDER BY time ASC",
                aggregation, aggregation, aggregation, aggregation,
                MEASUREMENT, deviceId, startTime, endTime, interval
        );
        return executeQuery(queryStr);
    }

    private List<InverterDataDTO> executeQuery(String queryStr) {
        List<InverterDataDTO> result = new ArrayList<>();

        try {
            Query query = new Query(queryStr, database);
            QueryResult queryResult = influxDB.query(query);

            if (queryResult.getResults() != null) {
                for (QueryResult.Result qResult : queryResult.getResults()) {
                    if (qResult.getSeries() != null) {
                        for (QueryResult.Series series : qResult.getSeries()) {
                            List<String> columns = series.getColumns();
                            Map<String, String> tags = series.getTags();
                            List<List<Object>> values = series.getValues();

                            if (values != null) {
                                for (List<Object> value : values) {
                                    InverterDataDTO dto = parseDataPoint(columns, tags, value);
                                    if (dto != null) {
                                        result.add(dto);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询InfluxDB失败", e);
        }

        return result;
    }

    private InverterDataDTO parseDataPoint(List<String> columns, Map<String, String> tags, List<Object> values) {
        InverterDataDTO dto = new InverterDataDTO();

        if (tags != null) {
            dto.setDeviceId(tags.get("deviceId"));
            dto.setStationId(tags.get("stationId"));
        }

        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            Object value = values.get(i);

            if (value == null) {
                continue;
            }

            switch (column) {
                case "time":
                    dto.setTimestamp(parseTime(value));
                    break;
                case "voltage":
                    dto.setVoltage(((Number) value).doubleValue());
                    break;
                case "current":
                    dto.setCurrent(((Number) value).doubleValue());
                    break;
                case "power":
                    dto.setPower(((Number) value).doubleValue());
                    break;
                case "energy":
                    dto.setEnergy(((Number) value).doubleValue());
                    break;
                case "temperature":
                    dto.setTemperature(((Number) value).doubleValue());
                    break;
                case "faultCode":
                    dto.setFaultCode(((Number) value).intValue());
                    break;
                case "deviceId":
                    dto.setDeviceId(value.toString());
                    break;
                case "stationId":
                    dto.setStationId(value.toString());
                    break;
                default:
                    break;
            }
        }

        return dto;
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
}
