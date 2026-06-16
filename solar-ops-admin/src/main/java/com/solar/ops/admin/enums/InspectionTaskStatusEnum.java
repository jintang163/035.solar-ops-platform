package com.solar.ops.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InspectionTaskStatusEnum {

    PENDING_DOWNLOAD(0, "待下载"),
    DOWNLOADED(1, "已下载"),
    IN_PROGRESS(2, "执行中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;

    public static String getDesc(Integer code) {
        if (code == null) return null;
        for (InspectionTaskStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return null;
    }
}
