package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum DataScopeEnum {

    ALL(1, "全部数据"),
    ORG_AND_CHILD(2, "本组织及以下"),
    SELF_ONLY(3, "仅本人数据");

    private final Integer code;
    private final String desc;

    DataScopeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) return null;
        for (DataScopeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return null;
    }
}
