package com.solar.ops.drone.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class DroneWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private DroneProperties droneProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        DroneProperties.ImageStorage storage = droneProperties.getImageStorage();

        String baseUrl = storage.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "/uploads/drone";
        }

        String imagesPath = new File(storage.getPath()).getAbsolutePath();
        String annotatedPath = new File(storage.getAnnotatedPath()).getAbsolutePath();

        registry.addResourceHandler(baseUrl + "/images/**")
                .addResourceLocations("file:" + imagesPath + File.separator);

        registry.addResourceHandler(baseUrl + "/annotated/**")
                .addResourceLocations("file:" + annotatedPath + File.separator);
    }
}
