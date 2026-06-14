package com.solar.ops.prediction.service;

import com.solar.ops.prediction.dto.AlertQueryDTO;
import com.solar.ops.prediction.dto.PredictionInputDTO;
import com.solar.ops.prediction.dto.PredictionQueryDTO;
import com.solar.ops.prediction.entity.PowerPrediction;
import com.solar.ops.prediction.entity.PredictionAlert;
import com.solar.ops.prediction.entity.WeatherRecord;
import com.solar.ops.prediction.vo.PredictionAlertVO;
import com.solar.ops.prediction.vo.PredictionCurveVO;
import com.solar.ops.prediction.vo.PredictionSummaryVO;
import com.solar.ops.prediction.vo.WeatherOverviewVO;

import java.util.List;

public interface PowerPredictionService {

    PredictionCurveVO getPredictionCurve(Long stationId, Long inverterId,
                                          Integer hours);

    PredictionSummaryVO getPredictionSummary(Long stationId);

    List<PowerPrediction> queryPredictions(PredictionQueryDTO queryDTO);

    WeatherOverviewVO getWeatherOverview(Long stationId);

    List<PredictionAlertVO> queryAlerts(AlertQueryDTO queryDTO);

    PredictionAlertVO handleAlert(Long alertId, Integer status, String remark, String rootCause);

    PredictionCurveVO executePrediction(Long stationId, Long inverterId, Integer horizon);

    boolean triggerModelTraining(Long stationId, Long inverterId);

    List<WeatherRecord> queryWeatherHistory(Long stationId, Integer hours);

    int countPendingAlerts(Long stationId);
}
