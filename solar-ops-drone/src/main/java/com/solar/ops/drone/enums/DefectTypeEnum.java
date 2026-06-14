package com.solar.ops.drone.enums;

import lombok.Getter;

@Getter
public enum DefectTypeEnum {

    HOT_SPOT("hot_spot", "热斑"),
    MICROCRACK("microcrack", "隐裂"),
    SHADOW("shadow", "遮挡"),
    DELAMINATION("delamination", "脱层"),
    BROKEN("broken", "破损"),
    DIRT("dirt", "脏污");

    private final String code;
    private final String desc;

    DefectTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(String code) {
        for (DefectTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type.getDesc();
            }
        }
        return "未知";
    }
}
