package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum AssetStatusEnum {

    NORMAL(1, "正常"),
    IN_MAINTENANCE(2, "运维中"),
    RETIRED(3, "已退役"),
    SCRAPPED(4, "已报废");

    private final Integer code;
    private final String desc;

    AssetStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (AssetStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getDesc();
            }
        }
        return null;
    }
}
