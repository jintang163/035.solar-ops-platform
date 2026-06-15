package com.solar.ops.admin.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "采购建议查询DTO")
public class PurchaseSuggestionQueryDTO {

    @ApiModelProperty(value = "关键词(备件编号/名称/建议单号)")
    private String keyword;

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "状态 0-待处理 1-已采购 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "紧急程度 1-一般 2-紧急 3-非常紧急")
    private Integer urgency;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;
}
