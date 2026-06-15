package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum DiffTypeEnum {

    NO_DIFF(0, "无差异"),
    PROFIT(1, "盘盈"),
    LOSS(2, "盘亏");

    private final Integer code;
    private final String desc;

    DiffTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DiffTypeEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
