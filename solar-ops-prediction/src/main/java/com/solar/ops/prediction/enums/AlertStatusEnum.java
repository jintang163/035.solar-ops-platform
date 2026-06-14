package com.solar.ops.prediction.enums;

import lombok.Getter;

@Getter
public enum AlertStatusEnum {

    PENDING(0, "未处理"),
    HANDLED(1, "已处理"),
    IGNORED(2, "已忽略");

    private final Integer code;
    private final String desc;

    AlertStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return null;
        for (AlertStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return null;
    }
}
