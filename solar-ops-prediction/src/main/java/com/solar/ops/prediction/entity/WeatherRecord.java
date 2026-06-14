package com.solar.ops.prediction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("weather_record")
public class WeatherRecord extends BaseEntity {

    private Long stationId;

    private LocalDateTime recordTime;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private BigDecimal irradiance;

    private BigDecimal cloudCover;

    private String weather;

    private String windDirection;

    private BigDecimal windSpeed;

    private BigDecimal pressure;

    private String source;
}
