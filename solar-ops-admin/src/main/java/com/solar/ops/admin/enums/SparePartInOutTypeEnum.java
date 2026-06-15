package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum SparePartInOutTypeEnum {

    PURCHASE_IN(11, "采购入库"),
    PROFIT_IN(12, "盘盈入库"),
    RETURN_IN(13, "退库入库"),
    WORK_ORDER_OUT(21, "工单出库"),
    LOSS_OUT(22, "盘亏出库"),
    SCRAP_OUT(23, "报废出库");

    private final Integer code;
    private final String desc;

    SparePartInOutTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SparePartInOutTypeEnum enums : values()) {
            if (enums.getCode().equals(code)) {
                return enums.getDesc();
            }
        }
        return null;
    }

    public static boolean isInType(Integer code) {
        return code != null && code >= 10 && code < 20;
    }

    public static boolean isOutType(Integer code) {
        return code != null && code >= 20 && code < 30;
    }
}
