package com.solar.ops.prediction.service;

import com.solar.ops.prediction.dto.LifetimeAlertQueryDTO;
import com.solar.ops.prediction.entity.InverterHealth;
import com.solar.ops.prediction.entity.LifetimeAlert;
import com.solar.ops.prediction.entity.LifetimePrediction;
import com.solar.ops.prediction.vo.LifetimeAlertVO;
import com.solar.ops.prediction.vo.LifetimePredictionVO;
import com.solar.ops.prediction.vo.SparePartAdviceVO;

import java.time.LocalDate;
import java.util.List;

public interface LifetimePredictionService {

    LifetimePredictionVO predictLifetime(Long stationId, Long inverterId, Integer forecastDays);

    LifetimePredictionVO getLatestPrediction(Long stationId, Long inverterId);

    List<LifetimePrediction> getPredictionHistory(Long stationId, Long inverterId,
                                                  LocalDate startDate, LocalDate endDate);

    List<InverterHealth> getHealthHistory(Long stationId, Long inverterId,
                                          LocalDate startDate, LocalDate endDate);

    boolean triggerModelTraining(Long stationId, Long inverterId);

    SparePartAdviceVO getSparePartAdvice(Long stationId, Long inverterId);

    List<LifetimeAlertVO> queryAlerts(LifetimeAlertQueryDTO queryDTO);

    LifetimeAlertVO handleAlert(Long alertId, Integer status, String handleRemark);

    int countPendingAlerts(Long stationId);

    InverterHealth calculateDailyHealth(Long stationId, Long inverterId, LocalDate date);

    void batchCalculateHealth(List<Long> stationIds);

    void batchPredictLifetime(List<Long> stationIds);
}
