package com.solar.ops.prediction.enums;

import lombok.Getter;

@Getter
public enum AlertTypeEnum {

    DEVIATION_EXCEEDED(1, "预测偏差超标"),
    ABNORMAL_WEATHER(2, "气象异常"),
    EQUIPMENT_FAULT_SUSPECTED(3, "疑似设备故障");

    private final Integer code;
    private final String desc;

    AlertTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return null;
        for (AlertTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return null;
    }
}
