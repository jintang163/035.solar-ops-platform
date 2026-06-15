package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum OrgTypeEnum {

    GROUP(1, "集团总部"),
    REGION(2, "区域公司"),
    STATION(3, "电站");

    private final Integer code;
    private final String desc;

    OrgTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) return null;
        for (OrgTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return null;
    }
}
