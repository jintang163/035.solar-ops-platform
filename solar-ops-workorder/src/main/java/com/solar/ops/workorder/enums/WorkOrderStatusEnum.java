package com.solar.ops.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatusEnum {

    PENDING(0, "待接单"),
    ACCEPTED(1, "已接单"),
    PROCESSING(2, "处理中"),
    CHECKING(3, "待验收"),
    COMPLETED(4, "已完成"),
    CLOSED(5, "已关闭");

    private final Integer code;
    private final String desc;

    public static WorkOrderStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
