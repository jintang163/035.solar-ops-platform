package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum SparePartTypeEnum {

    FAN("fan", "风扇"),
    CAPACITOR("capacitor", "电容"),
    BOARD("board", "板卡"),
    OTHER("other", "其他");

    private final String code;
    private final String desc;

    SparePartTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SparePartTypeEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
