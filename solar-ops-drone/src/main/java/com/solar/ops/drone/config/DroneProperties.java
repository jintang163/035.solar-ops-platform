package com.solar.ops.drone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "drone")
public class DroneProperties {

    private AiService aiService = new AiService();

    private ImageStorage imageStorage = new ImageStorage();

    private DefectLevelThreshold defectLevelThreshold = new DefectLevelThreshold();

    private Websocket websocket = new Websocket();

    @Data
    public static class AiService {
        private String host = "127.0.0.1";
        private int port = 50053;
        private int timeout = 60000;
        private double confidenceThreshold = 0.5;
    }

    @Data
    public static class ImageStorage {
        private String path = "./data/drone/images";
        private String annotatedPath = "./data/drone/annotated";
        private String baseUrl = "/uploads/drone";
        private long maxSize = 52428800L;
        private String[] allowedTypes = {"image/jpeg", "image/png", "image/jpg", "image/tiff"};
        private String[] allowedExtensions = {"jpg", "jpeg", "png", "bmp", "tiff"};
    }

    @Data
    public static class DefectLevelThreshold {
        private HotSpot hotSpot = new HotSpot();

        @Data
        public static class HotSpot {
            private double warning = 50;
            private double danger = 70;
        }
    }

    @Data
    public static class Websocket {
        private String endpoint = "/ws/drone";
    }
}
