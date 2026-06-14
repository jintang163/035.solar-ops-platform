package com.solar.ops.prediction.grpc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.prediction.config.GrpcProperties;
import com.solar.ops.prediction.dto.PredictionInputDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class PredictionGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionGrpcClient.class);

    @Autowired
    private GrpcProperties grpcProperties;

    private String httpBaseUrl;

    @PostConstruct
    public void init() {
        httpBaseUrl = "http://" + grpcProperties.getHost() + ":" + (grpcProperties.getPort() + 1);
        log.info("预测服务初始化, gRPC: {}:{}, HTTP fallback: {}",
                grpcProperties.getHost(), grpcProperties.getPort(), httpBaseUrl);
    }

    @PreDestroy
    public void destroy() {
        log.info("预测服务客户端关闭");
    }

    public Map<String, Object> predict(PredictionInputDTO input) {
        try {
            return predictViaHttp(input);
        } catch (Exception e) {
            log.error("预测服务调用失败: {}", e.getMessage());
            throw new BusinessException("预测服务暂不可用，请稍后重试: " + e.getMessage());
        }
    }

    public Map<String, Object> train(Long stationId, Long inverterId, List<Map<String, Object>> trainingData) {
        try {
            return trainViaHttp(stationId, inverterId, trainingData);
        } catch (Exception e) {
            log.error("训练服务调用失败: {}", e.getMessage());
            throw new BusinessException("模型训练服务暂不可用，请稍后重试: " + e.getMessage());
        }
    }

    public Map<String, Object> getModelStatus(Long stationId, Long inverterId) {
        try {
            return getModelStatusViaHttp(stationId, inverterId);
        } catch (Exception e) {
            log.error("模型状态查询失败: {}", e.getMessage());
            throw new BusinessException("模型状态查询失败，请稍后重试: " + e.getMessage());
        }
    }

    private Map<String, Object> predictViaHttp(PredictionInputDTO input) {
        JSONObject body = new JSONObject();
        body.set("station_id", input.getStationId());
        body.set("inverter_id", input.getInverterId());
        body.set("horizon", input.getHorizon());

        JSONArray features = new JSONArray();
        if (input.getFeatures() != null) {
            for (PredictionInputDTO.FeaturePoint fp : input.getFeatures()) {
                JSONObject f = new JSONObject();
                f.set("timestamp", fp.getTime() != null
                        ? fp.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
                        : System.currentTimeMillis() / 1000);
                f.set("temperature", fp.getTemperature());
                f.set("humidity", fp.getHumidity());
                f.set("irradiance", fp.getIrradiance());
                f.set("cloud_cover", fp.getCloudCover());
                f.set("hour", fp.getHour());
                f.set("day_of_year", fp.getDayOfYear());
                f.set("historical_power", fp.getHistoricalPower());
                features.add(f);
            }
        }
        body.set("features", features);

        HttpResponse response = HttpRequest.post(httpBaseUrl + "/predict")
                .body(body.toString())
                .timeout(10000)
                .execute();

        if (!response.isOk()) {
            throw new BusinessException("预测服务调用失败: HTTP " + response.getStatus());
        }

        JSONObject result = JSONUtil.parseObj(response.body());
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.getBool("success", false));
        map.put("message", result.getStr("message", ""));
        map.put("model_version", result.getStr("model_version", ""));

        List<Map<String, Object>> predictions = new ArrayList<>();
        JSONArray predArray = result.getJSONArray("predictions");
        if (predArray != null) {
            for (int i = 0; i < predArray.size(); i++) {
                JSONObject p = predArray.getJSONObject(i);
                Map<String, Object> pm = new HashMap<>();
                pm.put("target_time", p.getLong("target_time"));
                pm.put("predicted_power", p.getDouble("predicted_power"));
                pm.put("confidence", p.getDouble("confidence"));
                predictions.add(pm);
            }
        }
        map.put("predictions", predictions);
        return map;
    }

    private Map<String, Object> trainViaHttp(Long stationId, Long inverterId, List<Map<String, Object>> trainingData) {
        JSONObject body = new JSONObject();
        body.set("station_id", stationId);
        body.set("inverter_id", inverterId);
        body.set("training_data", trainingData);

        HttpResponse response = HttpRequest.post(httpBaseUrl + "/train")
                .body(body.toString())
                .timeout(60000)
                .execute();

        if (!response.isOk()) {
            throw new BusinessException("训练服务调用失败: HTTP " + response.getStatus());
        }

        JSONObject result = JSONUtil.parseObj(response.body());
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.getBool("success", false));
        map.put("message", result.getStr("message", ""));
        map.put("model_version", result.getStr("model_version", ""));
        map.put("train_score", result.getDouble("train_score", 0.0));
        map.put("validation_score", result.getDouble("validation_score", 0.0));
        return map;
    }

    private Map<String, Object> getModelStatusViaHttp(Long stationId, Long inverterId) {
        Map<String, Object> params = new HashMap<>();
        params.put("station_id", stationId);
        if (inverterId != null) {
            params.put("inverter_id", inverterId);
        }

        HttpResponse response = HttpRequest.get(httpBaseUrl + "/model_status")
                .form(params)
                .timeout(5000)
                .execute();

        if (!response.isOk()) {
            throw new BusinessException("模型状态查询失败: HTTP " + response.getStatus());
        }

        JSONObject result = JSONUtil.parseObj(response.body());
        Map<String, Object> map = new HashMap<>();
        map.put("exists", result.getBool("exists", false));
        map.put("model_version", result.getStr("model_version", ""));
        map.put("last_train_time", result.getStr("last_train_time", ""));
        map.put("sample_count", result.getInt("sample_count", 0));
        map.put("last_metric", result.getDouble("last_metric", 0.0));
        return map;
    }

}
