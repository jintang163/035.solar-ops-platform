package com.solar.ops.analysis.enums;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ApiModel(value = "收益统计类型枚举")
public enum RevenueStatisticsTypeEnum {

    DAY(1, "日"),
    WEEK(2, "周"),
    MONTH(3, "月"),
    YEAR(4, "年");

    @ApiModelProperty(value = "类型码")
    private final Integer code;

    @ApiModelProperty(value = "类型描述")
    private final String desc;

    public static RevenueStatisticsTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RevenueStatisticsTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
