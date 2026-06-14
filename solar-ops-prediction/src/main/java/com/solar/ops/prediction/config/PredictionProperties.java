package com.solar.ops.prediction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "prediction")
public class PredictionProperties {

    private int hoursAhead = 6;
    private double deviationAlertThreshold = 0.2;

    public int getHoursAhead() {
        return hoursAhead;
    }

    public void setHoursAhead(int hoursAhead) {
        this.hoursAhead = hoursAhead;
    }

    public double getDeviationAlertThreshold() {
        return deviationAlertThreshold;
    }

    public void setDeviationAlertThreshold(double deviationAlertThreshold) {
        this.deviationAlertThreshold = deviationAlertThreshold;
    }
}
