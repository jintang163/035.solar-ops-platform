package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum StocktakeStatusEnum {

    PENDING(0, "待盘点"),
    IN_PROGRESS(1, "盘点中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final Integer code;
    private final String desc;

    StocktakeStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StocktakeStatusEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
