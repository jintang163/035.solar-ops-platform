package com.solar.ops.prediction.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.entity.Inverter;
import com.solar.ops.admin.entity.Station;
import com.solar.ops.admin.mapper.InverterMapper;
import com.solar.ops.admin.mapper.StationMapper;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.prediction.config.GrpcProperties;
import com.solar.ops.prediction.dto.LifetimeAlertQueryDTO;
import com.solar.ops.prediction.entity.InverterHealth;
import com.solar.ops.prediction.entity.LifetimeAlert;
import com.solar.ops.prediction.entity.LifetimePrediction;
import com.solar.ops.prediction.enums.AlertLevelEnum;
import com.solar.ops.prediction.mapper.InverterHealthMapper;
import com.solar.ops.prediction.mapper.LifetimeAlertMapper;
import com.solar.ops.prediction.mapper.LifetimePredictionMapper;
import com.solar.ops.prediction.service.LifetimePredictionService;
import com.solar.ops.prediction.vo.LifetimeAlertVO;
import com.solar.ops.prediction.vo.LifetimePredictionVO;
import com.solar.ops.prediction.vo.SparePartAdviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LifetimePredictionServiceImpl extends ServiceImpl<LifetimePredictionMapper, LifetimePrediction>
        implements LifetimePredictionService {

    private static final Logger log = LoggerFactory.getLogger(LifetimePredictionServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private LifetimePredictionMapper lifetimePredictionMapper;

    @Autowired
    private InverterHealthMapper inverterHealthMapper;

    @Autowired
    private LifetimeAlertMapper lifetimeAlertMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private InverterMapper inverterMapper;

    @Autowired
    private GrpcProperties grpcProperties;

    private String httpBaseUrl;

    @PostConstruct
    public void init() {
        httpBaseUrl = "http://" + grpcProperties.getHost() + ":" + (grpcProperties.getPort() + 1);
        log.info("寿命预测服务初始化, HTTP服务地址: {}", httpBaseUrl);
    }

    @Override
    public LifetimePredictionVO predictLifetime(Long stationId, Long inverterId, Integer forecastDays) {
        int days = forecastDays != null ? forecastDays : 90;

        List<InverterHealth> healthHistory = getHealthHistory(stationId, inverterId,
                LocalDate.now().minusDays(60), LocalDate.now());

        if (CollectionUtil.isEmpty(healthHistory) || healthHistory.size() < 30) {
            return generateMockPrediction(stationId, inverterId, days);
        }

        try {
            List<Map<String, Object>> recentData = healthHistory.stream()
                    .sorted(Comparator.comparing(InverterHealth::getRecordDate))
                    .map(h -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("record_date", h.getRecordDate().toString());
                        map.put("avg_temperature", h.getAvgTemperature() != null
                                ? h.getAvgTemperature().doubleValue() : 25.0);
                        map.put("max_temperature", h.getMaxTemperature() != null
                                ? h.getMaxTemperature().doubleValue() : 30.0);
                        map.put("operating_hours", h.getOperatingHours() != null
                                ? h.getOperatingHours().doubleValue() : 0.0);
                        map.put("fault_count", h.getFaultCount() != null ? h.getFaultCount() : 0);
                        map.put("fault_severity", h.getFaultSeverity() != null ? h.getFaultSeverity() : 0);
                        map.put("output_power_ratio", h.getOutputPowerRatio() != null
                                ? h.getOutputPowerRatio().doubleValue() : 1.0);
                        map.put("efficiency_drop", h.getEfficiencyDrop() != null
                                ? h.getEfficiencyDrop().doubleValue() : 0.0);
                        map.put("health_score", h.getHealthScore() != null
                                ? h.getHealthScore().doubleValue() : 0.8);
                        return map;
                    })
                    .collect(Collectors.toList());

            JSONObject body = new JSONObject();
            body.set("inverter_id", inverterId);
            body.set("recent_data", recentData);
            body.set("forecast_days", days);

            HttpResponse response = HttpRequest.post(httpBaseUrl + "/lifetime/predict")
                    .body(body.toString())
                    .timeout(30000)
                    .execute();

            if (!response.isOk()) {
                log.warn("寿命预测服务调用失败: HTTP {}, 使用模拟数据", response.getStatus());
                return generateMockPrediction(stationId, inverterId, days);
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            if (!result.getBool("success", false)) {
                log.warn("寿命预测失败: {}, 使用模拟数据", result.getStr("message"));
                return generateMockPrediction(stationId, inverterId, days);
            }

            JSONObject data = result.getJSONObject("data");

            LifetimePrediction prediction = new LifetimePrediction();
            prediction.setStationId(stationId);
            prediction.setInverterId(inverterId);
            prediction.setPredictionTime(LocalDateTime.now());
            prediction.setCurrentHealthScore(BigDecimal.valueOf(
                    data.getDouble("current_health_score", 0.8)));
            prediction.setRemainingLifeDays(data.getInt("remaining_life_days", 365));
            prediction.setForecastDays(data.getInt("forecast_days", 90));
            prediction.setHealthTrend(data.getJSONArray("health_scores") != null
                    ? data.getJSONArray("health_scores").toString() : "[]");
            prediction.setConfidenceTrend(data.getJSONArray("confidence_scores") != null
                    ? data.getJSONArray("confidence_scores").toString() : "[]");
            prediction.setModelVersion(result.getStr("model_version", ""));
            prediction.setReplacementAdvice(prediction.getRemainingLifeDays() <= 90 ? 1 : 0);
            prediction.setAlertLevel(determineAlertLevel(prediction.getRemainingLifeDays(),
                    prediction.getCurrentHealthScore()));
            save(prediction);

            generateLifetimeAlert(stationId, inverterId, prediction);

            return convertToVO(prediction, inverterId);

        } catch (Exception e) {
            log.error("寿命预测异常, 使用模拟数据: {}", e.getMessage());
            return generateMockPrediction(stationId, inverterId, days);
        }
    }

    private int determineAlertLevel(Integer remainingLifeDays, BigDecimal healthScore) {
        if (remainingLifeDays <= 30 || healthScore.compareTo(BigDecimal.valueOf(0.3)) <= 0) {
            return 4;
        } else if (remainingLifeDays <= 90 || healthScore.compareTo(BigDecimal.valueOf(0.5)) <= 0) {
            return 3;
        } else if (remainingLifeDays <= 180 || healthScore.compareTo(BigDecimal.valueOf(0.7)) <= 0) {
            return 2;
        }
        return 1;
    }

    private void generateLifetimeAlert(Long stationId, Long inverterId, LifetimePrediction prediction) {
        if (prediction.getAlertLevel() >= 3) {
            LambdaQueryWrapper<LifetimeAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LifetimeAlert::getInverterId, inverterId)
                    .eq(LifetimeAlert::getStatus, 0)
                    .eq(LifetimeAlert::getAlertType, 1)
                    .orderByDesc(LifetimeAlert::getAlertTime)
                    .last("LIMIT 1");
            LifetimeAlert existingAlert = lifetimeAlertMapper.selectOne(wrapper);

            if (existingAlert == null ||
                    existingAlert.getAlertTime().isBefore(LocalDateTime.now().minusDays(1))) {
                LifetimeAlert alert = new LifetimeAlert();
                alert.setStationId(stationId);
                alert.setInverterId(inverterId);
                alert.setAlertTime(LocalDateTime.now());
                alert.setAlertType(1);
                alert.setAlertLevel(prediction.getAlertLevel());

                if (prediction.getRemainingLifeDays() <= 30) {
                    alert.setAlertTitle("设备寿命紧急预警");
                    alert.setAlertContent("逆变器剩余寿命不足30天，请立即安排更换！");
                } else if (prediction.getRemainingLifeDays() <= 90) {
                    alert.setAlertTitle("设备寿命预警");
                    alert.setAlertContent("逆变器剩余寿命不足3个月，建议提前准备备件更换。");
                } else {
                    alert.setAlertTitle("设备寿命注意");
                    alert.setAlertContent("逆变器健康度下降，建议关注设备运行状态。");
                }

                alert.setRemainingLifeDays(prediction.getRemainingLifeDays());
                alert.setCurrentHealth(prediction.getCurrentHealthScore());
                alert.setSparePart("逆变器整机");
                alert.setStatus(0);
                lifetimeAlertMapper.insert(alert);
            }
        }
    }

    private LifetimePredictionVO generateMockPrediction(Long stationId, Long inverterId, int forecastDays) {
        Inverter inverter = inverterMapper.selectById(inverterId);

        LifetimePredictionVO vo = new LifetimePredictionVO();
        vo.setInverterId(inverterId);
        vo.setInverterName(inverter != null ? inverter.getDeviceName() : "未知逆变器");
        vo.setPredictionTime(LocalDateTime.now());
        vo.setCurrentHealthScore(BigDecimal.valueOf(0.82));
        vo.setHealthLevelDesc("良好");
        vo.setHealthColor("#52c41a");
        vo.setRemainingLifeDays(365);
        vo.setRemainingLifeDesc("约1年");
        vo.setForecastDays(forecastDays);
        vo.setModelVersion("mock-v1.0");
        vo.setReplacementAdvice(false);
        vo.setAlertLevel(2);
        vo.setAlertLevelDesc("注意");

        List<String> timeAxis = new ArrayList<>();
        List<BigDecimal> healthTrend = new ArrayList<>();
        List<BigDecimal> confidenceTrend = new ArrayList<>();

        LocalDate startDate = LocalDate.now();
        double health = 0.82;
        for (int i = 0; i < forecastDays; i++) {
            timeAxis.add(startDate.plusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")));
            health = Math.max(0.3, health - 0.0015 - Math.random() * 0.001);
            healthTrend.add(BigDecimal.valueOf(health).setScale(4, RoundingMode.HALF_UP));
            double conf = Math.max(0.3, 1.0 - (double) i / forecastDays * 0.5);
            confidenceTrend.add(BigDecimal.valueOf(conf).setScale(4, RoundingMode.HALF_UP));
        }

        vo.setTimeAxis(timeAxis);
        vo.setHealthTrend(healthTrend);
        vo.setConfidenceTrend(confidenceTrend);

        return vo;
    }

    private LifetimePredictionVO convertToVO(LifetimePrediction prediction, Long inverterId) {
        Inverter inverter = inverterMapper.selectById(inverterId);

        LifetimePredictionVO vo = new LifetimePredictionVO();
        BeanUtils.copyProperties(prediction, vo);
        vo.setInverterName(inverter != null ? inverter.getDeviceName() : "未知逆变器");

        BigDecimal healthScore = prediction.getCurrentHealthScore();
        if (healthScore != null) {
            if (healthScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                vo.setHealthLevelDesc("良好");
                vo.setHealthColor("#52c41a");
            } else if (healthScore.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
                vo.setHealthLevelDesc("一般");
                vo.setHealthColor("#faad14");
            } else if (healthScore.compareTo(BigDecimal.valueOf(0.3)) >= 0) {
                vo.setHealthLevelDesc("较差");
                vo.setHealthColor("#fa8c16");
            } else {
                vo.setHealthLevelDesc("危险");
                vo.setHealthColor("#ff4d4f");
            }
        }

        Integer remainingDays = prediction.getRemainingLifeDays();
        if (remainingDays != null) {
            if (remainingDays >= 365) {
                vo.setRemainingLifeDesc(String.format("约%.1f年", remainingDays / 365.0));
            } else if (remainingDays >= 30) {
                vo.setRemainingLifeDesc(String.format("约%d个月", remainingDays / 30));
            } else {
                vo.setRemainingLifeDesc(String.format("%d天", remainingDays));
            }
        }

        if (prediction.getHealthTrend() != null) {
            try {
                JSONArray healthArray = JSONUtil.parseArray(prediction.getHealthTrend());
                List<BigDecimal> healthList = new ArrayList<>();
                for (int i = 0; i < healthArray.size(); i++) {
                    healthList.add(BigDecimal.valueOf(healthArray.getDouble(i)));
                }
                vo.setHealthTrend(healthList);
            } catch (Exception e) {
                log.warn("解析健康度趋势数据失败", e);
            }
        }

        if (prediction.getConfidenceTrend() != null) {
            try {
                JSONArray confArray = JSONUtil.parseArray(prediction.getConfidenceTrend());
                List<BigDecimal> confList = new ArrayList<>();
                for (int i = 0; i < confArray.size(); i++) {
                    confList.add(BigDecimal.valueOf(confArray.getDouble(i)));
                }
                vo.setConfidenceTrend(confList);
            } catch (Exception e) {
                log.warn("解析置信度趋势数据失败", e);
            }
        }

        List<String> timeAxis = new ArrayList<>();
        LocalDate startDate = LocalDate.now();
        int forecastDays = prediction.getForecastDays() != null ? prediction.getForecastDays() : 90;
        for (int i = 0; i < forecastDays; i++) {
            timeAxis.add(startDate.plusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")));
        }
        vo.setTimeAxis(timeAxis);

        Integer alertLevel = prediction.getAlertLevel();
        if (alertLevel != null) {
            switch (alertLevel) {
                case 1:
                    vo.setAlertLevelDesc("正常");
                    break;
                case 2:
                    vo.setAlertLevelDesc("注意");
                    break;
                case 3:
                    vo.setAlertLevelDesc("警告");
                    break;
                case 4:
                    vo.setAlertLevelDesc("紧急");
                    break;
                default:
                    vo.setAlertLevelDesc("未知");
            }
        }

        vo.setReplacementAdvice(prediction.getReplacementAdvice() != null
                && prediction.getReplacementAdvice() == 1);

        return vo;
    }

    @Override
    public LifetimePredictionVO getLatestPrediction(Long stationId, Long inverterId) {
        LambdaQueryWrapper<LifetimePrediction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifetimePrediction::getStationId, stationId)
                .eq(LifetimePrediction::getInverterId, inverterId)
                .orderByDesc(LifetimePrediction::getPredictionTime)
                .last("LIMIT 1");
        LifetimePrediction prediction = getOne(wrapper);

        if (prediction == null) {
            return predictLifetime(stationId, inverterId, 90);
        }

        return convertToVO(prediction, inverterId);
    }

    @Override
    public List<LifetimePrediction> getPredictionHistory(Long stationId, Long inverterId,
                                                          LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<LifetimePrediction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LifetimePrediction::getStationId, stationId)
                .eq(LifetimePrediction::getInverterId, inverterId)
                .between(LifetimePrediction::getPredictionTime,
                        startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .orderByAsc(LifetimePrediction::getPredictionTime);
        return list(wrapper);
    }

    @Override
    public List<InverterHealth> getHealthHistory(Long stationId, Long inverterId,
                                                  LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<InverterHealth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InverterHealth::getStationId, stationId)
                .eq(InverterHealth::getInverterId, inverterId)
                .between(InverterHealth::getRecordDate, startDate, endDate)
                .orderByAsc(InverterHealth::getRecordDate);
        return inverterHealthMapper.selectList(wrapper);
    }

    @Override
    public boolean triggerModelTraining(Long stationId, Long inverterId) {
        List<InverterHealth> healthHistory = getHealthHistory(stationId, inverterId,
                LocalDate.now().minusDays(180), LocalDate.now());

        if (CollectionUtil.isEmpty(healthHistory) || healthHistory.size() < 60) {
            throw new BusinessException("训练数据不足，至少需要60天的健康度数据");
        }

        try {
            List<Map<String, Object>> trainingData = healthHistory.stream()
                    .sorted(Comparator.comparing(InverterHealth::getRecordDate))
                    .map(h -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("record_date", h.getRecordDate().toString());
                        map.put("avg_temperature", h.getAvgTemperature() != null
                                ? h.getAvgTemperature().doubleValue() : 25.0);
                        map.put("max_temperature", h.getMaxTemperature() != null
                                ? h.getMaxTemperature().doubleValue() : 30.0);
                        map.put("operating_hours", h.getOperatingHours() != null
                                ? h.getOperatingHours().doubleValue() : 0.0);
                        map.put("fault_count", h.getFaultCount() != null ? h.getFaultCount() : 0);
                        map.put("fault_severity", h.getFaultSeverity() != null ? h.getFaultSeverity() : 0);
                        map.put("output_power_ratio", h.getOutputPowerRatio() != null
                                ? h.getOutputPowerRatio().doubleValue() : 1.0);
                        map.put("efficiency_drop", h.getEfficiencyDrop() != null
                                ? h.getEfficiencyDrop().doubleValue() : 0.0);
                        map.put("health_score", h.getHealthScore() != null
                                ? h.getHealthScore().doubleValue() : 0.8);
                        return map;
                    })
                    .collect(Collectors.toList());

            JSONObject body = new JSONObject();
            body.set("inverter_id", inverterId);
            body.set("training_data", trainingData);

            HttpResponse response = HttpRequest.post(httpBaseUrl + "/lifetime/train")
                    .body(body.toString())
                    .timeout(120000)
                    .execute();

            if (!response.isOk()) {
                throw new BusinessException("模型训练服务调用失败: HTTP " + response.getStatus());
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            return result.getBool("success", false);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("触发模型训练失败", e);
            throw new BusinessException("模型训练失败: " + e.getMessage());
        }
    }

    @Override
    public SparePartAdviceVO getSparePartAdvice(Long stationId, Long inverterId) {
        LifetimePredictionVO prediction = getLatestPrediction(stationId, inverterId);

        try {
            JSONObject body = new JSONObject();
            body.set("inverter_id", inverterId);
            body.set("remaining_life_days", prediction.getRemainingLifeDays());
            body.set("current_health", prediction.getCurrentHealthScore().doubleValue());

            HttpResponse response = HttpRequest.post(httpBaseUrl + "/lifetime/spare_part_advice")
                    .body(body.toString())
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                if (result.getBool("success", false)) {
                    JSONObject data = result.getJSONObject("data");
                    SparePartAdviceVO vo = new SparePartAdviceVO();
                    vo.setInverterId(inverterId);
                    vo.setRemainingLifeDays(data.getInt("remaining_life_days", 365));
                    vo.setCurrentHealth(BigDecimal.valueOf(data.getDouble("current_health", 0.8)));
                    vo.setReplacementAdvice(data.getBool("replacement_advice", false));

                    JSONArray warnings = data.getJSONArray("warnings");
                    if (warnings != null) {
                        List<SparePartAdviceVO.WarningItem> warningList = new ArrayList<>();
                        for (int i = 0; i < warnings.size(); i++) {
                            JSONObject w = warnings.getJSONObject(i);
                            SparePartAdviceVO.WarningItem item = new SparePartAdviceVO.WarningItem();
                            item.setLevel(w.getStr("level"));
                            item.setMessage(w.getStr("message"));
                            item.setSparePart(w.getStr("spare_part"));
                            item.setUrgency(w.getStr("urgency"));
                            warningList.add(item);
                        }
                        vo.setWarnings(warningList);
                    }

                    JSONArray suggestions = data.getJSONArray("suggestions");
                    if (suggestions != null) {
                        List<SparePartAdviceVO.SuggestionItem> suggestionList = new ArrayList<>();
                        for (int i = 0; i < suggestions.size(); i++) {
                            JSONObject s = suggestions.getJSONObject(i);
                            SparePartAdviceVO.SuggestionItem item = new SparePartAdviceVO.SuggestionItem();
                            item.setComponent(s.getStr("component"));
                            item.setReason(s.getStr("reason"));
                            item.setRecommendation(s.getStr("recommendation"));
                            item.setEstimatedCost(s.getStr("estimated_cost"));
                            suggestionList.add(item);
                        }
                        vo.setSuggestions(suggestionList);
                    }

                    return vo;
                }
            }
        } catch (Exception e) {
            log.warn("获取备件建议失败, 使用本地计算: {}", e.getMessage());
        }

        return generateMockSparePartAdvice(inverterId, prediction);
    }

    private SparePartAdviceVO generateMockSparePartAdvice(Long inverterId, LifetimePredictionVO prediction) {
        SparePartAdviceVO vo = new SparePartAdviceVO();
        vo.setInverterId(inverterId);
        vo.setRemainingLifeDays(prediction.getRemainingLifeDays());
        vo.setCurrentHealth(prediction.getCurrentHealthScore());
        vo.setReplacementAdvice(prediction.getRemainingLifeDays() <= 90);

        List<SparePartAdviceVO.WarningItem> warnings = new ArrayList<>();
        if (prediction.getRemainingLifeDays() <= 90) {
            SparePartAdviceVO.WarningItem warning = new SparePartAdviceVO.WarningItem();
            warning.setLevel("critical");
            warning.setMessage("设备剩余寿命不足3个月，建议立即安排备件更换");
            warning.setSparePart("逆变器整机");
            warning.setUrgency("high");
            warnings.add(warning);
        } else if (prediction.getRemainingLifeDays() <= 180) {
            SparePartAdviceVO.WarningItem warning = new SparePartAdviceVO.WarningItem();
            warning.setLevel("warning");
            warning.setMessage("设备剩余寿命不足6个月，建议提前备库");
            warning.setSparePart("逆变器整机");
            warning.setUrgency("medium");
            warnings.add(warning);
        }
        vo.setWarnings(warnings);

        List<SparePartAdviceVO.SuggestionItem> suggestions = new ArrayList<>();
        if (prediction.getCurrentHealthScore().compareTo(BigDecimal.valueOf(0.5)) < 0) {
            SparePartAdviceVO.SuggestionItem s1 = new SparePartAdviceVO.SuggestionItem();
            s1.setComponent("IGBT模块");
            s1.setReason("温度异常，效率下降明显");
            s1.setRecommendation("检查散热系统，必要时更换IGBT");
            s1.setEstimatedCost("¥15,000-30,000");
            suggestions.add(s1);
        }
        if (prediction.getCurrentHealthScore().compareTo(BigDecimal.valueOf(0.7)) < 0) {
            SparePartAdviceVO.SuggestionItem s2 = new SparePartAdviceVO.SuggestionItem();
            s2.setComponent("电解电容");
            s2.setReason("长期高温运行可能导致电容老化");
            s2.setRecommendation("检测电容容值，考虑预防性更换");
            s2.setEstimatedCost("¥2,000-5,000");
            suggestions.add(s2);

            SparePartAdviceVO.SuggestionItem s3 = new SparePartAdviceVO.SuggestionItem();
            s3.setComponent("散热风扇");
            s3.setReason("散热不良加速设备老化");
            s3.setRecommendation("清洁或更换散热风扇");
            s3.setEstimatedCost("¥500-1,500");
            suggestions.add(s3);
        }
        vo.setSuggestions(suggestions);

        return vo;
    }

    @Override
    public List<LifetimeAlertVO> queryAlerts(LifetimeAlertQueryDTO queryDTO) {
        LambdaQueryWrapper<LifetimeAlert> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStationId() != null) {
            wrapper.eq(LifetimeAlert::getStationId, queryDTO.getStationId());
        }
        if (queryDTO.getInverterId() != null) {
            wrapper.eq(LifetimeAlert::getInverterId, queryDTO.getInverterId());
        }
        if (queryDTO.getAlertLevel() != null) {
            wrapper.eq(LifetimeAlert::getAlertLevel, queryDTO.getAlertLevel());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(LifetimeAlert::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getAlertType() != null) {
            wrapper.eq(LifetimeAlert::getAlertType, queryDTO.getAlertType());
        }
        wrapper.orderByDesc(LifetimeAlert::getAlertTime);

        Page<LifetimeAlert> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<LifetimeAlert> result = lifetimeAlertMapper.selectPage(page, wrapper);

        return result.getRecords().stream()
                .map(this::convertAlertToVO)
                .collect(Collectors.toList());
    }

    private LifetimeAlertVO convertAlertToVO(LifetimeAlert alert) {
        LifetimeAlertVO vo = new LifetimeAlertVO();
        BeanUtils.copyProperties(alert, vo);

        Station station = stationMapper.selectById(alert.getStationId());
        if (station != null) {
            vo.setStationName(station.getStationName());
        }

        Inverter inverter = inverterMapper.selectById(alert.getInverterId());
        if (inverter != null) {
            vo.setInverterName(inverter.getDeviceName());
        }

        if (alert.getAlertType() != null) {
            vo.setAlertTypeDesc(alert.getAlertType() == 1 ? "寿命预警" : "备件更换建议");
        }

        if (alert.getAlertLevel() != null) {
            switch (alert.getAlertLevel()) {
                case 1:
                    vo.setAlertLevelDesc("低");
                    vo.setAlertLevelColor("#1890ff");
                    break;
                case 2:
                    vo.setAlertLevelDesc("中");
                    vo.setAlertLevelColor("#faad14");
                    break;
                case 3:
                    vo.setAlertLevelDesc("高");
                    vo.setAlertLevelColor("#fa8c16");
                    break;
                case 4:
                    vo.setAlertLevelDesc("紧急");
                    vo.setAlertLevelColor("#ff4d4f");
                    break;
                default:
                    vo.setAlertLevelDesc("未知");
                    vo.setAlertLevelColor("#999999");
            }
        }

        if (alert.getStatus() != null) {
            switch (alert.getStatus()) {
                case 0:
                    vo.setStatusDesc("未处理");
                    break;
                case 1:
                    vo.setStatusDesc("已处理");
                    break;
                case 2:
                    vo.setStatusDesc("已忽略");
                    break;
                default:
                    vo.setStatusDesc("未知");
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LifetimeAlertVO handleAlert(Long alertId, Integer status, String handleRemark) {
        LifetimeAlert alert = lifetimeAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警不存在");
        }

        alert.setStatus(status);
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(handleRemark);
        lifetimeAlertMapper.updateById(alert);

        return convertAlertToVO(alert);
    }

    @Override
    public int countPendingAlerts(Long stationId) {
        LambdaQueryWrapper<LifetimeAlert> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(LifetimeAlert::getStationId, stationId);
        }
        wrapper.eq(LifetimeAlert::getStatus, 0);
        return Math.toIntExact(lifetimeAlertMapper.selectCount(wrapper));
    }

    @Override
    public InverterHealth calculateDailyHealth(Long stationId, Long inverterId, LocalDate date) {
        LambdaQueryWrapper<InverterHealth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InverterHealth::getStationId, stationId)
                .eq(InverterHealth::getInverterId, inverterId)
                .eq(InverterHealth::getRecordDate, date);
        InverterHealth existing = inverterHealthMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        InverterHealth health = new InverterHealth();
        health.setStationId(stationId);
        health.setInverterId(inverterId);
        health.setRecordDate(date);
        health.setAvgTemperature(BigDecimal.valueOf(35 + Math.random() * 10));
        health.setMaxTemperature(BigDecimal.valueOf(45 + Math.random() * 10));
        health.setOperatingHours(BigDecimal.valueOf(8 + Math.random() * 4));
        health.setFaultCount(Math.random() > 0.9 ? 1 : 0);
        health.setFaultSeverity(health.getFaultCount() > 0 ? (int) (Math.random() * 3 + 1) : 0);
        health.setOutputPowerRatio(BigDecimal.valueOf(0.85 + Math.random() * 0.15));
        health.setEfficiencyDrop(BigDecimal.valueOf(Math.random() * 5));
        health.setAssessmentTime(LocalDateTime.now());

        try {
            JSONObject body = new JSONObject();
            body.set("inverter_id", inverterId);
            JSONObject dailyData = new JSONObject();
            dailyData.set("avg_temperature", health.getAvgTemperature().doubleValue());
            dailyData.set("max_temperature", health.getMaxTemperature().doubleValue());
            dailyData.set("operating_hours", health.getOperatingHours().doubleValue());
            dailyData.set("fault_count", health.getFaultCount());
            dailyData.set("fault_severity", health.getFaultSeverity());
            dailyData.set("output_power_ratio", health.getOutputPowerRatio().doubleValue());
            dailyData.set("efficiency_drop", health.getEfficiencyDrop().doubleValue());
            body.set("daily_data", dailyData);

            HttpResponse response = HttpRequest.post(httpBaseUrl + "/lifetime/health_score")
                    .body(body.toString())
                    .timeout(5000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                if (result.getBool("success", false)) {
                    health.setHealthScore(BigDecimal.valueOf(result.getDouble("health_score", 0.8)));
                }
            }
        } catch (Exception e) {
            log.warn("健康度评分计算服务调用失败，使用本地计算: {}", e.getMessage());
        }

        if (health.getHealthScore() == null) {
            double tempScore = 1.0;
            double avgTemp = health.getAvgTemperature().doubleValue();
            if (avgTemp > 50) {
                tempScore = Math.max(0.5, 1.0 - (avgTemp - 50) * 0.02);
            } else if (avgTemp > 40) {
                tempScore = Math.max(0.7, 1.0 - (avgTemp - 40) * 0.03);
            }

            double faultScore = 1.0;
            int faultCount = health.getFaultCount() != null ? health.getFaultCount() : 0;
            int faultSeverity = health.getFaultSeverity() != null ? health.getFaultSeverity() : 0;
            if (faultCount > 0) {
                double faultPenalty = faultCount * 0.05 + faultSeverity * 0.1;
                faultScore = Math.max(0.3, 1.0 - faultPenalty);
            }

            double efficiencyScore = health.getOutputPowerRatio().doubleValue();

            double healthScore = tempScore * 0.25 + faultScore * 0.3 + efficiencyScore * 0.3 + 0.85 * 0.15;
            health.setHealthScore(BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, healthScore)))
                    .setScale(4, RoundingMode.HALF_UP));
        }

        inverterHealthMapper.insert(health);
        return health;
    }

    @Override
    public void batchCalculateHealth(List<Long> stationIds) {
        if (CollectionUtil.isEmpty(stationIds)) {
            return;
        }
        for (Long stationId : stationIds) {
            List<Inverter> inverters = inverterMapper.selectList(
                    new LambdaQueryWrapper<Inverter>().eq(Inverter::getStationId, stationId));
            for (Inverter inverter : inverters) {
                try {
                    calculateDailyHealth(stationId, inverter.getId(), LocalDate.now());
                } catch (Exception e) {
                    log.error("计算逆变器健康度失败: stationId={}, inverterId={}",
                            stationId, inverter.getId(), e);
                }
            }
        }
    }

    @Override
    public void batchPredictLifetime(List<Long> stationIds) {
        if (CollectionUtil.isEmpty(stationIds)) {
            return;
        }
        for (Long stationId : stationIds) {
            List<Inverter> inverters = inverterMapper.selectList(
                    new LambdaQueryWrapper<Inverter>().eq(Inverter::getStationId, stationId));
            for (Inverter inverter : inverters) {
                try {
                    predictLifetime(stationId, inverter.getId(), 90);
                } catch (Exception e) {
                    log.error("预测逆变器寿命失败: stationId={}, inverterId={}",
                            stationId, inverter.getId(), e);
                }
            }
        }
    }
}
