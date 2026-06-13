package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum AssetTypeEnum {

    STATION("station", "电站"),
    INVERTER("inverter", "逆变器"),
    COMBINER("combiner", "汇流箱"),
    PANEL("panel", "光伏组件"),
    TRANSFORMER("transformer", "变压器"),
    OTHER("other", "其他");

    private final String code;
    private final String desc;

    AssetTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(String code) {
        for (AssetTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getDesc();
            }
        }
        return null;
    }
}
