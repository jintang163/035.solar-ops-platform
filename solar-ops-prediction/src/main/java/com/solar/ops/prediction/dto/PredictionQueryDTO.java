package com.solar.ops.prediction.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredictionQueryDTO {

    private Long stationId;

    private Long inverterId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer horizon;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
