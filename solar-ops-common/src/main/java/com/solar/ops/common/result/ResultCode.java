package com.solar.ops.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    DEVICE_OFFLINE(1001, "设备离线"),
    DEVICE_FAULT(1002, "设备故障"),
    DATA_EXCEPTION(1003, "数据异常"),

    WORKORDER_NOT_EXIST(2001, "工单不存在"),
    WORKORDER_STATUS_ERROR(2002, "工单状态错误");

    private final Integer code;
    private final String message;
}
