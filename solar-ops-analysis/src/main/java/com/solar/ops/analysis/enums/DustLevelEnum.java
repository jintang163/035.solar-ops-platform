package com.solar.ops.analysis.enums;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@ApiModel(value = "积灰程度枚举")
public enum DustLevelEnum {

    NONE(0, "无积灰", new BigDecimal("0.00"), new BigDecimal("0.05"), "#52c41a"),
    LIGHT(1, "轻度积灰", new BigDecimal("0.05"), new BigDecimal("0.10"), "#faad14"),
    MODERATE(2, "中度积灰", new BigDecimal("0.10"), new BigDecimal("0.15"), "#fa8c16"),
    HEAVY(3, "重度积灰", new BigDecimal("0.15"), new BigDecimal("1.00"), "#ff4d4f");

    @ApiModelProperty(value = "等级码")
    private final Integer code;

    @ApiModelProperty(value = "等级描述")
    private final String desc;

    @ApiModelProperty(value = "衰减率下限")
    private final BigDecimal minRate;

    @ApiModelProperty(value = "衰减率上限")
    private final BigDecimal maxRate;

    @ApiModelProperty(value = "标识颜色")
    private final String color;

    public static DustLevelEnum getByAttenuationRate(BigDecimal attenuationRate) {
        if (attenuationRate == null) {
            return NONE;
        }
        for (DustLevelEnum level : values()) {
            if (attenuationRate.compareTo(level.getMinRate()) >= 0
                    && attenuationRate.compareTo(level.getMaxRate()) < 0) {
                return level;
            }
        }
        return NONE;
    }

    public static DustLevelEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DustLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
