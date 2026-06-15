package com.solar.ops.analysis.enums;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ApiModel(value = "清洗计划状态枚举")
public enum CleaningStatusEnum {

    PENDING(0, "待执行"),
    IN_PROGRESS(1, "执行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    @ApiModelProperty(value = "状态码")
    private final Integer code;

    @ApiModelProperty(value = "状态描述")
    private final String desc;

    public static CleaningStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CleaningStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
