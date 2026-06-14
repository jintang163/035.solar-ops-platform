package com.solar.ops.prediction.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.prediction.config.PredictionProperties;
import com.solar.ops.prediction.entity.PowerPrediction;
import com.solar.ops.prediction.entity.PredictionAlert;
import com.solar.ops.prediction.enums.AlertLevelEnum;
import com.solar.ops.prediction.enums.AlertStatusEnum;
import com.solar.ops.prediction.enums.AlertTypeEnum;
import com.solar.ops.prediction.mapper.PowerPredictionMapper;
import com.solar.ops.prediction.mapper.PredictionAlertMapper;
import com.solar.ops.prediction.service.PredictionInfluxDBService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeviationCheckJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DeviationCheckJob.class);

    @Autowired
    private PowerPredictionMapper predictionMapper;

    @Autowired
    private PredictionAlertMapper alertMapper;

    @Autowired
    private PredictionProperties predictionProperties;

    @Autowired
    private PredictionInfluxDBService influxDBService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行预测偏差校验任务");

        try {
            LocalDateTime checkStartTime = LocalDateTime.now().minusHours(12);
            LocalDateTime checkEndTime = LocalDateTime.now().minusMinutes(30);

            LambdaQueryWrapper<PowerPrediction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PowerPrediction::getStatus, 0)
                    .isNotNull(PowerPrediction::getPredictedPower)
                    .ge(PowerPrediction::getTargetTime, checkStartTime)
                    .le(PowerPrediction::getTargetTime, checkEndTime);

            List<PowerPrediction> unverifiedPredictions = predictionMapper.selectList(wrapper);
            log.info("待校验预测记录数: {}", unverifiedPredictions.size());

            int alertCount = 0;
            int skipCount = 0;
            for (PowerPrediction prediction : unverifiedPredictions) {
                try {
                    boolean hasAlert = checkAndGenerateAlert(prediction);
                    if (hasAlert) {
                        alertCount++;
                    } else if (prediction.getStatus() == 2) {
                        skipCount++;
                    }
                } catch (Exception e) {
                    log.error("校验预测记录失败: predictionId={}", prediction.getId(), e);
                }
            }

            log.info("预测偏差校验完成，生成告警数: {}, 无实际数据跳过: {}", alertCount, skipCount);
        } catch (Exception e) {
            log.error("预测偏差校验任务异常", e);
            throw new JobExecutionException(e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean checkAndGenerateAlert(PowerPrediction prediction) {
        if (prediction.getPredictedPower() == null
                || prediction.getPredictedPower().compareTo(BigDecimal.ONE) < 0) {
            prediction.setStatus(2);
            predictionMapper.updateById(prediction);
            return false;
        }

        BigDecimal actualPower = fetchActualPower(prediction);
        if (actualPower == null) {
            log.debug("时序库无实际功率数据，跳过校验: predictionId={}, targetTime={}",
                    prediction.getId(), prediction.getTargetTime());
            return false;
        }

        prediction.setActualPower(actualPower);
        BigDecimal deviation = actualPower.subtract(prediction.getPredictedPower());
        prediction.setDeviation(deviation);

        BigDecimal deviationRate = deviation.abs()
                .divide(prediction.getPredictedPower(), 6, RoundingMode.HALF_UP);
        prediction.setDeviationRate(deviationRate);
        prediction.setStatus(1);
        predictionMapper.updateById(prediction);

        BigDecimal threshold = BigDecimal.valueOf(predictionProperties.getDeviationAlertThreshold());
        if (deviationRate.compareTo(threshold) > 0) {
            generateAlert(prediction, deviationRate, threshold);
            return true;
        }

        return false;
    }

    private BigDecimal fetchActualPower(PowerPrediction prediction) {
        LocalDateTime targetTime = prediction.getTargetTime();

        if (prediction.getInverterId() != null) {
            return influxDBService.fetchActualPower(prediction.getInverterId(), targetTime);
        } else {
            return influxDBService.fetchStationActualPower(prediction.getStationId(), targetTime);
        }
    }

    private void generateAlert(PowerPrediction prediction, BigDecimal deviationRate,
                               BigDecimal threshold) {
        LambdaQueryWrapper<PredictionAlert> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(PredictionAlert::getPredictionId, prediction.getId());
        if (alertMapper.selectCount(existWrapper) > 0) {
            return;
        }

        PredictionAlert alert = new PredictionAlert();
        alert.setStationId(prediction.getStationId());
        alert.setInverterId(prediction.getInverterId());
        alert.setPredictionId(prediction.getId());
        alert.setAlertTime(LocalDateTime.now());
        alert.setTargetTime(prediction.getTargetTime());
        alert.setAlertType(AlertTypeEnum.DEVIATION_EXCEEDED.getCode());

        BigDecimal deviationPercent = deviationRate.multiply(BigDecimal.valueOf(100));
        if (deviationPercent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            alert.setAlertLevel(AlertLevelEnum.URGENT.getCode());
        } else if (deviationPercent.compareTo(BigDecimal.valueOf(35)) >= 0) {
            alert.setAlertLevel(AlertLevelEnum.HIGH.getCode());
        } else if (deviationPercent.compareTo(BigDecimal.valueOf(25)) >= 0) {
            alert.setAlertLevel(AlertLevelEnum.MEDIUM.getCode());
        } else {
            alert.setAlertLevel(AlertLevelEnum.LOW.getCode());
        }

        String rootCause = analyzeRootCause(prediction);

        StringBuilder content = new StringBuilder();
        content.append(String.format("预测偏差%.2f%%超过阈值%.0f%%。",
                deviationPercent.doubleValue(), threshold.multiply(BigDecimal.valueOf(100)).doubleValue()));
        content.append(String.format("预测功率: %.2f kW, 实际功率: %.2f kW。",
                prediction.getPredictedPower() != null ? prediction.getPredictedPower().doubleValue() : 0,
                prediction.getActualPower() != null ? prediction.getActualPower().doubleValue() : 0));

        if ("weather".equals(rootCause)) {
            content.append("初步判断: 气象因素导致（辐照度/云量偏差）");
        } else if ("equipment".equals(rootCause)) {
            content.append("初步判断: 疑似设备故障，建议检查逆变器运行状态");
        } else {
            content.append("初步判断: 待进一步排查");
        }

        alert.setAlertContent(content.toString());
        alert.setPredictedValue(prediction.getPredictedPower());
        alert.setActualValue(prediction.getActualPower());
        alert.setDeviationRate(deviationRate);
        alert.setThreshold(threshold);
        alert.setStatus(AlertStatusEnum.PENDING.getCode());
        alert.setRootCause(rootCause);

        alertMapper.insert(alert);

        log.warn("生成预测偏差告警: stationId={}, inverterId={}, deviationRate={:.4f}, rootCause={}",
                prediction.getStationId(), prediction.getInverterId(), deviationRate, rootCause);
    }

    private String analyzeRootCause(PowerPrediction prediction) {
        if (prediction.getIrradiance() == null || prediction.getCloudCover() == null) {
            return "other";
        }

        double irradiance = prediction.getIrradiance().doubleValue();
        double cloudCover = prediction.getCloudCover().doubleValue();
        BigDecimal predicted = prediction.getPredictedPower();
        BigDecimal actual = prediction.getActualPower();

        if (predicted == null || actual == null) {
            return "other";
        }

        double ratio = actual.doubleValue() / predicted.doubleValue();

        if (cloudCover > 70 && irradiance < 200) {
            return "weather";
        }

        if (cloudCover < 30 && irradiance > 500 && ratio < 0.7) {
            return "equipment";
        }

        if (ratio < 0.5) {
            return "equipment";
        }

        if (cloudCover > 50 && ratio < 0.8) {
            return "weather";
        }

        return "other";
    }
}
