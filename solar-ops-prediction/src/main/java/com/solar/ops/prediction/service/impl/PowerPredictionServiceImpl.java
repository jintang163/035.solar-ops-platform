package com.solar.ops.prediction.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.prediction.config.PredictionProperties;
import com.solar.ops.prediction.dto.AlertQueryDTO;
import com.solar.ops.prediction.dto.PredictionInputDTO;
import com.solar.ops.prediction.dto.PredictionQueryDTO;
import com.solar.ops.prediction.dto.WeatherDataDTO;
import com.solar.ops.prediction.entity.PowerPrediction;
import com.solar.ops.prediction.entity.PredictionAlert;
import com.solar.ops.prediction.entity.WeatherRecord;
import com.solar.ops.prediction.enums.AlertLevelEnum;
import com.solar.ops.prediction.enums.AlertStatusEnum;
import com.solar.ops.prediction.enums.AlertTypeEnum;
import com.solar.ops.prediction.grpc.PredictionGrpcClient;
import com.solar.ops.prediction.mapper.PowerPredictionMapper;
import com.solar.ops.prediction.mapper.PredictionAlertMapper;
import com.solar.ops.prediction.mapper.WeatherRecordMapper;
import com.solar.ops.prediction.service.PowerPredictionService;
import com.solar.ops.prediction.service.WeatherService;
import com.solar.ops.prediction.vo.PredictionAlertVO;
import com.solar.ops.prediction.vo.PredictionCurveVO;
import com.solar.ops.prediction.vo.PredictionSummaryVO;
import com.solar.ops.prediction.vo.WeatherOverviewVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PowerPredictionServiceImpl implements PowerPredictionService {

    private static final Logger log = LoggerFactory.getLogger(PowerPredictionServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private PowerPredictionMapper predictionMapper;

    @Autowired
    private PredictionAlertMapper alertMapper;

    @Autowired
    private WeatherRecordMapper weatherRecordMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private InverterMapper inverterMapper;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private PredictionGrpcClient predictionGrpcClient;

    @Autowired
    private PredictionProperties predictionProperties;

    @Autowired
    private com.solar.ops.prediction.service.PredictionInfluxDBService influxDBService;

    @Override
    public PredictionCurveVO getPredictionCurve(Long stationId, Long inverterId, Integer hours) {
        LocalDateTime now = LocalDateTime.now();
        int actualHours = hours != null ? hours : predictionProperties.getHoursAhead();
        LocalDateTime startTime = now.minusHours(actualHours);
        LocalDateTime endTime = now.plusHours(actualHours);

        List<PowerPrediction> predictions;
        if (inverterId != null) {
            predictions = predictionMapper.selectByInverterAndTimeRange(inverterId, startTime, endTime);
        } else {
            predictions = predictionMapper.selectByStationAndTimeRange(stationId, startTime, endTime);
        }

        if (CollectionUtil.isEmpty(predictions)) {
            predictions = generateMockPredictions(stationId, inverterId, actualHours);
        }

        PredictionCurveVO vo = new PredictionCurveVO();
        vo.setStationId(stationId);
        vo.setInverterId(inverterId);

        List<String> timeAxis = new ArrayList<>();
        List<BigDecimal> predictedPower = new ArrayList<>();
        List<BigDecimal> actualPower = new ArrayList<>();
        List<BigDecimal> deviationRate = new ArrayList<>();

        predictions.sort(Comparator.comparing(PowerPrediction::getTargetTime));

        BigDecimal maxDeviation = BigDecimal.ZERO;
        BigDecimal sumDeviation = BigDecimal.ZERO;
        int validCount = 0;
        int alertCount = 0;

        for (PowerPrediction p : predictions) {
            timeAxis.add(p.getTargetTime().format(TIME_FORMATTER));
            predictedPower.add(p.getPredictedPower() != null ? p.getPredictedPower() : BigDecimal.ZERO);
            actualPower.add(p.getActualPower() != null ? p.getActualPower() : null);
            deviationRate.add(p.getDeviationRate() != null
                    ? p.getDeviationRate().multiply(BigDecimal.valueOf(100)) : null);

            if (p.getDeviationRate() != null) {
                BigDecimal absDev = p.getDeviationRate().abs();
                maxDeviation = maxDeviation.max(absDev);
                sumDeviation = sumDeviation.add(absDev);
                validCount++;
                if (absDev.compareTo(BigDecimal.valueOf(predictionProperties.getDeviationAlertThreshold())) > 0) {
                    alertCount++;
                }
            }
        }

        vo.setTimeAxis(timeAxis);
        vo.setPredictedPower(predictedPower);
        vo.setActualPower(actualPower);
        vo.setDeviationRate(deviationRate);
        vo.setMaxDeviation(maxDeviation.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP));
        vo.setAvgDeviation(validCount > 0
                ? sumDeviation.divide(BigDecimal.valueOf(validCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setAlertCount(alertCount);

        return vo;
    }

    @Override
    public PredictionSummaryVO getPredictionSummary(Long stationId) {
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException("电站不存在");
        }

        PredictionSummaryVO vo = new PredictionSummaryVO();
        vo.setStationId(stationId);
        vo.setStationName(station.getStationName());

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<PowerPrediction> todayPredictions = predictionMapper.selectByStationAndTimeRange(
                stationId, startOfDay, endOfDay);

        vo.setTotalPredictions(todayPredictions.size());

        List<PowerPrediction> validated = todayPredictions.stream()
                .filter(p -> p.getActualPower() != null && p.getPredictedPower() != null)
                .collect(Collectors.toList());

        if (!validated.isEmpty()) {
            BigDecimal sumAccuracy = BigDecimal.ZERO;
            for (PowerPrediction p : validated) {
                if (p.getDeviationRate() != null) {
                    BigDecimal accuracy = BigDecimal.ONE.subtract(p.getDeviationRate().abs());
                    sumAccuracy = sumAccuracy.add(accuracy);
                }
            }
            BigDecimal avgAccuracy = sumAccuracy.divide(BigDecimal.valueOf(validated.size()), 4, RoundingMode.HALF_UP);
            vo.setAvgAccuracy(avgAccuracy.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        } else {
            vo.setAvgAccuracy(BigDecimal.valueOf(90.0));
        }

        LambdaQueryWrapper<PredictionAlert> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(PredictionAlert::getStationId, stationId)
                .ge(PredictionAlert::getAlertTime, startOfDay)
                .le(PredictionAlert::getAlertTime, endOfDay);
        List<PredictionAlert> todayAlerts = alertMapper.selectList(alertWrapper);
        vo.setAlertCount(todayAlerts.size());

        long pendingCount = todayAlerts.stream()
                .filter(a -> AlertStatusEnum.PENDING.getCode().equals(a.getStatus()))
                .count();
        vo.setPendingAlertCount((int) pendingCount);

        BigDecimal predictedEnergy = BigDecimal.ZERO;
        BigDecimal actualEnergy = BigDecimal.ZERO;
        for (PowerPrediction p : validated) {
            if (p.getPredictedPower() != null) {
                predictedEnergy = predictedEnergy.add(p.getPredictedPower());
            }
            if (p.getActualPower() != null) {
                actualEnergy = actualEnergy.add(p.getActualPower());
            }
        }
        vo.setTodayPredictedEnergy(predictedEnergy.setScale(2, RoundingMode.HALF_UP));
        vo.setTodayActualEnergy(actualEnergy.setScale(2, RoundingMode.HALF_UP));

        if (predictedEnergy.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deviation = actualEnergy.subtract(predictedEnergy)
                    .divide(predictedEnergy, 4, RoundingMode.HALF_UP);
            vo.setEnergyDeviationRate(deviation.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            vo.setEnergyDeviationRate(BigDecimal.ZERO);
        }

        return vo;
    }

    @Override
    public List<PowerPrediction> queryPredictions(PredictionQueryDTO queryDTO) {
        LambdaQueryWrapper<PowerPrediction> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getStationId() != null) {
            wrapper.eq(PowerPrediction::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(PowerPrediction::getInverterId, queryDTO.getInverterId());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(PowerPrediction::getTargetTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(PowerPrediction::getTargetTime, queryDTO.getEndTime());
        }
        if (queryDTO.getHorizon() != null) {
            wrapper.eq(PowerPrediction::getHorizon, queryDTO.getHorizon());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PowerPrediction::getStatus, queryDTO.getStatus());
        }

        wrapper.orderByDesc(PowerPrediction::getTargetTime);
        return predictionMapper.selectList(wrapper);
    }

    @Override
    public WeatherOverviewVO getWeatherOverview(Long stationId) {
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException("电站不存在");
        }

        WeatherDataDTO weatherData = weatherService.fetchHourlyForecast(
                stationId, null,
                station.getLongitude() != null ? station.getLongitude().doubleValue() : null,
                station.getLatitude() != null ? station.getLatitude().doubleValue() : null,
                predictionProperties.getHoursAhead()
        );

        WeatherOverviewVO vo = new WeatherOverviewVO();
        vo.setStationId(stationId);
        vo.setStationName(station.getStationName());
        vo.setFetchTime(weatherData.getFetchTime());
        vo.setTemperature(toBigDecimal(weatherData.getTemperature()));
        vo.setHumidity(toBigDecimal(weatherData.getHumidity()));
        vo.setIrradiance(toBigDecimal(weatherData.getIrradiance()));
        vo.setCloudCover(toBigDecimal(weatherData.getCloudCover()));
        vo.setWeather(weatherData.getWeather());
        vo.setWindDirection(weatherData.getWindDirection());
        vo.setWindSpeed(toBigDecimal(weatherData.getWindSpeed()));

        List<WeatherOverviewVO.HourlyWeatherVO> hourly = new ArrayList<>();
        if (weatherData.getHourlyForecast() != null) {
            for (WeatherDataDTO.HourlyWeather hw : weatherData.getHourlyForecast()) {
                WeatherOverviewVO.HourlyWeatherVO hvo = new WeatherOverviewVO.HourlyWeatherVO();
                hvo.setForecastTime(hw.getForecastTime());
                hvo.setTemperature(toBigDecimal(hw.getTemperature()));
                hvo.setHumidity(toBigDecimal(hw.getHumidity()));
                hvo.setIrradiance(toBigDecimal(hw.getIrradiance()));
                hvo.setCloudCover(toBigDecimal(hw.getCloudCover()));
                hvo.setWeather(hw.getWeather());
                hourly.add(hvo);
            }
        }
        vo.setHourlyForecast(hourly);

        saveWeatherRecord(stationId, weatherData);

        return vo;
    }

    @Override
    public List<PredictionAlertVO> queryAlerts(AlertQueryDTO queryDTO) {
        LambdaQueryWrapper<PredictionAlert> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getStationId() != null) {
            wrapper.eq(PredictionAlert::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(PredictionAlert::getInverterId, queryDTO.getInverterId());
        }
        if (queryDTO.getAlertType() != null) {
            wrapper.eq(PredictionAlert::getAlertType, queryDTO.getAlertType());
        }
        if (queryDTO.getAlertLevel() != null) {
            wrapper.eq(PredictionAlert::getAlertLevel, queryDTO.getAlertLevel());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PredictionAlert::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(PredictionAlert::getAlertTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(PredictionAlert::getAlertTime, queryDTO.getEndTime());
        }

        wrapper.orderByDesc(PredictionAlert::getAlertTime);

        Page<PredictionAlert> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<PredictionAlert> pageResult = alertMapper.selectPage(page, wrapper);

        Map<Long, String> stationNameMap = new HashMap<>();
        Map<Long, String> inverterNameMap = new HashMap<>();

        return pageResult.getRecords().stream().map(alert -> {
            PredictionAlertVO vo = new PredictionAlertVO();
            BeanUtils.copyProperties(alert, vo);
            vo.setAlertTypeName(AlertTypeEnum.getDescByCode(alert.getAlertType()));
            vo.setAlertLevelName(AlertLevelEnum.getDescByCode(alert.getAlertLevel()));
            vo.setStatusName(AlertStatusEnum.getDescByCode(alert.getStatus()));

            if (alert.getStationId() != null) {
                vo.setStationName(stationNameMap.computeIfAbsent(alert.getStationId(),
                        id -> {
                            Station s = stationMapper.selectById(id);
                            return s != null ? s.getStationName() : null;
                        }));
            }
            if (alert.getInverterId() != null) {
                vo.setInverterName(inverterNameMap.computeIfAbsent(alert.getInverterId(),
                        id -> {
                            Inverter inv = inverterMapper.selectById(id);
                            return inv != null ? inv.getDeviceName() : null;
                        }));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PredictionAlertVO handleAlert(Long alertId, Integer status, String remark, String rootCause) {
        PredictionAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("告警记录不存在");
        }

        alert.setStatus(status);
        alert.setHandleRemark(remark);
        alert.setHandleTime(LocalDateTime.now());
        alert.setRootCause(rootCause);
        alertMapper.updateById(alert);

        PredictionAlertVO vo = new PredictionAlertVO();
        BeanUtils.copyProperties(alert, vo);
        vo.setAlertTypeName(AlertTypeEnum.getDescByCode(alert.getAlertType()));
        vo.setAlertLevelName(AlertLevelEnum.getDescByCode(alert.getAlertLevel()));
        vo.setStatusName(AlertStatusEnum.getDescByCode(alert.getStatus()));

        if (alert.getStationId() != null) {
            Station s = stationMapper.selectById(alert.getStationId());
            vo.setStationName(s != null ? s.getStationName() : null);
        }
        if (alert.getInverterId() != null) {
            Inverter inv = inverterMapper.selectById(alert.getInverterId());
            vo.setInverterName(inv != null ? inv.getDeviceName() : null);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PredictionCurveVO executePrediction(Long stationId, Long inverterId, Integer horizon) {
        log.info("开始执行功率预测: station={}, inverter={}, horizon={}", stationId, inverterId, horizon);

        int actualHorizon = horizon != null ? horizon : predictionProperties.getHoursAhead();

        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException("电站不存在");
        }

        WeatherDataDTO weatherData = weatherService.fetchHourlyForecast(
                stationId, null,
                station.getLongitude() != null ? station.getLongitude().doubleValue() : null,
                station.getLatitude() != null ? station.getLatitude().doubleValue() : null,
                actualHorizon
        );

        saveWeatherRecord(stationId, weatherData);

        PredictionInputDTO input = buildPredictionInput(stationId, inverterId, actualHorizon, weatherData);

        Map<String, Object> grpcResult = predictionGrpcClient.predict(input);

        List<PowerPrediction> savedPredictions = savePredictions(stationId, inverterId, grpcResult, weatherData);

        return getPredictionCurve(stationId, inverterId, actualHorizon);
    }

    @Override
    public boolean triggerModelTraining(Long stationId, Long inverterId) {
        log.info("触发模型训练: station={}, inverter={}", stationId, inverterId);

        List<Map<String, Object>> trainingData = buildTrainingData(stationId, inverterId);

        Map<String, Object> result = predictionGrpcClient.train(stationId, inverterId, trainingData);

        boolean success = (Boolean) result.getOrDefault("success", false);
        log.info("模型训练结果: success={}, train_score={}, val_score={}",
                success, result.get("train_score"), result.get("validation_score"));

        return success;
    }

    @Override
    public List<WeatherRecord> queryWeatherHistory(Long stationId, Integer hours) {
        int actualHours = hours != null ? hours : 24;
        LocalDateTime startTime = LocalDateTime.now().minusHours(actualHours);

        LambdaQueryWrapper<WeatherRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherRecord::getStationId, stationId)
                .ge(WeatherRecord::getRecordTime, startTime)
                .orderByAsc(WeatherRecord::getRecordTime);

        return weatherRecordMapper.selectList(wrapper);
    }

    @Override
    public int countPendingAlerts(Long stationId) {
        LambdaQueryWrapper<PredictionAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PredictionAlert::getStatus, AlertStatusEnum.PENDING.getCode());
        if (stationId != null) {
            wrapper.eq(PredictionAlert::getStationId, stationId);
        }
        return alertMapper.selectCount(wrapper).intValue();
    }

    private PredictionInputDTO buildPredictionInput(Long stationId, Long inverterId,
                                                    int horizon, WeatherDataDTO weatherData) {
        PredictionInputDTO input = new PredictionInputDTO();
        input.setStationId(stationId);
        input.setInverterId(inverterId);
        input.setHorizon(horizon);

        List<PredictionInputDTO.FeaturePoint> features = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<WeatherDataDTO.HourlyWeather> hourlyForecast = weatherData.getHourlyForecast();

        for (int i = 0; i < horizon; i++) {
            PredictionInputDTO.FeaturePoint fp = new PredictionInputDTO.FeaturePoint();
            LocalDateTime targetTime = now.plusHours(i + 1);
            fp.setTime(targetTime);
            fp.setHour((double) targetTime.getHour());
            fp.setDayOfYear((double) targetTime.getDayOfYear());

            if (hourlyForecast != null && i < hourlyForecast.size()) {
                WeatherDataDTO.HourlyWeather hw = hourlyForecast.get(i);
                fp.setTemperature(hw.getTemperature());
                fp.setHumidity(hw.getHumidity());
                fp.setIrradiance(hw.getIrradiance());
                fp.setCloudCover(hw.getCloudCover());
            } else {
                fp.setTemperature(weatherData.getTemperature());
                fp.setHumidity(weatherData.getHumidity());
                int hour = targetTime.getHour();
                double baseIrradiance = weatherData.getIrradiance() != null ? weatherData.getIrradiance() : 0.0;
                double cloudCover = weatherData.getCloudCover() != null ? weatherData.getCloudCover() : 50.0;
                double irradiance = calculateIrradianceByTime(targetTime, baseIrradiance, cloudCover);
                fp.setIrradiance(irradiance);
                fp.setCloudCover(cloudCover);
            }

            fp.setHistoricalPower(getHistoricalPower(stationId, inverterId, targetTime));
            features.add(fp);
        }

        input.setFeatures(features);
        return input;
    }

    private Double getHistoricalPower(Long stationId, Long inverterId, LocalDateTime targetTime) {
        BigDecimal historicalPower = influxDBService.getHistoricalPowerAtTime(stationId, inverterId, targetTime);
        if (historicalPower != null) {
            return historicalPower.doubleValue();
        }

        LocalDateTime queryStart = targetTime.minusDays(7).minusMinutes(30);
        LocalDateTime queryEnd = targetTime.minusDays(7).plusMinutes(30);

        List<PowerPrediction> history;
        if (inverterId != null) {
            history = predictionMapper.selectByInverterAndTimeRange(inverterId, queryStart, queryEnd);
        } else {
            history = predictionMapper.selectByStationAndTimeRange(stationId, queryStart, queryEnd);
        }

        if (!history.isEmpty()) {
            return history.stream()
                    .filter(p -> p.getActualPower() != null)
                    .mapToDouble(p -> p.getActualPower().doubleValue())
                    .average()
                    .orElse(0.0);
        }

        return 0.0;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PowerPrediction> savePredictions(Long stationId, Long inverterId,
                                                  Map<String, Object> grpcResult,
                                                  WeatherDataDTO weatherData) {
        List<PowerPrediction> savedList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> predictions = (List<Map<String, Object>>) grpcResult.get("predictions");

        if (predictions == null) {
            return savedList;
        }

        int index = 0;
        for (Map<String, Object> p : predictions) {
            PowerPrediction pp = new PowerPrediction();
            pp.setStationId(stationId);
            pp.setInverterId(inverterId);
            pp.setPredictTime(now);

            Long targetTimeSec = (Long) p.get("target_time");
            LocalDateTime targetTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(targetTimeSec), ZoneId.systemDefault());
            pp.setTargetTime(targetTime);

            int horizon = (int) Duration.between(now, targetTime).toHours();
            pp.setHorizon(Math.max(1, Math.min(horizon, 6)));

            Double predictedPower = (Double) p.get("predicted_power");
            pp.setPredictedPower(BigDecimal.valueOf(predictedPower).setScale(4, RoundingMode.HALF_UP));
            pp.setStatus(0);
            pp.setModelVersion((String) grpcResult.getOrDefault("model_version", ""));

            if (weatherData.getHourlyForecast() != null && index < weatherData.getHourlyForecast().size()) {
                WeatherDataDTO.HourlyWeather hw = weatherData.getHourlyForecast().get(index);
                pp.setTemperature(toBigDecimal(hw.getTemperature()));
                pp.setHumidity(toBigDecimal(hw.getHumidity()));
                pp.setIrradiance(toBigDecimal(hw.getIrradiance()));
                pp.setCloudCover(toBigDecimal(hw.getCloudCover()));
            }

            predictionMapper.insert(pp);
            savedList.add(pp);
            index++;
        }

        return savedList;
    }

    private void saveWeatherRecord(Long stationId, WeatherDataDTO weatherData) {
        try {
            WeatherRecord record = new WeatherRecord();
            record.setStationId(stationId);
            record.setRecordTime(weatherData.getFetchTime() != null
                    ? weatherData.getFetchTime() : LocalDateTime.now());
            record.setTemperature(toBigDecimal(weatherData.getTemperature()));
            record.setHumidity(toBigDecimal(weatherData.getHumidity()));
            record.setIrradiance(toBigDecimal(weatherData.getIrradiance()));
            record.setCloudCover(toBigDecimal(weatherData.getCloudCover()));
            record.setWeather(weatherData.getWeather());
            record.setWindDirection(weatherData.getWindDirection());
            record.setWindSpeed(toBigDecimal(weatherData.getWindSpeed()));
            record.setPressure(toBigDecimal(weatherData.getPressure()));
            record.setSource("api");
            weatherRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("保存气象记录失败: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> buildTrainingData(Long stationId, Long inverterId) {
        List<Map<String, Object>> trainingData = new ArrayList<>();

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(30);

        List<Map<String, Object>> influxHistory = influxDBService.fetchHistoricalPowerData(stationId, inverterId, 30);

        if (influxHistory.isEmpty()) {
            log.warn("时序库无历史功率数据，使用MySQL历史预测记录作为训练数据");
            List<PowerPrediction> mysqlHistory;
            if (inverterId != null) {
                mysqlHistory = predictionMapper.selectByInverterAndTimeRange(inverterId, startTime, endTime);
            } else {
                mysqlHistory = predictionMapper.selectByStationAndTimeRange(stationId, startTime, endTime);
            }

            for (PowerPrediction p : mysqlHistory) {
                if (p.getActualPower() == null || p.getTargetTime() == null) {
                    continue;
                }
                Map<String, Object> dp = new HashMap<>();
                dp.put("temperature", p.getTemperature() != null ? p.getTemperature().doubleValue() : 20.0);
                dp.put("humidity", p.getHumidity() != null ? p.getHumidity().doubleValue() : 50.0);
                dp.put("irradiance", p.getIrradiance() != null ? p.getIrradiance().doubleValue() : 300.0);
                dp.put("cloud_cover", p.getCloudCover() != null ? p.getCloudCover().doubleValue() : 30.0);
                dp.put("hour", (double) p.getTargetTime().getHour());
                dp.put("day_of_year", (double) p.getTargetTime().getDayOfYear());
                dp.put("historical_power", p.getPredictedPower() != null ? p.getPredictedPower().doubleValue() : 0.0);
                dp.put("target_power", p.getActualPower().doubleValue());
                trainingData.add(dp);
            }
        } else {
            log.info("从时序库获取到{}条历史功率数据用于训练", influxHistory.size());

            Map<LocalDate, WeatherRecord> dailyWeatherMap = new HashMap<>();
            List<WeatherRecord> weatherHistory = weatherRecordMapper.selectList(
                    new LambdaQueryWrapper<WeatherRecord>()
                            .eq(WeatherRecord::getStationId, stationId)
                            .ge(WeatherRecord::getRecordTime, startTime)
                            .le(WeatherRecord::getRecordTime, endTime)
                            .orderByAsc(WeatherRecord::getRecordTime));

            for (WeatherRecord wr : weatherHistory) {
                dailyWeatherMap.put(wr.getRecordTime().toLocalDate(), wr);
            }

            for (Map<String, Object> dp : influxHistory) {
                Long timestamp = (Long) dp.get("timestamp");
                if (timestamp == null) continue;

                LocalDateTime dataTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                Double power = (Double) dp.get("power");
                if (power == null || power <= 0) continue;

                WeatherRecord wr = dailyWeatherMap.get(dataTime.toLocalDate());

                double temperature = wr != null && wr.getTemperature() != null ? wr.getTemperature().doubleValue() : 20.0;
                double humidity = wr != null && wr.getHumidity() != null ? wr.getHumidity().doubleValue() : 50.0;
                double baseIrradiance = wr != null && wr.getIrradiance() != null ? wr.getIrradiance().doubleValue() : 0.0;
                double cloudCover = wr != null && wr.getCloudCover() != null ? wr.getCloudCover().doubleValue() : 30.0;

                double irradiance = calculateIrradianceByTime(dataTime, baseIrradiance, cloudCover);

                Map<String, Object> trainingPoint = new HashMap<>();
                trainingPoint.put("temperature", temperature);
                trainingPoint.put("humidity", humidity);
                trainingPoint.put("irradiance", irradiance);
                trainingPoint.put("cloud_cover", cloudCover);
                trainingPoint.put("hour", (double) dataTime.getHour());
                trainingPoint.put("day_of_year", (double) dataTime.getDayOfYear());
                trainingPoint.put("historical_power", power * 0.98);
                trainingPoint.put("target_power", power);
                trainingData.add(trainingPoint);
            }
        }

        log.info("模型训练数据集大小: {}", trainingData.size());
        return trainingData;
    }

    private double calculateIrradianceByTime(LocalDateTime time, double baseIrradiance, double cloudCover) {
        int hour = time.getHour();
        if (hour < 6 || hour > 18) {
            return 0.0;
        }
        double solarFactor = Math.sin(Math.PI * (hour - 6) / 12);
        double cloudFactor = 1.0 - (cloudCover / 150.0);
        double maxIrradiance = 1000.0 * solarFactor * cloudFactor;
        if (baseIrradiance > 0) {
            return Math.max(baseIrradiance, maxIrradiance * 0.3);
        }
        return maxIrradiance;
    }

    private List<PowerPrediction> generateMockPredictions(Long stationId, Long inverterId, int hours) {
        List<PowerPrediction> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = -hours; i <= hours; i++) {
            LocalDateTime targetTime = now.plusHours(i);
            int hour = targetTime.getHour();
            double factor = (hour >= 6 && hour <= 18) ? Math.sin(Math.PI * (hour - 6) / 12) : 0;
            double basePower = 80 * factor;

            PowerPrediction p = new PowerPrediction();
            p.setStationId(stationId);
            p.setInverterId(inverterId);
            p.setTargetTime(targetTime);
            p.setPredictTime(targetTime.minusHours(1));
            p.setHorizon(Math.max(1, Math.min(i > 0 ? i : 1, 6)));

            double predicted = basePower * (0.9 + Math.random() * 0.2);
            p.setPredictedPower(BigDecimal.valueOf(predicted).setScale(4, RoundingMode.HALF_UP));

            if (i <= 0) {
                double actual = basePower * (0.85 + Math.random() * 0.25);
                p.setActualPower(BigDecimal.valueOf(actual).setScale(4, RoundingMode.HALF_UP));
                BigDecimal deviation = p.getActualPower().subtract(p.getPredictedPower());
                p.setDeviation(deviation);
                if (p.getPredictedPower().compareTo(BigDecimal.ZERO) > 0) {
                    p.setDeviationRate(deviation.divide(p.getPredictedPower(), 4, RoundingMode.HALF_UP));
                }
                p.setStatus(1);
            } else {
                p.setStatus(0);
            }

            list.add(p);
        }

        return list;
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
