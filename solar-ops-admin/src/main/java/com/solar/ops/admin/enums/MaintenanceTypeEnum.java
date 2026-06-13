package com.solar.ops.admin.enums;

import lombok.Getter;

@Getter
public enum MaintenanceTypeEnum {

    ROUTINE(1, "日常维护"),
    FAULT_REPAIR(2, "故障维修"),
    INSPECTION(3, "定期巡检"),
    SPARE_PART(4, "备件更换");

    private final Integer code;
    private final String desc;

    MaintenanceTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (MaintenanceTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getDesc();
            }
        }
        return null;
    }
}
