package com.solar.ops.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "采购建议VO")
public class SparePartPurchaseSuggestionVO {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "建议单号")
    private String suggestionNo;

    @ApiModelProperty(value = "备件ID")
    private Long partId;

    @ApiModelProperty(value = "备件编号")
    private String partCode;

    @ApiModelProperty(value = "备件名称")
    private String partName;

    @ApiModelProperty(value = "备件型号")
    private String partModel;

    @ApiModelProperty(value = "备件类型")
    private String partType;

    @ApiModelProperty(value = "备件类型描述")
    private String partTypeDesc;

    @ApiModelProperty(value = "当前库存")
    private Integer currentQuantity;

    @ApiModelProperty(value = "安全库存")
    private Integer safeQuantity;

    @ApiModelProperty(value = "建议采购数量")
    private Integer suggestQuantity;

    @ApiModelProperty(value = "最小采购数量")
    private Integer minPurchaseQuantity;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "预估金额")
    private BigDecimal estimatedAmount;

    @ApiModelProperty(value = "供应商")
    private String supplier;

    @ApiModelProperty(value = "紧急程度 1-一般 2-紧急 3-非常紧急")
    private Integer urgency;

    @ApiModelProperty(value = "紧急程度描述")
    private String urgencyDesc;

    @ApiModelProperty(value = "状态 0-待处理 1-已采购 2-已忽略")
    private Integer status;

    @ApiModelProperty(value = "状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "生成时间")
    private LocalDateTime generateTime;

    @ApiModelProperty(value = "处理人ID")
    private Long processorId;

    @ApiModelProperty(value = "处理人姓名")
    private String processorName;

    @ApiModelProperty(value = "处理时间")
    private LocalDateTime processTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
