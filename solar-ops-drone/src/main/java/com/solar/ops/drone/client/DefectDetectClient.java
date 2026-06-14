package com.solar.ops.drone.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.drone.config.DroneProperties;
import com.solar.ops.drone.entity.DroneDefect;
import com.solar.ops.drone.enums.DefectTypeEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Data
    public static class DetectResult {
        private List<DroneDefect> defects;
        private String annotatedImagePath;
        private String annotatedImageUrl;
        private Integer imageWidth;
        private Integer imageHeight;
        private Integer defectCount;
    }

    public DetectResult detectAndSave(byte[] imageData, String fileName, String imageType, Long taskId, Long imageId, Long stationId) {
        String url = String.format("http://%s:%d/api/v1/detect",
                droneProperties.getAiService().getHost(),
                droneProperties.getAiService().getPort());

        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", fileName,
                            RequestBody.create(imageData, MediaType.parse(imageType != null && imageType.startsWith("image/") ? imageType : "image/jpeg")))
                    .addFormDataPart("confidence_threshold",
                            String.valueOf(droneProperties.getAiService().getConfidenceThreshold()))
                    .addFormDataPart("image_type", imageType != null ? imageType : "visible")
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
                List<DroneDefect> defects = parseDetections(detections, taskId, imageId, stationId);

                String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String annotatedPath = saveAnnotatedImageFromResult(result, fileName, dateDir);
                String annotatedUrl = buildAnnotatedImageUrl(annotatedPath);

                DetectResult detectResult = new DetectResult();
                detectResult.setDefects(defects);
                detectResult.setAnnotatedImagePath(annotatedPath);
                detectResult.setAnnotatedImageUrl(annotatedUrl);
                detectResult.setImageWidth(result.getInteger("image_width"));
                detectResult.setImageHeight(result.getInteger("image_height"));
                detectResult.setDefectCount(defects.size());

                return detectResult;
            }
        } catch (IOException e) {
            log.error("调用AI检测服务异常", e);
            throw new BusinessException("AI检测服务连接失败: " + e.getMessage());
        }
    }

    private String saveAnnotatedImageFromResult(JSONObject result, String fileName, String dateDir) {
        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();
        Path annotatedDir = Paths.get(storage.getAnnotatedPath(), dateDir);

        try {
            if (!Files.exists(annotatedDir)) {
                Files.createDirectories(annotatedDir);
            }

            String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
            String annotatedFileName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + "_annotated.jpg";
            Path annotatedFilePath = annotatedDir.resolve(annotatedFileName);

            if (result.containsKey("annotated_image_base64")) {
                String base64Data = result.getString("annotated_image_base64");
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                try (FileOutputStream fos = new FileOutputStream(annotatedFilePath.toFile())) {
                    fos.write(imageBytes);
                }
            }

            if (Files.exists(annotatedFilePath)) {
                return annotatedFilePath.toString();
            }
        } catch (Exception e) {
            log.warn("保存标注图片失败，将使用原图: {}", e.getMessage());
        }

        return null;
    }

    private String buildAnnotatedImageUrl(String localPath) {
        if (localPath == null) {
            return null;
        }
        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();
        String baseAnnotatedPath = storage.getAnnotatedPath();

        try {
            Path basePath = Paths.get(baseAnnotatedPath).toAbsolutePath();
            Path fullPath = Paths.get(localPath).toAbsolutePath();

            if (fullPath.startsWith(basePath)) {
                String relativePath = basePath.relativize(fullPath).toString().replace("\\", "/");
                return storage.getBaseUrl() + "/annotated/" + relativePath;
            }
        } catch (Exception e) {
            log.warn("构建标注图片URL失败", e);
        }

        return null;
    }

    public List<DroneDefect> detectDefects(byte[] imageData, String fileName, String imageType, Long taskId, Long imageId) {
        DetectResult result = detectAndSave(imageData, fileName, imageType, taskId, imageId, null);
        return result.getDefects();
    }

    public String getAnnotatedImagePath(String fileName) {
        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
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

    private List<DroneDefect> parseDetections(JSONArray detections, Long taskId, Long imageId, Long stationId) {
        List<DroneDefect> defects = new ArrayList<>();
        double threshold = droneProperties.getAiService().getConfidenceThreshold();

        for (int i = 0; i < detections.size(); i++) {
            JSONObject det = detections.getJSONObject(i);
            double confidence = det.getDoubleValue("confidence");

            if (confidence < threshold) {
                continue;
            }

            DroneDefect defect = new DroneDefect();
            defect.setTaskId(taskId);
            defect.setImageId(imageId);
            defect.setStationId(stationId);
            defect.setDefectCode("DEF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            defect.setDefectType(det.getString("class"));
            defect.setConfidence(BigDecimal.valueOf(confidence));
            defect.setDetectTime(LocalDateTime.now());
            defect.setConfirmed(0);
            defect.setVerified(0);
            defect.setStatus(0);

            Integer x1 = null, y1 = null, x2 = null, y2 = null;

            if (det.containsKey("bbox_x1") && det.containsKey("bbox_y1")
                    && det.containsKey("bbox_x2") && det.containsKey("bbox_y2")) {
                x1 = det.getInteger("bbox_x1");
                y1 = det.getInteger("bbox_y1");
                x2 = det.getInteger("bbox_x2");
                y2 = det.getInteger("bbox_y2");
            } else if (det.containsKey("bbox")) {
                JSONArray bbox = det.getJSONArray("bbox");
                if (bbox != null && bbox.size() >= 4) {
                    x1 = bbox.getInteger(0);
                    y1 = bbox.getInteger(1);
                    x2 = bbox.getInteger(2);
                    y2 = bbox.getInteger(3);
                }
            } else if (det.containsKey("x_min") && det.containsKey("y_min")
                    && det.containsKey("x_max") && det.containsKey("y_max")) {
                x1 = det.getInteger("x_min");
                y1 = det.getInteger("y_min");
                x2 = det.getInteger("x_max");
                y2 = det.getInteger("y_max");
            }

            if (x1 != null && y1 != null && x2 != null && y2 != null) {
                defect.setBboxX1(x1);
                defect.setBboxY1(y1);
                defect.setBboxX2(x2);
                defect.setBboxY2(y2);
                defect.setXMin(x1);
                defect.setYMin(y1);
                defect.setXMax(x2);
                defect.setYMax(y2);

                Integer centerX = null, centerY = null;
                if (det.containsKey("center_x") && det.containsKey("center_y")) {
                    centerX = det.getInteger("center_x");
                    centerY = det.getInteger("center_y");
                } else {
                    centerX = (x1 + x2) / 2;
                    centerY = (y1 + y2) / 2;
                }
                defect.setCenterX(centerX);
                defect.setCenterY(centerY);

                Integer bboxWidth = null, bboxHeight = null;
                if (det.containsKey("bbox_width") && det.containsKey("bbox_height")) {
                    bboxWidth = det.getInteger("bbox_width");
                    bboxHeight = det.getInteger("bbox_height");
                } else {
                    bboxWidth = x2 - x1;
                    bboxHeight = y2 - y1;
                }
                defect.setBboxWidth(bboxWidth);
                defect.setBboxHeight(bboxHeight);
            }

            if (det.containsKey("area_ratio")) {
                defect.setAreaRatio(BigDecimal.valueOf(det.getDoubleValue("area_ratio")));
            }

            if (det.containsKey("temperature")) {
                defect.setTemperature(BigDecimal.valueOf(det.getDoubleValue("temperature")));
            }
            if (det.containsKey("max_temperature")) {
                defect.setMaxTemperature(BigDecimal.valueOf(det.getDoubleValue("max_temperature")));
            }
            if (det.containsKey("min_temperature")) {
                defect.setMinTemperature(BigDecimal.valueOf(det.getDoubleValue("min_temperature")));
            }
            if (det.containsKey("delta_temperature")) {
                defect.setDeltaTemperature(BigDecimal.valueOf(det.getDoubleValue("delta_temperature")));
            }
            if (det.containsKey("description")) {
                defect.setDescription(det.getString("description"));
            }
            if (det.containsKey("suggestion")) {
                defect.setSuggestion(det.getString("suggestion"));
            }

            Integer defectLevel = null;
            if (det.containsKey("defect_level")) {
                defectLevel = det.getInteger("defect_level");
            } else {
                defectLevel = determineLevel(defect);
            }
            defect.setDefectLevel(defectLevel);

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
                DroneProperties.DefectLevelThreshold thresholdConfig = droneProperties.getDefectLevelThreshold();
                if (thresholdConfig != null && thresholdConfig.getHotSpot() != null) {
                    DroneProperties.DefectLevelThreshold.HotSpot hotSpot = thresholdConfig.getHotSpot();
                    if (temp >= hotSpot.getDanger()) {
                        return 3;
                    } else if (temp >= hotSpot.getWarning()) {
                        return 2;
                    }
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

        double confidence = defect.getConfidence() != null ? defect.getConfidence().doubleValue() : 0.5;
        if (confidence >= 0.9) {
            return 3;
        } else if (confidence >= 0.7) {
            return 2;
        }
        return 1;
    }

    public boolean isServiceAvailable() {
        String url = String.format("http://%s:%d/health",
                droneProperties.getAiService().getHost(),
                droneProperties.getAiService().getPort());
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("AI检测服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
