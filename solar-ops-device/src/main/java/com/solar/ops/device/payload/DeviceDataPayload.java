package com.solar.ops.device.payload;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceDataPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;

    private String deviceType;

    private String stationId;

    private String dataType;

    private String rawData;

    private Long timestamp;

    private String protocol;
}
