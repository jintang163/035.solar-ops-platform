package com.solar.ops.prediction.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertQueryDTO {

    private Long stationId;

    private Long inverterId;

    private Integer alertType;

    private Integer alertLevel;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
