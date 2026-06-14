package com.solar.ops.drone.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.drone.config.DroneProperties;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.enums.DefectTypeEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DefectDetectClient {

    private final OkHttpClient httpClient;
    private final DroneProperties droneProperties;

    @Autowired
    public DefectDetectClient(DroneProperties droneProperties) {
        this.droneProperties = droneProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public List<DroneDefect> detectDefects(byte[] imageData, String fileName, String imageType, Long taskId, Long imageId) {
        String url = String.format("http://%s:%d/api/v1/detect",
                droneProperties.getAiService().getHost(),
                droneProperties.getAiService().getPort());

        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", fileName,
                            RequestBody.create(imageData, MediaType.parse(imageType != null ? imageType : "image/jpeg")))
                    .addFormDataPart("confidence_threshold",
                            String.valueOf(droneProperties.getAiService().getConfidenceThreshold()))
                    .addFormDataPart("task_id", String.valueOf(taskId != null ? taskId : 0))
                    .addFormDataPart("image_id", String.valueOf(imageId != null ? imageId : 0))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("AI检测服务调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject result = JSON.parseObject(responseBody);

                if (result.getInteger("code") != null && result.getInteger("code") != 200) {
                    throw new BusinessException("AI检测失败: " + result.getString("message"));
                }

                JSONArray detections = result.getJSONArray("data");
                return parseDetections(detections, taskId, imageId);
            }
        } catch (IOException e) {
            log.error("调用AI检测服务异常", e);
            throw new BusinessException("AI检测服务连接失败: " + e.getMessage());
        }
    }

    public String getAnnotatedImagePath(String fileName) {
        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();
        String dateDir = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path annotatedPath = Paths.get(storage.getAnnotatedPath(), dateDir);
        
        try {
            if (!Files.exists(annotatedPath)) {
                Files.createDirectories(annotatedPath);
            }
            
            String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : ".jpg";
            String annotatedFileName = baseName + "_annotated" + extension;
            
            Path fullPath = annotatedPath.resolve(annotatedFileName);
            
            if (Files.exists(fullPath)) {
                return fullPath.toString();
            }
        } catch (IOException e) {
            log.error("获取标注图片路径失败", e);
        }
        
        return null;
    }

    private List<DroneDefect> parseDetections(JSONArray detections, Long taskId, Long imageId) {
        List<DroneDefect> defects = new ArrayList<>();
        double threshold = droneProperties.getAiService().getConfidenceThreshold();
        DroneProperties.DefectLevelThreshold.HotSpot hotSpotThreshold = 
                droneProperties.getDefectLevelThreshold().getHotSpot();

        for (int i = 0; i < detections.size(); i++) {
            JSONObject det = detections.getJSONObject(i);
            double confidence = det.getDoubleValue("confidence");

            if (confidence < threshold) {
                continue;
            }

            DroneDefect defect = new DroneDefect();
            defect.setTaskId(taskId);
            defect.setImageId(imageId);
            defect.setDefectCode("DEF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            defect.setDefectType(det.getString("class"));
            defect.setConfidence(BigDecimal.valueOf(confidence));
            defect.setDetectTime(LocalDateTime.now());
            defect.setConfirmed(0);

            JSONArray bbox = det.getJSONArray("bbox");
            int x1 = bbox.getInteger(0);
            int y1 = bbox.getInteger(1);
            int x2 = bbox.getInteger(2);
            int y2 = bbox.getInteger(3);

            defect.setBboxX1(x1);
            defect.setBboxY1(y1);
            defect.setBboxX2(x2);
            defect.setBboxY2(y2);
            defect.setCenterX((x1 + x2) / 2);
            defect.setCenterY((y1 + y2) / 2);
            defect.setBboxWidth(x2 - x1);
            defect.setBboxHeight(y2 - y1);

            if (det.containsKey("temperature")) {
                defect.setTemperature(BigDecimal.valueOf(det.getDoubleValue("temperature")));
            }
            if (det.containsKey("max_temperature")) {
                defect.setMaxTemperature(BigDecimal.valueOf(det.getDoubleValue("max_temperature")));
            }
            if (det.containsKey("min_temperature")) {
                defect.setMinTemperature(BigDecimal.valueOf(det.getDoubleValue("min_temperature")));
            }
            if (det.containsKey("description")) {
                defect.setDescription(det.getString("description"));
            }

            defect.setDefectLevel(determineLevel(defect));

            defects.add(defect);
        }

        return defects;
    }

    private int determineLevel(DroneDefect defect) {
        String type = defect.getDefectType();
        DefectTypeEnum typeEnum = DefectTypeEnum.getByCode(type);

        if (typeEnum == DefectTypeEnum.HOT_SPOT) {
            if (defect.getTemperature() != null) {
                double temp = defect.getTemperature().doubleValue();
                DroneProperties.DefectLevelThreshold.HotSpot threshold = 
                        droneProperties.getDefectLevelThreshold().getHotSpot();
                if (temp >= threshold.getDanger()) {
                    return 3;
                } else if (temp >= threshold.getWarning()) {
                    return 2;
                }
            }
            return 2;
        } else if (typeEnum == DefectTypeEnum.MICROCRACK || typeEnum == DefectTypeEnum.BROKEN) {
            return 3;
        } else if (typeEnum == DefectTypeEnum.DELAMINATION) {
            return 2;
        } else if (typeEnum == DefectTypeEnum.SHADOW || typeEnum == DefectTypeEnum.DIRTY) {
            return 1;
        }

        double confidence = defect.getConfidence().doubleValue();
        if (confidence >= 0.9) {
            return 3;
        } else if (confidence >= 0.7) {
            return 2;
        }
        return 1;
    }
}
