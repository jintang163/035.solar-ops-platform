package com.solar.ops.drone.enums;

import lombok.Getter;

@Getter
public enum DetectStatusEnum {

    PENDING(0, "待检测"),
    DETECTING(1, "检测中"),
    COMPLETED(2, "检测完成"),
    FAILED(3, "检测失败");

    private final Integer code;
    private final String desc;

    DetectStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        for (DetectStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status.getDesc();
            }
        }
        return "未知";
    }
}
