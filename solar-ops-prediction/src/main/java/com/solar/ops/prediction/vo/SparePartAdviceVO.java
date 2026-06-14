package com.solar.ops.prediction.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "备件更换建议VO")
public class SparePartAdviceVO {

    @ApiModelProperty(value = "逆变器ID")
    private Long inverterId;

    @ApiModelProperty(value = "剩余寿命(天)")
    private Integer remainingLifeDays;

    @ApiModelProperty(value = "当前健康度")
    private BigDecimal currentHealth;

    @ApiModelProperty(value = "是否建议更换")
    private Boolean replacementAdvice;

    @ApiModelProperty(value = "预警列表")
    private List<WarningItem> warnings;

    @ApiModelProperty(value = "建议列表")
    private List<SuggestionItem> suggestions;

    @Data
    public static class WarningItem {
        @ApiModelProperty(value = "级别")
        private String level;

        @ApiModelProperty(value = "消息")
        private String message;

        @ApiModelProperty(value = "备件名称")
        private String sparePart;

        @ApiModelProperty(value = "紧急程度")
        private String urgency;
    }

    @Data
    public static class SuggestionItem {
        @ApiModelProperty(value = "组件名称")
        private String component;

        @ApiModelProperty(value = "原因")
        private String reason;

        @ApiModelProperty(value = "建议")
        private String recommendation;

        @ApiModelProperty(value = "预估费用")
        private String estimatedCost;
    }
}
