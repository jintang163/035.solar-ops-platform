package com.solar.ops.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part_purchase_suggestion")
@ApiModel(value = "SparePartPurchaseSuggestion对象", description = "备件采购建议表")
public class SparePartPurchaseSuggestion extends BaseEntity {

    private static final long serialVersionUID = 1L;

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

    @ApiModelProperty(value = "状态 0-待处理 1-已采购 2-已忽略")
    private Integer status;

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
}
