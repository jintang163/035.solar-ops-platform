package com.solar.ops.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InverterDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;

    private String stationId;

    private Double voltage;

    private Double current;

    private Double power;

    private Double energy;

    private Double temperature;

    private Integer faultCode;

    private Long timestamp;
}
