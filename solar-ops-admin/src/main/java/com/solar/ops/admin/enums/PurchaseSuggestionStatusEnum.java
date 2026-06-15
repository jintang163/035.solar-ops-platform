package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum PurchaseSuggestionStatusEnum {

    PENDING(0, "待处理"),
    PURCHASED(1, "已采购"),
    IGNORED(2, "已忽略");

    private final Integer code;
    private final String desc;

    PurchaseSuggestionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PurchaseSuggestionStatusEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
