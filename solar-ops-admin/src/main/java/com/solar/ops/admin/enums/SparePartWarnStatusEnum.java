package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum SparePartWarnStatusEnum {

    NORMAL(0, "正常"),
    LOW_WARN(1, "低库存预警"),
    INSUFFICIENT(2, "库存不足");

    private final Integer code;
    private final String desc;

    SparePartWarnStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SparePartWarnStatusEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
