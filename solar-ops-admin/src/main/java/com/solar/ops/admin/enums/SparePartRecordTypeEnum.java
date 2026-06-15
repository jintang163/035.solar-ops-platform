package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum SparePartRecordTypeEnum {

    IN(1, "入库"),
    OUT(2, "出库");

    private final Integer code;
    private final String desc;

    SparePartRecordTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SparePartRecordTypeEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
