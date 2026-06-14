package com.solar.ops.drone.enums;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {

    PENDING(0, "待执行"),
    EXECUTING(1, "执行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    ABNORMAL(4, "异常");

    private final Integer code;
    private final String desc;

    TaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        for (TaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status.getDesc();
            }
        }
        return "未知";
    }
}
