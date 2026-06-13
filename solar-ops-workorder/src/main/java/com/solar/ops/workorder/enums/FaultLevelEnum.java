package com.solar.ops.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FaultLevelEnum {

    LOW(1, "低级"),
    MEDIUM(2, "中级"),
    HIGH(3, "高级"),
    CRITICAL(4, "紧急");

    private final Integer code;
    private final String desc;

    public static FaultLevelEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FaultLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
