package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum UrgencyEnum {

    NORMAL(1, "一般"),
    URGENT(2, "紧急"),
    VERY_URGENT(3, "非常紧急");

    private final Integer code;
    private final String desc;

    UrgencyEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UrgencyEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
