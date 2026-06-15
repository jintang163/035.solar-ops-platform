package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum StocktakeTypeEnum {

    FULL(1, "全盘"),
    SAMPLING(2, "抽盘"),
    SPECIAL(3, "专项盘点");

    private final Integer code;
    private final String desc;

    StocktakeTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StocktakeTypeEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }
}
